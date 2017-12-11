package cool.graph.api.mutations

import cool.graph.api.database.{DataItem, DataResolver}
import cool.graph.api.database.mutactions.ClientSqlMutaction
import cool.graph.api.database.mutactions.mutactions._
import cool.graph.api.mutations.MutationTypes.ArgumentValue
import cool.graph.api.schema.APIErrors.RelationIsRequired
import cool.graph.api.schema.{APIErrors, SchemaBuilderConstants}
import cool.graph.cuid.Cuid.createCuid
import cool.graph.shared.models.IdType.Id
import cool.graph.shared.models.{Field, Model, Project}

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class SqlMutactions(dataResolver: DataResolver) {
  case class ParentInfo(model: Model, field: Field, id: Id)
  case class CreateMutactionsResult(createMutaction: CreateDataItem, nestedMutactions: Seq[ClientSqlMutaction]) {
    def allMutactions: Vector[ClientSqlMutaction] = Vector(createMutaction) ++ nestedMutactions
  }

  def getMutactionsForDelete(model: Model, project: Project, id: Id, previousValues: DataItem): List[ClientSqlMutaction] = {

    val requiredRelationViolations     = model.relationFields.flatMap(field => checkIfRemovalWouldFailARequiredRelation(field, id, project))
    val removeFromConnectionMutactions = model.relationFields.map(field => RemoveDataItemFromManyRelationByToId(project.id, field, id))
    val deleteItemMutaction            = DeleteDataItem(project, model, id, previousValues)

    requiredRelationViolations ++ removeFromConnectionMutactions ++ List(deleteItemMutaction)
  }

  def getMutactionsForUpdate(project: Project, model: Model, args: CoolArgs, id: Id, previousValues: DataItem, requestId: String): List[ClientSqlMutaction] = {

    val updateMutaction = getUpdateMutaction(project, model, args, id, previousValues)

    updateMutaction.toList
  }

  def getMutactionsForCreate(
      project: Project,
      model: Model,
      args: CoolArgs,
      id: Id = createCuid(),
      parentInfo: Option[ParentInfo] = None
  ): CreateMutactionsResult = {

    val createMutaction = getCreateMutaction(project, model, args, id)
    val relationToParent = parentInfo.map { parent =>
      AddDataItemToManyRelation(project = project, fromModel = parent.model, fromField = parent.field, fromId = parent.id, toId = id, toIdAlreadyInDB = false)
    }

    val nested = getMutactionsForNestedMutation(project, model, args, fromId = id)

    CreateMutactionsResult(createMutaction = createMutaction, nestedMutactions = relationToParent.toVector ++ nested)
  }

  def getCreateMutaction(project: Project, model: Model, args: CoolArgs, id: Id): CreateDataItem = {
    val scalarArguments = for {
      field      <- model.scalarFields
      fieldValue <- args.getFieldValueAs[Any](field)
    } yield {
      if (field.isRequired && field.defaultValue.isDefined && fieldValue.isEmpty) {
        throw APIErrors.InputInvalid("null", field.name, model.name)
      }
      ArgumentValue(field.name, fieldValue)
    }

    CreateDataItem(
      project = project,
      model = model,
      values = scalarArguments :+ ArgumentValue("id", id),
      originalArgs = Some(args)
    )
  }

  def getUpdateMutaction(project: Project, model: Model, args: CoolArgs, id: Id, previousValues: DataItem): Option[UpdateDataItem] = {
    val scalarArguments = for {
      field      <- model.scalarFields.filter(_.name != "id")
      fieldValue <- args.getFieldValueAs[Any](field)
    } yield {
      ArgumentValue(field.name, fieldValue)
    }
    if (scalarArguments.nonEmpty) {
      Some(
        UpdateDataItem(
          project = project,
          model = model,
          id = id,
          values = scalarArguments,
          originalArgs = Some(args),
          previousValues = previousValues,
          itemExists = true
        ))
    } else None
  }

  def getMutactionsForNestedMutation(project: Project, model: Model, args: CoolArgs, fromId: Id): Seq[ClientSqlMutaction] = {
    val x = for {
      field    <- model.relationFields
      args     <- args.subArgs(field) // this is the hash input object containing the stuff
      subModel = field.relatedModel_!(project)
    } yield {
      args match {
        case Some(args) => getMutactionsForNestedCreateMutation(project, subModel, field, args, ParentInfo(model, field, fromId))
        case None       => Vector.empty // if the user specifies an explicit null for the relation field
      }
    }
    x.flatten
  }

  def getMutactionsForNestedCreateMutation(
      project: Project,
      model: Model,
      field: Field,
      args: CoolArgs,
      parentInfo: ParentInfo
  ): Seq[ClientSqlMutaction] = {
    val x = for {
      args <- if (field.isList) {
               args.subArgsList("create")
             } else {
               args.subArgs("create").map(_.toVector)
             }
    } yield {
      args.flatMap { args =>
        getMutactionsForCreate(project, model, args, parentInfo = Some(parentInfo)).allMutactions
      }
    }
    x.getOrElse(Vector.empty)
  }

  private def checkIfRemovalWouldFailARequiredRelation(field: Field, fromId: String, project: Project): Option[InvalidInputClientSqlMutaction] = {
    val isInvalid = () => dataResolver.resolveByRelation(fromField = field, fromModelId = fromId, args = None).map(_.items.nonEmpty)

    runRequiredRelationCheckWithInvalidFunction(field, project, isInvalid)
  }

  private def runRequiredRelationCheckWithInvalidFunction(field: Field,
                                                          project: Project,
                                                          isInvalid: () => Future[Boolean]): Option[InvalidInputClientSqlMutaction] = {
    val relatedField = field.relatedFieldEager(project)
    val relatedModel = field.relatedModel_!(project)
    if (relatedField.isRequired && !relatedField.isList) {
      Some(InvalidInputClientSqlMutaction(RelationIsRequired(fieldName = relatedField.name, typeName = relatedModel.name), isInvalid = isInvalid))
    } else None
  }

  def getComplexMutactions(project: Project, model: Model, args: CoolArgs, fromId: Id): Seq[ClientSqlMutaction] = {
    val x: Seq[List[ClientSqlMutaction]] = for {
      field     <- model.relationFields
      nestedArg <- args.subArgs(field).flatten
      subArgs   <- nestedArg.subArgs("create")
      subModel  = field.relatedModel(project).get
    } yield {

      val removeOldFromRelation =
        List(checkIfRemovalWouldFailARequiredRelation(field, fromId, project), Some(RemoveDataItemFromManyRelationByFromId(project.id, field, fromId))).flatten

      val itemsToCreate = subArgs.toVector.flatMap { subArg =>
        getMutactionsForCreate(project, subModel, subArg, parentInfo = Some(ParentInfo(model, field, fromId))).allMutactions
      }

      removeOldFromRelation ++ itemsToCreate
    }
    x.flatten
  }
}
