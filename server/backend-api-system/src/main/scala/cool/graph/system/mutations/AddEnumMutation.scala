package cool.graph.system.mutations

import cool.graph.cuid.Cuid
import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.models
import cool.graph.shared.models.{Enum, Project}
import cool.graph.system.mutactions.internal.{BumpProjectRevision, CreateEnum, InvalidateSchema}
import cool.graph.{InternalProjectMutation, Mutaction}
import sangria.relay.Mutation
import scaldi.Injector

case class AddEnumMutation(client: models.Client, project: models.Project, args: AddEnumInput, projectDbsFn: models.Project => InternalAndProjectDbs)(
    implicit inj: Injector)
    extends InternalProjectMutation[AddEnumMutationPayload] {

  val enum: Enum              = Enum(args.id, name = args.name, values = args.values)
  val updatedProject: Project = project.copy(enums = project.enums :+ enum)

  override def prepareActions(): List[Mutaction] = {
    this.actions = List(CreateEnum(project, enum), BumpProjectRevision(project = project), InvalidateSchema(project))
    this.actions
  }

  override def getReturnValue(): Option[AddEnumMutationPayload] = {
    Some(AddEnumMutationPayload(args.clientMutationId, updatedProject, enum))
  }
}

case class AddEnumMutationPayload(clientMutationId: Option[String], project: models.Project, enum: models.Enum) extends Mutation

case class AddEnumInput(clientMutationId: Option[String], projectId: String, name: String, values: Seq[String]) {
  val id: String = Cuid.createCuid()
}
