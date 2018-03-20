package com.prisma.api.import_export

import com.prisma.api.ApiDependencies
import com.prisma.api.connector._
import com.prisma.api.import_export.GCValueJsonFormatter.UnknownFieldException
import com.prisma.api.import_export.ImportExport.MyJsonProtocol._
import com.prisma.api.import_export.ImportExport._
import com.prisma.shared.models._
import com.prisma.util.json.PlaySprayConversions
import org.scalactic.{Bad, Good, Or}
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class BulkImport(project: Project)(implicit apiDependencies: ApiDependencies) extends PlaySprayConversions {
  import com.prisma.utils.future.FutureUtils._

  def executeImport(json: JsValue): Future[JsValue] = {

    val bundle = json.convertTo[ImportBundle]

    val res: Future[Vector[Try[Unit]]] =
      bundle.valueType match {
        case "nodes" =>
          val x                                                        = bundle.values.elements.map(convertToImportNode)
          val goods                                                    = x.collect { case Good(x) => x }
          val bads                                                     = x.collect { case x: Bad[Exception] => x }
          val newGoods                                                 = generateImportNodesDBActions(goods).map(Good(_))
          val mutactions: Vector[Or[CreateDataItemsImport, Exception]] = newGoods ++ bads

          Future.sequence(mutactions.map {
            case Good(m)        => apiDependencies.databaseMutactionExecutor.execute(Vector(m), runTransactionally = false).toFutureTry
            case Bad(exception) => Future.successful(Failure(exception))
          })

        case "relations" =>
          val mutactions = generateImportRelationsDBActions(bundle.values.elements.map(convertToImportRelation))
          Future.sequence(mutactions.map(m => apiDependencies.databaseMutactionExecutor.execute(Vector(m), runTransactionally = false).toFutureTry))

        case "lists" =>
          val mutactions = generateImportListsDBActions(bundle.values.elements.map(convertToImportList))
          mutactions.map(m => () => apiDependencies.databaseMutactionExecutor.execute(Vector(m), runTransactionally = false).toFutureTry).runSequentially
      }

    res
      .map(vector => vector.collect { case elem if elem.isFailure => elem.failed.get.getMessage.split("-@-").map(_.toJson) }.flatten)
      .map(x => JsArray(x))
  }

  private def convertToImportNode(json: JsValue): ImportNode Or Exception = {
    val jsObject = json.asJsObject
    val typeName = jsObject.fields("_typeName").asInstanceOf[JsString].value
    val id       = jsObject.fields("id").asInstanceOf[JsString].value
    val model    = project.schema.getModelByName_!(typeName)

    val newJsObject = JsObject(jsObject.fields.filter(_._1 != "_typeName"))

    Try {
      GCValueJsonFormatter.readModelAwareGcValue(model)(newJsObject.toPlay()).get
    } match {
      case Success(x)                        => Good(ImportNode(id, model, x))
      case Failure(e: UnknownFieldException) => Bad(new Exception(s"The model ${model.name} with id $id has an unknown field '${e.field}' in field list."))
      case Failure(e)                        => throw e
    }
  }

  private def convertToImportList(json: JsValue): ImportList = {
    val jsObject     = json.asJsObject
    val typeName     = jsObject.fields("_typeName").asInstanceOf[JsString].value
    val id           = jsObject.fields("id").asInstanceOf[JsString].value
    val fieldName    = jsObject.fields.filterKeys(k => k != "_typeName" && k != "id").keys.head
    val jsonForField = jsObject.fields(fieldName)
    val field        = project.schema.getModelByName_!(typeName).getFieldByName_!(fieldName)

    val gcValue = GCValueJsonFormatter.readListGCValue(field)(jsonForField.toPlay()).get
    ImportList(ImportIdentifier(typeName, id), fieldName, gcValue)
  }

  private def convertToImportRelation(json: JsValue): ImportRelation = {
    val array    = json.convertTo[JsArray]
    val leftMap  = array.elements.head.convertTo[Map[String, Option[String]]]
    val rightMap = array.elements.last.convertTo[Map[String, Option[String]]]
    val left     = ImportRelationSide(ImportIdentifier(leftMap("_typeName").get, leftMap("id").get), leftMap.get("fieldName").flatten)
    val right    = ImportRelationSide(ImportIdentifier(rightMap("_typeName").get, rightMap("id").get), rightMap.get("fieldName").flatten)

    ImportRelation(left, right)
  }

  private def generateImportNodesDBActions(nodes: Vector[ImportNode]): Vector[CreateDataItemsImport] = {
    val creates                                                = nodes.map(node => CreateDataItemImport(project, node.model, ReallyCoolArgs(node.values)))
    val groupedItems: Map[Model, Vector[CreateDataItemImport]] = creates.groupBy(_.model)
    groupedItems.map { case (model, group) => CreateDataItemsImport(project, model, group.map(_.args)) }.toVector
  }

  private def generateImportRelationsDBActions(relations: Vector[ImportRelation]): Vector[CreateRelationRowsImport] = {
    val createRows = relations.map { element =>
      val (left, right) = (element.left, element.right) match {
        case (l, r) if l.fieldName.isDefined => (l, r)
        case (l, r) if r.fieldName.isDefined => (r, l)
        case _                               => throw sys.error("Invalid ImportRelation at least one fieldName needs to be defined.")
      }

      val fromModel                                                 = project.schema.getModelByName_!(left.identifier.typeName)
      val fromField                                                 = fromModel.getFieldByName_!(left.fieldName.get)
      val relationSide: com.prisma.shared.models.RelationSide.Value = fromField.relationSide.get
      val relation: Relation                                        = fromField.relation.get
      val aValue: String                                            = if (relationSide == RelationSide.A) left.identifier.id else right.identifier.id
      val bValue: String                                            = if (relationSide == RelationSide.A) right.identifier.id else left.identifier.id
      CreateRelationRow(project, relation, aValue, bValue)
    }
    val groupedItems = createRows.groupBy(_.relation)
    groupedItems.map { case (relation, group) => CreateRelationRowsImport(project, relation, group.map(item => (item.a, item.b))) }.toVector
  }

  private def generateImportListsDBActions(lists: Vector[ImportList]): Vector[PushScalarListsImport] = lists.map { element =>
    PushScalarListsImport(project, s"${element.identifier.typeName}_${element.fieldName}", element.identifier.id, element.values)
  }
}
