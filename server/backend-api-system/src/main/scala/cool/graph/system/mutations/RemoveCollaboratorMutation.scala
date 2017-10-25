package cool.graph.system.mutations

import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.models
import cool.graph.system.mutactions.internal.{DeleteSeat, InvalidateSchema}
import cool.graph.{InternalProjectMutation, Mutaction}
import sangria.relay.Mutation
import scaldi.Injector

case class RemoveCollaboratorMutation(client: models.Client,
                                      project: models.Project,
                                      seat: models.Seat,
                                      args: RemoveCollaboratorInput,
                                      projectDbsFn: models.Project => InternalAndProjectDbs)(implicit inj: Injector)
    extends InternalProjectMutation[RemoveCollaboratorMutationPayload] {

  // note: this mutation does not bump revision as collaborators are not part of the project structure

  override def prepareActions(): List[Mutaction] = {
    val deleteSeat       = DeleteSeat(client, project = project, seat = seat, internalDatabase.databaseDef)
    val invalidateSchema = InvalidateSchema(project = project)
    actions = List(deleteSeat, invalidateSchema)
    actions
  }

  override def getReturnValue: Option[RemoveCollaboratorMutationPayload] = {
    Some(
      RemoveCollaboratorMutationPayload(clientMutationId = args.clientMutationId,
                                        project = project.copy(seats = project.seats.filter(_.id != seat.id)),
                                        seat = seat))
  }
}

case class RemoveCollaboratorMutationPayload(clientMutationId: Option[String], project: models.Project, seat: models.Seat) extends Mutation

case class RemoveCollaboratorInput(clientMutationId: Option[String], projectId: String, email: String)
