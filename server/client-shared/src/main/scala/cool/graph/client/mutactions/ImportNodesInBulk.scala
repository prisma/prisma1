package cool.graph.client.mutactions

import java.sql.SQLIntegrityConstraintViolationException

import cool.graph.client.ClientInjector
import cool.graph.client.database.DatabaseMutationBuilder.MirrorFieldDbValues
import cool.graph.client.database.{DataResolver, DatabaseMutationBuilder, ProjectRelayId, ProjectRelayIdTable}
import cool.graph.client.mutactions.DataImport._
import cool.graph.cuid.Cuid
import cool.graph.shared.RelationFieldMirrorColumn
import cool.graph.shared.database.Databases
import cool.graph.shared.errors.UserAPIErrors
import cool.graph.shared.models._
import cool.graph.{ClientSqlStatementResult, MutactionVerificationSuccess, _}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery
import slick.sql.SqlAction
import spray.json.{DefaultJsonProtocol, JsArray, JsFalse, JsNull, JsNumber, JsObject, JsString, JsTrue, JsValue, JsonFormat, RootJsonFormat}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.util.{Success, Try}

case class ImportNodesInBulk(
    project: Project,
    bulk: Vector[ImportNodeValue]
)(implicit injector: ClientInjector)
    extends ClientSqlDataChangeMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = {
    val relayIds = TableQuery(new ProjectRelayIdTable(_, project.id))

    val dbActions = bulk.flatMap { element =>
      val id        = element.identifier.id
      val modelName = element.identifier.typeName
      List(
        DatabaseMutationBuilder.createDataItem(project.id, element.identifier.typeName, element.values + ("id" -> id)),
        relayIds += ProjectRelayId(id = id, project.getModelByName_!(modelName).id)
      )
    }

    Future(
      ClientSqlStatementResult(
        sqlAction = DBIO.sequence(dbActions)
      ))
  }

  override def handleErrors = {
    implicit val anyFormat = JsonFormats.AnyJsonFormat
    Some({
      //https://dev.mysql.com/doc/refman/5.5/en/error-messages-server.html#error_er_dup_entry
      case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1062 =>
        UserAPIErrors.UniqueConstraintViolation("", "")
      case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1452 =>
        UserAPIErrors.NodeDoesNotExist("")
    })
  }

  override def verify(resolver: DataResolver): Future[Try[MutactionVerificationSuccess]] = Future.successful(Success(MutactionVerificationSuccess()))

}

object DataImport {

  case class ImportBundle(valueType: String, values: JsArray)
  case class ImportIdentifier(typeName: String, id: String)
  case class ImportRelationSide(identifier: ImportIdentifier, fieldName: String)
  case class ImportNodeValue(index: Int, identifier: ImportIdentifier, values: Map[String, Any])
  case class ImportRelation(index: Int, relationName: String, left: ImportRelationSide, right: ImportRelationSide)
  case class ImportListValue(index: Int, identifier: ImportIdentifier, values: Map[String, Vector[Any]])

  object MyJsonProtocol extends DefaultJsonProtocol {

    implicit object AnyJsonFormat extends JsonFormat[Any] {
      def write(x: Any) = x match {
        case n: Int                   => JsNumber(n)
        case s: String                => JsString(s)
        case b: Boolean if b == true  => JsTrue
        case b: Boolean if b == false => JsFalse
      }
      def read(value: JsValue) = value match {
        case JsNumber(n) => n.intValue()
        case JsString(s) => s
        case JsTrue      => true
        case JsFalse     => false
        case x: JsArray  => x.elements.map(read)
        case JsNull      => sys.error("null shouldnt happen")
        case JsObject(_) => sys.error("object shouldnt happen")
      }
    }

    implicit val importBundle: RootJsonFormat[ImportBundle]             = jsonFormat2(ImportBundle)
    implicit val importIdentifier: RootJsonFormat[ImportIdentifier]     = jsonFormat2(ImportIdentifier)
    implicit val importRelationSide: RootJsonFormat[ImportRelationSide] = jsonFormat2(ImportRelationSide)
    implicit val importNodeValue: RootJsonFormat[ImportNodeValue]       = jsonFormat3(ImportNodeValue)
    implicit val importListValue: RootJsonFormat[ImportListValue]       = jsonFormat3(ImportListValue)
    implicit val importRelation: RootJsonFormat[ImportRelation]         = jsonFormat4(ImportRelation)
  }

  def parseNodes(json: JsValue) = {
    import MyJsonProtocol._
    json.convertTo[JsArray].elements.map(_.convertTo[ImportNodeValue])
  }

  def parseLists(json: JsValue) = {
    import MyJsonProtocol._
    json.convertTo[JsArray].elements.map(_.convertTo[ImportListValue])
  }

  def parseRelations(json: JsValue) = {
    import MyJsonProtocol._
    json.convertTo[JsArray].elements.map(_.convertTo[ImportRelation])
  }

  def executeGeneric(project: Project, json: JsValue)(implicit injector: ClientInjector) = {
    import MyJsonProtocol._
    val bundle = json.convertTo[ImportBundle]

    val actions = bundle.valueType match {
      case "nodes"      => generateImportNodesDBActions(project, bundle.values.elements.map(_.convertTo[ImportNodeValue]))
      case "relations"  => generateImportRelationsDBActions(project, bundle.values.elements.map(_.convertTo[ImportRelation]))
      case "listvalues" => generateImportListsDBActions(project, bundle.values.elements.map(_.convertTo[ImportListValue]))
    }
    runDBActions(project, actions)
  }

