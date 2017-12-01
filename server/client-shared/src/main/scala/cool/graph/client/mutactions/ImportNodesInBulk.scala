package cool.graph.client.mutactions

import cool.graph.client.ClientInjector
import cool.graph.client.database.DatabaseMutationBuilder.MirrorFieldDbValues
import cool.graph.client.database.{DatabaseMutationBuilder, ProjectRelayId, ProjectRelayIdTable}
import cool.graph.cuid.Cuid
import cool.graph.shared.RelationFieldMirrorColumn
import cool.graph.shared.database.Databases
import cool.graph.shared.models._
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery
import slick.sql.SqlAction
import spray.json.{DefaultJsonProtocol, JsArray, JsFalse, JsNull, JsNumber, JsObject, JsString, JsTrue, JsValue, JsonFormat, RootJsonFormat}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

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

  def runDBActions(project: Project, actions: DBIOAction[Vector[Int], NoStream, Effect.Write])(implicit injector: ClientInjector): Future[Unit] = {
    val db: Databases = injector.globalDatabaseManager.getDbForProject(project)
    db.master.run(actions).map(_ => ())
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
}
