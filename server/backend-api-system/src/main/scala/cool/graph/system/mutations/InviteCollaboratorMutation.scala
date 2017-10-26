package cool.graph.system.mutations

import cool.graph.cuid.Cuid
import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.errors.UserInputErrors.CollaboratorProjectWithNameAlreadyExists
import cool.graph.shared.models
import cool.graph.shared.models.{Client, SeatStatus}
import cool.graph.shared.mutactions.InvalidInput
import cool.graph.system.mutactions.internal.{CreateSeat, InvalidateSchema}
import cool.graph.{InternalProjectMutation, Mutaction}
import sangria.relay.Mutation
import scaldi.Injector

case class InviteCollaboratorMutation(client: models.Client,
                                      invitedClient: Option[Client],
                                      project: models.Project,
                                      args: InviteCollaboratorInput,
                                      projectDbsFn: models.Project => InternalAndProjectDbs)(implicit inj: Injector)
    extends InternalProjectMutation[InviteCollaboratorMutationPayload] {

  var newSeat: Option[models.Seat] = None

  // note: this mutation does not bump revision as collaborators are not part of the project structure

  override def prepareActions(): List[Mutaction] = {

    actions = invitedClient match {
      case None =>
        newSeat = Some(
          models.Seat(id = Cuid.createCuid(),
                      name = None,
                      status = SeatStatus.INVITED_TO_PROJECT,
                      isOwner = false,
                      email = args.email,
                      clientId = invitedClient.map(_.id)))
        val addSeat = CreateSeat(client, project, newSeat.get, internalDatabase = internalDatabase.databaseDef)

        List(addSeat, InvalidateSchema(project = project))

      case Some(invitedClient) if invitedClient.projects.map(_.name).contains(project.name) =>
        List(InvalidInput(error = CollaboratorProjectWithNameAlreadyExists(name = project.name)))
      case Some(invitedClient) =>
        newSeat = Some(
          models.Seat(id = Cuid.createCuid(), name = None, status = SeatStatus.JOINED, isOwner = false, email = args.email, clientId = Some(invitedClient.id)))

        val addSeat = CreateSeat(client, project, newSeat.get, internalDatabase = internalDatabase.databaseDef)

        List(addSeat, InvalidateSchema(project = project))
    }

    actions
  }

  override def getReturnValue: Option[InviteCollaboratorMutationPayload] = {
    Some(
      InviteCollaboratorMutationPayload(clientMutationId = args.clientMutationId,
                                        project = project.copy(seats = project.seats :+ newSeat.get),
                                        seat = newSeat.get))
  }
}

case class InviteCollaboratorMutationPayload(clientMutationId: Option[String], project: models.Project, seat: models.Seat) extends Mutation

case class InviteCollaboratorInput(clientMutationId: Option[String], projectId: String, email: String)