  def generateImportNodesDBActions(project: Project, nodes: Vector[ImportNodeValue]) = {
    val relayIds = TableQuery(new ProjectRelayIdTable(_, project.id))
    val dbActions: Vector[SqlAction[Int, NoStream, Effect.Write]] = nodes.flatMap { element =>
      val id                              = element.identifier.id
      val model                           = project.getModelByName_!(element.identifier.typeName)
      val listFields: Map[String, String] = model.scalarFields.filter(_.isList).map(field => field.name -> "[]").toMap
      val values: Map[String, Any]        = element.values ++ listFields + ("id" -> id)

      List(DatabaseMutationBuilder.createDataItem(project.id, model.name, values), relayIds += ProjectRelayId(id = id, model.id))
    }
    val x: DBIOAction[Vector[Int], NoStream, Effect.Write] = DBIO.sequence(dbActions)
    x
  }

  def generateImportRelationsDBActions(project: Project, relations: Vector[ImportRelation]) = {
    val dbActions = relations.map { element =>
      val fromModel                                                 = project.getModelByName_!(element.left.identifier.typeName)
      val fromField                                                 = fromModel.getFieldByName_!(element.left.fieldName)
      val relationSide: cool.graph.shared.models.RelationSide.Value = fromField.relationSide.get
      val relation: Relation                                        = fromField.relation.get

      val aValue: String = if (relationSide == RelationSide.A) element.left.identifier.id else element.right.identifier.id
      val bValue: String = if (relationSide == RelationSide.A) element.right.identifier.id else element.left.identifier.id

      val aModel: Model = relation.getModelA_!(project)
      val bModel: Model = relation.getModelB_!(project)

      def getFieldMirrors(model: Model, id: String) =
        relation.fieldMirrors
          .filter(mirror => model.fields.map(_.id).contains(mirror.fieldId))
          .map(mirror => {
            val field = project.getFieldById_!(mirror.fieldId)
            MirrorFieldDbValues(
              relationColumnName = RelationFieldMirrorColumn.mirrorColumnName(project, field, relation),
              modelColumnName = field.name,
              model.name,
              id
            )
          })

      val fieldMirrors: List[MirrorFieldDbValues] = getFieldMirrors(aModel, aValue) ++ getFieldMirrors(bModel, bValue)

      DatabaseMutationBuilder.createRelationRow(project.id, relation.id, Cuid.createCuid(), aValue, bValue, fieldMirrors)
    }
    val x: DBIOAction[Vector[Int], NoStream, Effect.Write] = DBIO.sequence(dbActions)
    x
  }

  def generateImportListsDBActions(project: Project, lists: Vector[ImportListValue]) = {
    val dbActions: Vector[SqlAction[Int, NoStream, Effect.Write]] = lists.map { element =>
      val id    = element.identifier.id
      val model = project.getModelByName_!(element.identifier.typeName)
      DatabaseMutationBuilder.updateDataItemListValue(project.id, model.name, id, element.values)
    }
    val x: DBIOAction[Vector[Int], NoStream, Effect.Write] = DBIO.sequence(dbActions)
    x
  }

  def runDBActions(project: Project, actions: DBIOAction[Vector[Int], NoStream, Effect.Write])(implicit injector: ClientInjector) = {
    import scala.concurrent.duration._
    val db: Databases            = injector.globalDatabaseManager.getDbForProject(project)
    val res: Future[Vector[Int]] = db.master.run(actions)
    Await.result(res, 500.seconds)
  }

  def executeImport(project: Project, json: JsValue)(implicit injector: ClientInjector) = {

    val begin: Long = System.currentTimeMillis
    val parsed      = parseNodes(json)
    val actions     = generateImportNodesDBActions(project, parsed)
    runDBActions(project, actions)
    val end: Long = System.currentTimeMillis
    println("Importing: " + (end - begin))
  }

}

object teststuff {

  def readFile(fileName: String): JsValue = {
    import spray.json._
    val json_string = scala.io.Source
      .fromFile(s"/Users/matthias/repos/github.com/graphcool/closed-source/integration-testing/src/test/scala/cool/graph/importData/$fileName")
      .getLines
      .mkString
    json_string.parseJson
  }

  def parseNodesTest(fileName: String) = {
    import MyJsonProtocol._
    import spray.json._
    val begin: Long = System.currentTimeMillis

    val jsonAstFile                  = readFile(fileName)
    val array                        = jsonAstFile.convertTo[JsArray]
    val res: Vector[ImportNodeValue] = array.elements.map(_.convertTo[ImportNodeValue])

    val end: Long = System.currentTimeMillis
    println("Parsing Nodes: " + (end - begin))
    res
  }

  def parseListsTest(fileName: String) = {
    import MyJsonProtocol._
    import spray.json._
    val begin: Long = System.currentTimeMillis

    val jsonAstFile                  = readFile(fileName)
    val array                        = jsonAstFile.convertTo[JsArray]
    val res: Vector[ImportListValue] = array.elements.map(_.convertTo[ImportListValue])

    val end: Long = System.currentTimeMillis
    println("Parsing Lists: " + (end - begin))
    res
  }

  def parseRelationsTest(fileName: String) = {
    import MyJsonProtocol._
    import spray.json._
    val begin: Long = System.currentTimeMillis

    val jsonAstFile = readFile(fileName)

    ///
    val array                       = jsonAstFile.convertTo[JsArray]
    val res: Vector[ImportRelation] = array.elements.map(_.convertTo[ImportRelation])

    val end: Long = System.currentTimeMillis
    println("Parsing Relations: " + (end - begin))
    res
  }

}
