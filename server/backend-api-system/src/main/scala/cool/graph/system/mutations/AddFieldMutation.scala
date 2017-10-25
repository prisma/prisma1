package cool.graph.system.mutations

import cool.graph.GCDataTypes.{GCStringConverter, GCValue}
import cool.graph._
import cool.graph.cuid.Cuid
import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.errors.{SystemErrors, UserInputErrors}
import cool.graph.shared.models
import cool.graph.shared.models.TypeIdentifier.TypeIdentifier
import cool.graph.shared.models.{Enum, Model, Project}
import cool.graph.shared.mutactions.InvalidInput
import cool.graph.shared.schema.CustomScalarTypes
import cool.graph.system.database.SystemFields
import cool.graph.system.database.client.ClientDbQueries
import cool.graph.system.mutactions.client.{CreateColumn, OverwriteAllRowsForColumn}
import cool.graph.system.mutactions.internal.{BumpProjectRevision, CreateField, CreateSystemFieldIfNotExists, InvalidateSchema}
import org.scalactic.{Bad, Good}
import sangria.relay.Mutation
import scaldi.Injector

import scala.util.{Failure, Success}

case class AddFieldMutation(
    client: models.Client,
    project: models.Project,
    args: AddFieldInput,
    projectDbsFn: models.Project => InternalAndProjectDbs,
    clientDbQueries: ClientDbQueries
)(implicit inj: Injector)
    extends InternalProjectMutation[AddFieldMutationPayload] {

  val model: Model       = project.getModelById_!(args.modelId)
  val enum: Option[Enum] = args.enumId.flatMap(project.getEnumById)
  val gcStringConverter  = GCStringConverter(args.typeIdentifier, args.isList)

  val defaultValue: Option[GCValue] = for {
    defaultValue <- args.defaultValue
    gcValue      <- gcStringConverter.toGCValue(defaultValue).toOption
  } yield gcValue

  val verifyDefaultValue: List[UserInputErrors.InvalidValueForScalarType] = {
    args.defaultValue.map(dV => GCStringConverter(args.typeIdentifier, args.isList).toGCValue(dV)) match {
      case Some(Good(_))    => List.empty
      case Some(Bad(error)) => List(error)
      case None             => List.empty
    }
  }

  val newField: models.Field = models.Field(
    id = args.id,
    name = args.name,
    typeIdentifier = args.typeIdentifier,
    description = args.description,
    isRequired = args.isRequired,
    isList = args.isList,
    isUnique = args.isUnique,
    isSystem = false,
    isReadonly = false,
    enum = enum,
    defaultValue = defaultValue,
    relation = None,
    relationSide = None
  )

  val updatedModel: Model     = model.copy(fields = model.fields :+ newField)
  val updatedProject: Project = project.copy(models = project.models.filter(_.id != model.id) :+ updatedModel)

  override def prepareActions(): List[Mutaction] = {
    newField.isScalar match {
      case _ if verifyDefaultValue.nonEmpty =>
        actions = List(InvalidInput(verifyDefaultValue.head))

      case false =>
        actions = List(InvalidInput(SystemErrors.IsNotScalar(args.typeIdentifier.toString)))

      case true =>
        if (SystemFields.isReservedFieldName(newField.name)) {
          val systemFieldAction = SystemFields.generateSystemFieldFromInput(newField) match {
            case Success(field) => CreateSystemFieldIfNotExists(project, model, field.copy(id = newField.id))
            case Failure(err)   => InvalidInput(SystemErrors.InvalidPredefinedFieldFormat(newField.name, err.getMessage))
          }

          actions = List(systemFieldAction)
        } else {
          actions = regularFieldCreationMutactions
        }

        actions = actions ++ List(BumpProjectRevision(project = project), InvalidateSchema(project = project))
    }

    actions
  }

  def regularFieldCreationMutactions: List[Mutaction] = {
    val migrationAction = if (args.migrationValue.isDefined) {
      List(
        OverwriteAllRowsForColumn(
          project.id,
          model,
          newField,
          CustomScalarTypes.parseValueFromString(args.migrationValue.get, newField.typeIdentifier, newField.isList)
        ))
    } else {
      Nil
    }

    val createFieldClientDbAction  = CreateColumn(project.id, model, newField)
    val createFieldProjectDbAction = CreateField(project, model, newField, args.migrationValue, clientDbQueries)

    List(createFieldClientDbAction) ++ migrationAction ++ List(createFieldProjectDbAction)
  }

  override def getReturnValue: Option[AddFieldMutationPayload] =
    Some(AddFieldMutationPayload(clientMutationId = args.clientMutationId, project = updatedProject, model = updatedModel, field = newField))
}

case class AddFieldMutationPayload(clientMutationId: Option[String], project: models.Project, model: models.Model, field: models.Field) extends Mutation

case class AddFieldInput(
    clientMutationId: Option[String],
    modelId: String,
    name: String,
    typeIdentifier: TypeIdentifier,
    isRequired: Boolean,
    isList: Boolean,
    isUnique: Boolean,
    relationId: Option[String],
    defaultValue: Option[String],
    migrationValue: Option[String],
    description: Option[String],
    enumId: Option[String]
) {
  val id: String = Cuid.createCuid()
}
