package cool.graph.client.mutactions

import cool.graph.client.ClientInjector
import cool.graph.client.database.DatabaseMutationBuilder.MirrorFieldDbValues
import cool.graph.client.database._
import cool.graph.cuid.Cuid
import cool.graph.shared.RelationFieldMirrorColumn
import cool.graph.shared.database.Databases
import cool.graph.shared.models._
import slick.dbio.Effect
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery
import spray.json.{DefaultJsonProtocol, JsArray, JsFalse, JsNull, JsNumber, JsObject, JsString, JsTrue, JsValue, JsonFormat, RootJsonFormat}

import scala.concurrent.Future
import scala.util.Try

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

  def executeGeneric(project: Project, json: JsValue)(implicit injector: ClientInjector): Future[Vector[String]] = {
    import MyJsonProtocol._

    import scala.concurrent.ExecutionContext.Implicits.global
    val bundle = json.convertTo[ImportBundle]
    val cnt    = bundle.values.elements.length

    val actions = bundle.valueType match {
      case "nodes"      => generateImportNodesDBActions(project, bundle.values.elements.map(_.convertTo[ImportNodeValue]))
      case "relations"  => generateImportRelationsDBActions(project, bundle.values.elements.map(_.convertTo[ImportRelation]))
      case "listvalues" => generateImportListsDBActions(project, bundle.values.elements.map(_.convertTo[ImportListValue]))
    }

    val res: Future[Vector[Try[Int]]]                       = runDBActions(project, actions)
    def messageWithOutConnection(tryelem: Try[Any]): String = tryelem.failed.get.getMessage.substring(tryelem.failed.get.getMessage.indexOf(")") + 1)

    res.map(vector =>
      vector.zipWithIndex.collect {
        case (elem, idx) if elem.isFailure && idx < cnt  => s"Index: $idx Message: ${messageWithOutConnection(elem)}"
        case (elem, idx) if elem.isFailure && idx >= cnt => s"Index: ${idx - cnt} Message: Relay Id Failure ${messageWithOutConnection(elem)}"
    })
  }

  def generateImportNodesDBActions(project: Project, nodes: Vector[ImportNodeValue]): Vector[DBIOAction[Try[Int], NoStream, Effect.Write]] = {
    val items = nodes.map { element =>
      val id                              = element.identifier.id
      val model                           = project.getModelByName_!(element.identifier.typeName)
      val listFields: Map[String, String] = model.scalarFields.filter(_.isList).map(field => field.name -> "[]").toMap
      val values: Map[String, Any]        = element.values ++ listFields + ("id" -> id)
      DatabaseMutationBuilder.createDataItem(project.id, model.name, values).asTry
    }
    val relayIds: TableQuery[ProjectRelayIdTable] = TableQuery(new ProjectRelayIdTable(_, project.id))
    val relay = nodes.map { element =>
      val id    = element.identifier.id
      val model = project.getModelByName_!(element.identifier.typeName)
      val x     = relayIds += ProjectRelayId(id = id, model.id)
      x.asTry
    }
    items ++ relay
  }

  def generateImportRelationsDBActions(project: Project, relations: Vector[ImportRelation]): Vector[DBIOAction[Try[Int], NoStream, Effect.Write]] = {
    relations.map { element =>
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

      DatabaseMutationBuilder.createRelationRow(project.id, relation.id, Cuid.createCuid(), aValue, bValue, fieldMirrors).asTry
    }
  }

  def generateImportListsDBActions(project: Project, lists: Vector[ImportListValue]): Vector[DBIOAction[Try[Int], NoStream, Effect.Write]] = {
    lists.map { element =>
      val id    = element.identifier.id
      val model = project.getModelByName_!(element.identifier.typeName)
      DatabaseMutationBuilder.updateDataItemListValue(project.id, model.name, id, element.values).asTry
    }
  }

  def runDBActions(project: Project, actions: Vector[DBIOAction[Try[Int], NoStream, Effect.Write]])(
      implicit injector: ClientInjector): Future[Vector[Try[Int]]] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val db: Databases = injector.globalDatabaseManager.getDbForProject(project)
    Future.sequence(actions.map(db.master.run))
  }
}

object DataExport {}

object teststuff {

  def readFile(fileName: String): JsValue = {
    import spray.json._
    val json_string = scala.io.Source
      .fromFile(s"/Users/matthias/repos/github.com/graphcool/closed-source/integration-testing/src/test/scala/cool/graph/importData/$fileName")
      .getLines
      .mkString
    json_string.parseJson
  }
}
