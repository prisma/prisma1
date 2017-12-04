package cool.graph.client.mutactions

import cool.graph.DataItem
import cool.graph.Types.UserData
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
import spray.json.{DefaultJsonProtocol, JsArray, JsBoolean, JsFalse, JsNull, JsNumber, JsObject, JsString, JsTrue, JsValue, JsonFormat, RootJsonFormat}

import scala.concurrent.Future
import scala.util.Try

object ImportExportFormat {
  case class StringWithCursor(string: String, modelIndex: Int, start: Int)

  case class ImportBundle(valueType: String, values: JsArray)
  case class ImportIdentifier(typeName: String, id: String)
  case class ImportRelationSide(identifier: ImportIdentifier, fieldName: String)
  case class ImportNodeValue(index: Int, identifier: ImportIdentifier, values: Map[String, Any])
  case class ImportRelation(index: Int, relationName: String, left: ImportRelationSide, right: ImportRelationSide)
  case class ImportListValue(index: Int, identifier: ImportIdentifier, values: Map[String, Vector[Any]])

  object MyJsonProtocol extends DefaultJsonProtocol {

    //from requestpipelinerunner -> there's 10 different versions of this all over the place -.-
    implicit object AnyJsonFormat extends JsonFormat[Any] {
      def write(x: Any): JsValue = x match {
        case m: Map[_, _]   => JsObject(m.asInstanceOf[Map[String, Any]].mapValues(write))
        case l: List[Any]   => JsArray(l.map(write).toVector)
        case l: Vector[Any] => JsArray(l.map(write))
        case l: Seq[Any]    => JsArray(l.map(write).toVector)
        case n: Int         => JsNumber(n)
        case n: Long        => JsNumber(n)
        case n: BigDecimal  => JsNumber(n)
        case n: Double      => JsNumber(n)
        case s: String      => JsString(s)
        case true           => JsTrue
        case false          => JsFalse
        case v: JsValue     => v
        case null           => JsNull
        case r              => JsString(r.toString)
      }

      def read(x: JsValue): Any = {
        x match {
          case l: JsArray   => l.elements.map(read).toList
          case m: JsObject  => m.fields.mapValues(read)
          case s: JsString  => s.value
          case n: JsNumber  => n.value
          case b: JsBoolean => b.value
          case JsNull       => null
          case _            => sys.error("implement all scalar types!")
        }
      }
    }

    implicit val importBundle: RootJsonFormat[ImportBundle]             = jsonFormat2(ImportBundle)
    implicit val importIdentifier: RootJsonFormat[ImportIdentifier]     = jsonFormat2(ImportIdentifier)
    implicit val importRelationSide: RootJsonFormat[ImportRelationSide] = jsonFormat2(ImportRelationSide)
    implicit val importNodeValue: RootJsonFormat[ImportNodeValue]       = jsonFormat3(ImportNodeValue)
    implicit val importListValue: RootJsonFormat[ImportListValue]       = jsonFormat3(ImportListValue)
    implicit val importRelation: RootJsonFormat[ImportRelation]         = jsonFormat4(ImportRelation)
  }
}

object DataImport {
  import cool.graph.client.mutactions.ImportExportFormat._

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

object DataExport {

  import cool.graph.client.mutactions.ImportExportFormat._

  //missing:
  // return cursor for resume
  // exponential backoff

  def exportData(project: Project, dataResolver: DataResolver)(implicit injector: ClientInjector): Future[StringWithCursor] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val modelsWithIndex: List[(Model, Int)]  = project.models.zipWithIndex
    def isLimitReached(str: String): Boolean = str.length > 1500

    def createStringStartingFromCursor(index: Int, modelIndex: Int, start: Int, amount: Int, combinedString: String): Future[StringWithCursor] = {
      val model = modelsWithIndex.find(_._2 == modelIndex).get._1
      for {
        exportForModel <- getExportStringForModel(index, model, start, amount, combinedString)
        x <- if (isLimitReached(exportForModel.export)) { // we have to start next file from this cursor
              Future.successful(StringWithCursor(combinedString, modelIndex, start))
            } else if (modelIndex < project.models.length - 1) {
              val newIndex = exportForModel.lastIndex + 1
              createStringStartingFromCursor(newIndex, modelIndex + 1, 0, amount, exportForModel.export)
            } else { // we're done
              Future.successful(StringWithCursor(exportForModel.export, -1, -1))
            }
      } yield x
    }

    case class ExportForModel(export: String, lastIndex: Int)

    def getExportStringForModel(index: Int, model: Model, start: Int, amount: Int, startingString: String): Future[ExportForModel] = {
      val dataItemsPage: Future[DataItemsPage] = fetchDataItemsPage(model, start, amount)
      dataItemsPage.flatMap { page =>
        val serialized     = serializePage(page, model, index)
        val combinedString = startingString + serialized.serializedString
        val limitReached   = isLimitReached(combinedString)

        if (!limitReached && page.hasMore) {
          getExportStringForModel(serialized.nextIndex, model, start + amount, amount, combinedString)
        } else if (!limitReached) {
          Future.successful(ExportForModel(export = combinedString, lastIndex = serialized.nextIndex - 1))
        } else {
          Future.successful(ExportForModel(export = startingString, lastIndex = index - 1))
        }
      }
    }

    case class DataItemsPage(items: Seq[DataItem], hasMore: Boolean)
// we could use the id as cursor for nodes
    def fetchDataItemsPage(model: Model, start: Int, amount: Int): Future[DataItemsPage] = {
      val queryArguments = QueryArguments(skip = Some(start), after = None, first = Some(amount + 1), None, None, None, None)
      for {
        resolverResult <- dataResolver.resolveByModel(model, Some(queryArguments))
      } yield {
        DataItemsPage(resolverResult.items.take(amount), hasMore = resolverResult.items.length > amount)
      }
    }

    case class SerializeResult(serializedString: String, nextIndex: Int)

    def serializePage(page: DataItemsPage, model: Model, startIndex: Int): SerializeResult = {
      var index = startIndex
      val serializedItems = for {
        item <- page.items
      } yield {
        val tmp = dataItemToExportLine(index, model, item)
        index = index + 1
        tmp
      }
      SerializeResult(serializedString = serializedItems.mkString(","), nextIndex = index)
    }

    def dataItemToExportLine(index: Int, model: Model, item: DataItem): String = {
      import MyJsonProtocol._
      import spray.json._

      val dataValueMap: UserData                              = item.userData
      val createdAtUpdatedAtMap                               = dataValueMap.collect { case (k, Some(v)) if k == "createdAt" || k == "updatedAt" => (k, v) }
      val withoutCreatedAtUpdatedAt: Map[String, Option[Any]] = dataValueMap.collect { case (k, v) if k != "createdAt" && k != "updatedAt" => (k, v) }
      val nonListFieldsWithValues: Map[String, Any]           = withoutCreatedAtUpdatedAt.collect { case (k, Some(v)) if !model.getFieldByName_!(k).isList => (k, v) }
      val outputMap: Map[String, Any]                         = nonListFieldsWithValues ++ createdAtUpdatedAtMap

      val importIdentifier: ImportIdentifier = ImportIdentifier(model.name, item.id)
      val nodeValue: ImportNodeValue         = ImportNodeValue(index = index, identifier = importIdentifier, values = outputMap)
      nodeValue.toJson.toString
    }

    val res = createStringStartingFromCursor(0, 0, 0, 3, "")

    res
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
