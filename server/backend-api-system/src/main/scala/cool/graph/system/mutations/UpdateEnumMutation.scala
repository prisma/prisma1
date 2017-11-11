package cool.graph.system.mutations

import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.errors.SystemErrors
import cool.graph.shared.models
import cool.graph.shared.models.{Enum, Project}
import cool.graph.system.database.client.ClientDbQueries
import cool.graph.system.mutactions.internal.{BumpProjectRevision, InvalidateSchema, UpdateEnum}
import cool.graph.{InternalProjectMutation, Mutaction}
import sangria.relay.Mutation
import scaldi.Injector

case class UpdateEnumMutation(
    client: models.Client,
    project: models.Project,
    args: UpdateEnumInput,
    projectDbsFn: models.Project => InternalAndProjectDbs,
    clientDbQueries: ClientDbQueries
)(implicit inj: Injector)
    extends InternalProjectMutation[UpdateEnumMutationPayload] {

  val enum: Enum              = project.getEnumById_!(args.enumId)
  val updatedEnum: Enum       = enum.copy(name = args.name.getOrElse(enum.name), values = args.values.getOrElse(enum.values))
  val updatedProject: Project = project.copy(enums = project.enums.filter(_.id != args.enumId) :+ updatedEnum)

  checkIfEnumWithNameAlreadyExists

  private def checkIfEnumWithNameAlreadyExists = args.name.foreach(name => if (enumWithSameName(name)) throw SystemErrors.InvalidEnumName(name))
  private def enumWithSameName(name: String)   = project.enums.exists(enum => enum.name == name && enum.id != args.enumId)

  override def prepareActions(): List[Mutaction] = {
    val migrationArgs                          = MigrateEnumValuesInput(args.clientMutationId, enum, updatedEnum, args.migrationValue)
    val migrateFieldsUsingEnumValuesMutactions = MigrateEnumValuesMutation(client, project, migrationArgs, projectDbsFn, clientDbQueries).prepareActions()

    val updateEnumMutaction = List(UpdateEnum(newEnum = updatedEnum, oldEnum = enum), BumpProjectRevision(project = project), InvalidateSchema(project))

    this.actions ++= migrateFieldsUsingEnumValuesMutactions ++ updateEnumMutaction
    this.actions
  }

  override def getReturnValue: Option[UpdateEnumMutationPayload] = Some(UpdateEnumMutationPayload(args.clientMutationId, updatedProject, updatedEnum))
}

case class UpdateEnumMutationPayload(clientMutationId: Option[String], project: models.Project, enum: models.Enum) extends Mutation

case class UpdateEnumInput(clientMutationId: Option[String], enumId: String, name: Option[String], values: Option[Seq[String]], migrationValue: Option[String])
    extends MutationInput
