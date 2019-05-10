package com.prisma.api.import_export

import com.prisma.api.ApiDependencies
import com.prisma.api.connector._
import com.prisma.api.import_export.GCValueJsonFormatter.{InvalidFieldValueException, UnknownFieldException}
import com.prisma.api.import_export.ImportExport.MyJsonProtocol._
import com.prisma.api.import_export.ImportExport._
import com.prisma.gc_values.{IdGCValue, ListGCValue, StringIdGCValue, UuidGCValue}
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
          val importNodes                   = bundle.values.value.map(convertToImportNode).toVector
          val goods: Vector[ImportNode]     = importNodes.collect { case Good(x) => x }
          val bads: Vector[Exception]       = importNodes.collect { case x: Bad[Exception] => x.b }
          val newGoods: Vector[ImportNodes] = generateImportNodesDBActions(goods)

          val creates = newGoods.map(m => apiDependencies.databaseMutactionExecutor.executeNonTransactionally(m).toFutureTry)
          val errors  = bads.map(exception => Future.successful(Failure(exception)))
          Future.sequence(creates ++ errors)

        case "relations" =>
          val mutactions = generateImportRelationsDBActions(bundle.values.value.map(convertToImportRelation).toVector)
          Future.sequence(mutactions.map(m => apiDependencies.databaseMutactionExecutor.executeNonTransactionally(m).toFutureTry))

        case "lists" =>
          val mutactions = generateImportListsDBActions(bundle.values.value.map(convertToImportList).toVector)
          Future.sequence(mutactions.map(m => apiDependencies.databaseMutactionExecutor.executeNonTransactionally(m).toFutureTry))
      }

    res
      .map(vector => vector.collect { case elem if elem.isFailure => elem.failed.get.getMessage.split("-@-").map(Json.toJson(_)) }.flatten)
      .map(x => JsArray(x))
  }

  private def convertToImportNode(json: JsValue): ImportNode Or Exception = {
    val jsObject = json.as[JsObject]
    val typeName = jsObject.value("_typeName").as[String]
    val model    = project.schema.getModelByName_!(typeName)
    val id       = parseIdGCValue(jsObject, model)
    val idStr    = id.value.toString

    val newJsObject = JsObject(jsObject.fields.filter(_._1 != "_typeName"))

    Try {
      GCValueJsonFormatter.readModelAwareGcValue(model)(newJsObject).get
    } match {
      case Success(x)                             => Good(ImportNode(id, model, x))
      case Failure(e: UnknownFieldException)      => Bad(new Exception(s"The model ${model.name} with id $idStr has an unknown field '${e.field}' in field list."))
      case Failure(e: InvalidFieldValueException) => Bad(new Exception(s"The model ${model.name} with id $idStr has an invalid value for field' ${e.field}'."))
      case Failure(e)                             => Bad(new Exception(s"The model ${model.name} with id $idStr produced an exception during import: $e."))
    }
  }

  private def convertToImportList(json: JsValue): ImportList = {
    val jsObject     = json.as[JsObject]
    val typeName     = jsObject.value("_typeName").as[String]
    val model        = project.schema.getModelByName_!(typeName)
    val id           = parseIdGCValue(jsObject, model)
    val fieldName    = jsObject.value.filterKeys(k => k != "_typeName" && k != "id").keys.head
    val field        = model.getScalarFieldByName_!(fieldName)
    val jsonForField = jsObject.value(fieldName)
    val gcValue =
      GCValueJsonFormatter
        .readListGCValue(field)(jsonForField)
        .getOrElse(sys.error(s"conversion to GcValue for ${field.name} and value $jsonForField failed"))
    ImportList(ImportIdentifier(typeName, id), field, gcValue)
  }

  // TODO: do4gr should think about whether Import/Export works with custom names for id fields. I think it does but that should be validated.
  def parseIdGCValue(input: JsObject, model: Model): IdGCValue = model.idField_!.typeIdentifier match {
    case TypeIdentifier.UUID => UuidGCValue.parse_!(input.value("id").as[String])
    case TypeIdentifier.Cuid => StringIdGCValue(input.value("id").as[String])
    case x                   => sys.error("TypeIdentifier not yet supported in Import as ID. " + x)
  }

  private def convertToImportRelation(json: JsValue): ImportRelation = {
    val array         = json.as[JsArray]
    val leftJsObject  = array.value.head.as[JsObject]
    val rightJsObject = array.value.last.as[JsObject]
    val leftModel     = project.schema.getModelByName_!(leftJsObject("_typeName").as[String])
    val rightModel    = project.schema.getModelByName_!(rightJsObject("_typeName").as[String])
    val leftId        = parseIdGCValue(leftJsObject, leftModel)
    val rightId       = parseIdGCValue(rightJsObject, rightModel)
    val left          = ImportRelationSide(ImportIdentifier(leftModel.name, leftId), leftJsObject.value.get("fieldName").flatMap(_.asOpt[String]))
    val right         = ImportRelationSide(ImportIdentifier(rightModel.name, rightId), rightJsObject.value.get("fieldName").flatMap(_.asOpt[String]))

    ImportRelation(left, right)
  }

  private def generateImportNodesDBActions(nodes: Vector[ImportNode]): Vector[ImportNodes] = {
    val creates                                                = nodes.map(node => CreateDataItemImport(project, node.model, PrismaArgs(node.values)))
    val groupedItems: Map[Model, Vector[CreateDataItemImport]] = creates.groupBy(_.model)
    groupedItems.map { case (model, group) => ImportNodes(project, model, group.map(_.args)) }.toVector
  }

  private def generateImportRelationsDBActions(relations: Vector[ImportRelation]): Vector[ImportRelations] = {
    val createRows = relations.map { element =>
      val (left, right) = (element.left, element.right) match {
        case (l, r) if l.fieldName.isDefined => (l, r)
        case (l, r) if r.fieldName.isDefined => (r, l)
        case _                               => throw sys.error("Invalid ImportRelation at least one fieldName needs to be defined.")
      }

      val fromModel                                                 = project.schema.getModelByName_!(left.identifier.typeName)
      val fromField                                                 = fromModel.getRelationFieldByName_!(left.fieldName.get)
      val relationSide: com.prisma.shared.models.RelationSide.Value = fromField.relationSide
      val relation: Relation                                        = fromField.relation
      val aValue                                                    = if (relationSide == RelationSide.A) left.identifier.id else right.identifier.id
      val bValue                                                    = if (relationSide == RelationSide.A) right.identifier.id else left.identifier.id
      CreateRelationRow(project, relation, aValue, bValue)
    }
    val groupedItems = createRows.groupBy(_.relation)
    groupedItems.map { case (relation, group) => ImportRelations(project, relation, group.map(item => (item.a, item.b))) }.toVector
  }

  private def generateImportListsDBActions(lists: Vector[ImportList]): Vector[ImportScalarLists] = {
    lists
      .groupBy(_.field)
      .map {
        case (field: ScalarField, importLists: Vector[ImportList]) =>
          val emptyListValue                                             = ListGCValue(Vector.empty)
          val listValuesGroupedById: Map[IdGCValue, Vector[ListGCValue]] = importLists.groupBy(_.identifier.id).mapValues(_.map(_.values))
          val listValuesMerged: Map[IdGCValue, ListGCValue]              = listValuesGroupedById.mapValues(_.foldLeft(emptyListValue)(_ ++ _))
          ImportScalarLists(project, field, listValuesMerged)
      }
      .toVector
  }
}
