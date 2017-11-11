package cool.graph.client.mutations

import cool.graph.shared.mutactions.MutationTypes.ArgumentValue
import cool.graph.Types.Id
import cool.graph.shared.errors.UserAPIErrors.RelationIsRequired
import cool.graph.client.database.DataResolver
import cool.graph.client.mutactions._
import cool.graph.client.schema.SchemaBuilderConstants
import cool.graph.cuid.Cuid.createCuid
import cool.graph.shared.errors.UserAPIErrors
import cool.graph.shared.models.{Field, Model, Project}
import cool.graph.shared.mutactions.InvalidInputClientSqlMutaction
import cool.graph.{ClientSqlMutaction, DataItem}
import scaldi.Injector

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class SqlMutactions(dataResolver: DataResolver) {
  case class ParentInfo(model: Model, field: Field, id: Id)
  case class CreateMutactionsResult(createMutaction: CreateDataItem, nestedMutactions: Seq[ClientSqlMutaction]) {
    def allMutactions: List[ClientSqlMutaction] = List(createMutaction) ++ nestedMutactions
  }

  def getMutactionsForDelete(model: Model, project: Project, id: Id, previousValues: DataItem)(implicit inj: Injector): List[ClientSqlMutaction] = {

    val requiredRelationViolations     = model.relationFields.flatMap(field => { checkIfRemovalWouldFailARequiredRelation(field, id, project) })
    val removeFromConnectionMutactions = model.relationFields.map(field => RemoveDataItemFromManyRelationByToId(project.id, field, id))
    val deleteItemMutaction            = DeleteDataItem(project, model, id, previousValues)

    requiredRelationViolations ++ removeFromConnectionMutactions ++ List(deleteItemMutaction)
  }

  def getMutactionsForUpdate(project: Project, model: Model, args: CoolArgs, id: Id, previousValues: DataItem, requestId: String)(
      implicit inj: Injector): List[ClientSqlMutaction] = {

    val updateMutaction      = getUpdateMutaction(project, model, args, id, previousValues)
    val forFlatManyRelations = getAddToRelationMutactionsForIdListsForUpdate(project, model, args, fromId = id)
    val forFlatOneRelation   = getAddToRelationMutactionsForIdFieldsForUpdate(project, model, args, fromId = id)
    val forComplexMutactions = getComplexMutactions(project, model, args, fromId = id, requestId = requestId)

    updateMutaction.toList ++ forFlatManyRelations ++ forComplexMutactions ++ forFlatOneRelation
  }

  def getMutactionsForCreate(project: Project,
                             model: Model,
                             args: CoolArgs,
                             allowSettingManagedFields: Boolean,
                             id: Id = createCuid(),
                             parentInfo: Option[ParentInfo] = None,
                             requestId: String)(implicit inj: Injector): CreateMutactionsResult = {

    val createMutaction      = getCreateMutaction(project, model, args, id, allowSettingManagedFields, requestId)
    val forFlatManyRelations = getAddToRelationMutactionsForIdListsForCreate(project, model, args, fromId = createMutaction.id)
    val forFlatOneRelation   = getAddToRelationMutactionsForIdFieldsForCreate(project, model, args, fromId = createMutaction.id)
    val forComplexRelations  = getComplexMutactions(project, model, args, fromId = createMutaction.id, requestId = requestId)

    val relationToParent = parentInfo.map { parent =>
      AddDataItemToManyRelation(project = project, fromModel = parent.model, fromField = parent.field, fromId = parent.id, toId = id, toIdAlreadyInDB = false)
    }

    val requiredOneRelationFields = model.relationFields.filter(f => f.isRequired && !f.isList)
    val requiredRelationViolations = requiredOneRelationFields
      .filter { field =>
        val isRelatedById      = args.getFieldValueAs(field, suffix = SchemaBuilderConstants.idSuffix).flatten.isDefined
        val isRelatedByComplex = args.getFieldValueAs(field).flatten.isDefined
        val isRelatedToParent = parentInfo match {
          case None         => false
          case Some(parent) => parent.field.relation.map(_.id) == field.relation.map(_.id)
        }
        !isRelatedById && !isRelatedByComplex && !isRelatedToParent
      }
      .map(field => InvalidInputClientSqlMutaction(RelationIsRequired(field.name, model.name)))

    val nestedMutactions: Seq[ClientSqlMutaction] = forFlatManyRelations ++ forComplexRelations ++ forFlatOneRelation ++ relationToParent

    val correctExecutionOrder = nestedMutactions.sortWith { (x, _) =>
      x.isInstanceOf[RemoveDataItemFromManyRelationByFromId]
    }

    val result = CreateMutactionsResult(createMutaction = createMutaction, nestedMutactions = correctExecutionOrder ++ requiredRelationViolations)
    result
  }

  def getCreateMutaction(project: Project, model: Model, args: CoolArgs, id: Id, allowSettingManagedFields: Boolean, requestId: String)(
      implicit inj: Injector): CreateDataItem = {
    val scalarArguments = for {
      field      <- model.scalarFields
      fieldValue <- args.getFieldValueAs[Any](field)
    } yield {
      ArgumentValue(field.name, fieldValue, field)
    }

    def checkNullInputOnRequiredFieldWithDefaultValue(x: ArgumentValue) =
      if (x.field.get.isRequired && x.value == None && x.field.get.defaultValue.isDefined) throw UserAPIErrors.InputInvalid("null", x.name, model.name)
    scalarArguments.map(checkNullInputOnRequiredFieldWithDefaultValue)

    CreateDataItem(
      project = project,
      model = model,
      values = scalarArguments :+ ArgumentValue("id", id, model.getFieldByName("id")),
      allowSettingManagedFields = allowSettingManagedFields,
      requestId = Some(requestId),
      originalArgs = Some(args)
    )
  }

  def getUpdateMutaction(project: Project, model: Model, args: CoolArgs, id: Id, previousValues: DataItem)(implicit inj: Injector): Option[UpdateDataItem] = {
    val scalarArguments = for {
      field      <- model.scalarFields.filter(_.name != "id")
      fieldValue <- args.getFieldValueAs[Any](field)
    } yield {
      ArgumentValue(field.name, fieldValue, field)
    }
    if (scalarArguments.nonEmpty) {
      Some(
        UpdateDataItem(project = project,
                       model = model,
                       id = id,
                       values = scalarArguments,
                       originalArgs = Some(args),
                       previousValues = previousValues,
                       itemExists = true))
    } else None
  }

  def getAddToRelationMutactionsForIdListsForCreate(project: Project, model: Model, args: CoolArgs, fromId: Id): Seq[ClientSqlMutaction] = {
    val x = for {
      field <- model.relationFields if field.isList
      toIds <- args.getFieldValuesAs[Id](field, SchemaBuilderConstants.idListSuffix)
    } yield {

      val removeOldToRelations: List[ClientSqlMutaction] = if (field.isOneToManyRelation(project)) {
        toIds.map(toId => Some(RemoveDataItemFromManyRelationByToId(project.id, field, toId))).toList.flatten
      } else List()

      val relationsToAdd = toIds.map { toId =>
        AddDataItemToManyRelation(project = project, fromModel = model, fromField = field, fromId = fromId, toId = toId)
      }
      removeOldToRelations ++ relationsToAdd
    }
    x.flatten
  }

  def getAddToRelationMutactionsForIdListsForUpdate(project: Project, model: Model, args: CoolArgs, fromId: Id): Seq[ClientSqlMutaction] = {
    val x = for {
      field <- model.relationFields if field.isList
      toIds <- args.getFieldValuesAs[Id](field, SchemaBuilderConstants.idListSuffix)
    } yield {

      val removeOldFromRelation = List(checkIfUpdateWouldFailARequiredManyRelation(field, fromId, toIds.toList, project),
                                       Some(RemoveDataItemFromManyRelationByFromId(project.id, field, fromId))).flatten

      val removeOldToRelations: List[ClientSqlMutaction] = if (field.isOneToManyRelation(project)) {
        toIds.map(toId => RemoveDataItemFromManyRelationByToId(project.id, field, toId)).toList
      } else List()

      val relationsToAdd = toIds.map { toId =>
        AddDataItemToManyRelation(project = project, fromModel = model, fromField = field, fromId = fromId, toId = toId)
      }
      removeOldFromRelation ++ removeOldToRelations ++ relationsToAdd
    }
    x.flatten
  }

  def getAddToRelationMutactionsForIdFieldsForCreate(project: Project, model: Model, args: CoolArgs, fromId: Id): Seq[ClientSqlMutaction] = {
    val x: Seq[Iterable[ClientSqlMutaction]] = for {
      field   <- model.relationFields if !field.isList
      toIdOpt <- args.getFieldValueAs[String](field, suffix = SchemaBuilderConstants.idSuffix)
    } yield {

      val removeOldToRelation: List[ClientSqlMutaction] = if (field.isOneToOneRelation(project)) {
        toIdOpt
          .map { toId =>
            List(
              Some(RemoveDataItemFromManyRelationByToId(project.id, field, toId)),
              checkIfRemovalWouldFailARequiredRelation(field.relatedFieldEager(project), toId, project)
            ).flatten
          }
          .getOrElse(List.empty)
      } else List()

      val addToRelation = toIdOpt.map { toId =>
        AddDataItemToManyRelation(project = project, fromModel = model, fromField = field, fromId = fromId, toId = toId)
      }
      // FIXME: removes must be first here; How could we make that clearer?
      removeOldToRelation ++ addToRelation
    }
    x.flatten
  }

  def getAddToRelationMutactionsForIdFieldsForUpdate(project: Project, model: Model, args: CoolArgs, fromId: Id): Seq[ClientSqlMutaction] = {
    val x: Seq[Iterable[ClientSqlMutaction]] = for {
      field   <- model.relationFields if !field.isList
      toIdOpt <- args.getFieldValueAs[String](field, suffix = SchemaBuilderConstants.idSuffix)
    } yield {

      val removeOldFromRelation = List(Some(RemoveDataItemFromManyRelationByFromId(project.id, field, fromId)),
                                       checkIfUpdateWouldFailARequiredOneRelation(field, fromId, toIdOpt, project)).flatten

      val removeOldToRelation: List[ClientSqlMutaction] = if (field.isOneToOneRelation(project)) {
        toIdOpt
          .map { toId =>
            List(
              Some(RemoveDataItemFromManyRelationByToId(project.id, field, toId)),
              checkIfUpdateWouldFailARequiredOneRelation(field.relatedFieldEager(project), toId, Some(fromId), project)
            ).flatten
          }
          .getOrElse(List.empty)
      } else List()

      val addToRelation = toIdOpt.map { toId =>
        AddDataItemToManyRelation(project = project, fromModel = model, fromField = field, fromId = fromId, toId = toId)
      }
      // FIXME: removes must be first here; How could we make that clearer?
      removeOldFromRelation ++ removeOldToRelation ++ addToRelation
    }
    x.flatten
  }

  private def checkIfRemovalWouldFailARequiredRelation(field: Field, fromId: String, project: Project): Option[InvalidInputClientSqlMutaction] = {
    val isInvalid = () => dataResolver.resolveByRelation(fromField = field, fromModelId = fromId, args = None).map(_.items.nonEmpty)

    runRequiredRelationCheckWithInvalidFunction(field, project, isInvalid)
  }

  private def checkIfUpdateWouldFailARequiredOneRelation(field: Field,
                                                         fromId: String,
                                                         toId: Option[String],
                                                         project: Project): Option[InvalidInputClientSqlMutaction] = {
    val isInvalid = () =>
      dataResolver.resolveByRelation(fromField = field, fromModelId = fromId, args = None).map {
        _.items match {
          case x :: _ => x.id != toId.getOrElse("")
          case _      => false
        }
    }
    runRequiredRelationCheckWithInvalidFunction(field, project, isInvalid)
  }

  private def checkIfUpdateWouldFailARequiredManyRelation(field: Field,
                                                          fromId: String,
                                                          toIds: List[String],
                                                          project: Project): Option[InvalidInputClientSqlMutaction] = {
    val isInvalid = () =>
      dataResolver
        .resolveByRelation(fromField = field, fromModelId = fromId, args = None)
        .map(_.items.exists(x => !toIds.contains(x.id)))

    runRequiredRelationCheckWithInvalidFunction(field, project, isInvalid)
  }

  private def runRequiredRelationCheckWithInvalidFunction(field: Field, project: Project, isInvalid: () => Future[Boolean]) = {
    val relatedField = field.relatedFieldEager(project)
    val relatedModel = field.relatedModel_!(project)
    if (relatedField.isRequired && !relatedField.isList) {
      Some(InvalidInputClientSqlMutaction(RelationIsRequired(fieldName = relatedField.name, typeName = relatedModel.name), isInvalid = isInvalid))
    } else None
  }

  def getComplexMutactions(project: Project, model: Model, args: CoolArgs, fromId: Id, requestId: String)(implicit inj: Injector): Seq[ClientSqlMutaction] = {
    val x: Seq[List[ClientSqlMutaction]] = for {
      field    <- model.relationFields
      subArgs  <- args.subArgsList(field)
      subModel = field.relatedModel(project).get
    } yield {

      val removeOldFromRelation =
        List(checkIfRemovalWouldFailARequiredRelation(field, fromId, project), Some(RemoveDataItemFromManyRelationByFromId(project.id, field, fromId))).flatten

      val allowSettingManagedFields = false

      val itemsToCreate = subArgs.flatMap { subArg =>
        getMutactionsForCreate(project, subModel, subArg, allowSettingManagedFields, parentInfo = Some(ParentInfo(model, field, fromId)), requestId = requestId).allMutactions
      }

      removeOldFromRelation ++ itemsToCreate
    }
    x.flatten
  }
}
