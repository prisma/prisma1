package com.prisma.api.import_export

import com.prisma.api.ApiDependencies
import com.prisma.api.connector._
import com.prisma.api.import_export.GCValueJsonFormatter.UnknownFieldException
import com.prisma.api.import_export.ImportExport.MyJsonProtocol._
import com.prisma.api.import_export.ImportExport._
import com.prisma.shared.models._
import org.scalactic.{Bad, Good, Or}
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class BulkImport(project: Project)(implicit apiDependencies: ApiDependencies) {
  import com.prisma.utils.future.FutureUtils._

  def executeImport(json: JsValue): Future[JsValue] = executeImport(json.as[ImportBundle])

  def executeImport(bundle: ImportBundle): Future[JsValue] = {

    val res: Future[Vector[Try[_]]] =
      bundle.valueType match {
        case "nodes" =>
          val importNodes                             = bundle.values.value.map(convertToImportNode).toVector
          val goods: Vector[ImportNode]               = importNodes.collect { case Good(x) => x }
          val bads: Vector[Exception]                 = importNodes.collect { case x: Bad[Exception] => x.b }
          val newGoods: Vector[CreateDataItemsImport] = generateImportNodesDBActions(goods)

          val creates = newGoods.map(m => apiDependencies.databaseMutactionExecutor.execute(Vector(m), runTransactionally = false).toFutureTry)
          val errors  = bads.map(exception => Future.successful(Failure(exception)))
          Future.sequence(creates ++ errors)

        case "relations" =>
          val mutactions = generateImportRelationsDBActions(bundle.values.value.map(convertToImportRelation).toVector)
          Future.sequence(mutactions.map(m => apiDependencies.databaseMutactionExecutor.execute(Vector(m), runTransactionally = false).toFutureTry))

        case "lists" =>
          val mutactions = generateImportListsDBActions(bundle.values.value.map(convertToImportList).toVector)
          Future.sequence(mutactions.map(m => apiDependencies.databaseMutactionExecutor.execute(m, runTransactionally = false).toFutureTry))
      }

    res
      .map(vector => vector.collect { case elem if elem.isFailure => elem.failed.get.getMessage.split("-@-").map(Json.toJson(_)) }.flatten)
      .map(x => JsArray(x))
  }

  private def convertToImportNode(json: JsValue): ImportNode Or Exception = {
    val jsObject = json.as[JsObject]
    val typeName = jsObject.value("_typeName").as[String]
    val id       = jsObject.value("id").as[String]
    val model    = project.schema.getModelByName_!(typeName)

    val newJsObject = JsObject(jsObject.fields.filter(_._1 != "_typeName"))

    Try {
      GCValueJsonFormatter.readModelAwareGcValue(model)(newJsObject).get
    } match {
      case Success(x)                        => Good(ImportNode(id, model, x))
      case Failure(e: UnknownFieldException) => Bad(new Exception(s"The model ${model.name} with id $id has an unknown field '${e.field}' in field list."))
      case Failure(e)                        => throw e
    }
  }

  private def convertToImportList(json: JsValue): ImportList = {
    val jsObject     = json.as[JsObject]
    val typeName     = jsObject.value("_typeName").as[String]
    val id           = jsObject.value("id").as[String]
    val fieldName    = jsObject.value.filterKeys(k => k != "_typeName" && k != "id").keys.head
    val jsonForField = jsObject.value(fieldName)
    val field        = project.schema.getModelByName_!(typeName).getFieldByName_!(fieldName)
    val tableName    = s"${typeName}_$fieldName"
    val gcValue      = GCValueJsonFormatter.readListGCValue(field)(jsonForField).get
    ImportList(ImportIdentifier(typeName, id), tableName, gcValue)
  }

  private def convertToImportRelation(json: JsValue): ImportRelation = {
    val array    = json.as[JsArray]
    val leftMap  = array.value.head.as[JsObject].value
    val rightMap = array.value.last.as[JsObject].value
    val left =
      ImportRelationSide(ImportIdentifier(leftMap("_typeName").as[String], leftMap("id").as[String]), leftMap.get("fieldName").flatMap(_.asOpt[String]))
    val right =
      ImportRelationSide(ImportIdentifier(rightMap("_typeName").as[String], rightMap("id").as[String]), rightMap.get("fieldName").flatMap(_.asOpt[String]))

    ImportRelation(left, right)
  }

  private def generateImportNodesDBActions(nodes: Vector[ImportNode]): Vector[CreateDataItemsImport] = {
    val creates                                                = nodes.map(node => CreateDataItemImport(project, node.model, PrismaArgs(node.values)))
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

  private def generateImportListsDBActions(lists: Vector[ImportList]): Vector[Vector[PushScalarListsImport]] =
    lists
      .map(element => PushScalarListsImport(project, element.tableName, element.identifier.id, element.values))
      .groupBy(_.id)
      .values
      .toVector
}
