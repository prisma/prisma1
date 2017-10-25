package cool.graph.system.mutations

import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.errors.SystemErrors.{EmailAlreadyIsTheProjectOwner, NewOwnerOfAProjectNeedsAClientId, OnlyOwnerOfProjectCanTransferOwnership}
import cool.graph.shared.models
import cool.graph.shared.models.{Client, Project, Seat}
import cool.graph.system.database.finder.ProjectQueries
import cool.graph.system.mutactions.internal.{CreateSeat, DeleteSeat, InvalidateSchema, UpdateProject}
import cool.graph.{InternalProjectMutation, Mutaction}
import sangria.relay.Mutation
import scaldi.{Injectable, Injector}

case class TransferOwnershipMutation(client: models.Client,
                                     project: models.Project,
                                     args: TransferOwnershipInput,
                                     projectDbsFn: models.Project => InternalAndProjectDbs)(implicit inj: Injector)
    extends InternalProjectMutation[TransferOwnershipMutationPayload]
    with Injectable {

  // note: this mutation does not bump revision as collaborators are not part of the project structure

  val projectQueries: ProjectQueries = inject[ProjectQueries](identified by "projectQueries")

  val oldOwnerSeat: models.Seat = if (project.ownerId == client.id) project.seatByClientId_!(client.id) else throw OnlyOwnerOfProjectCanTransferOwnership()
  val newOwnerSeat: models.Seat = project.seatByEmail_!(args.email)

  if (newOwnerSeat.clientId.isEmpty) throw NewOwnerOfAProjectNeedsAClientId()
  if (args.email == oldOwnerSeat.email) throw EmailAlreadyIsTheProjectOwner(args.email)

  val unchangedSeats: List[Seat]           = project.seats.filter(seat => seat.id != oldOwnerSeat.id && seat.id != newOwnerSeat.id)
  val projectWithOutSwitchedSeats: Project = project.copy(seats = unchangedSeats)
  val updatedProject: Project              = project.copy(seats = unchangedSeats :+ oldOwnerSeat.copy(isOwner = false) :+ newOwnerSeat.copy(isOwner = true))

  override def prepareActions(): List[Mutaction] = {

    val deleteOldNewOwnerSeat = DeleteSeat(client, project, newOwnerSeat, internalDatabase = internalDatabase.databaseDef)
    val deleteOldOldOwnerSeat = DeleteSeat(client, project, oldOwnerSeat, internalDatabase = internalDatabase.databaseDef)

    val addUpdatedNewOwnerSeat =
      CreateSeat(
        client,
        projectWithOutSwitchedSeats,
        newOwnerSeat.copy(isOwner = true),
        internalDatabase = internalDatabase.databaseDef,
        ignoreDuplicateNameVerificationError = true
      )
    val addUpdatedOldOwnerSeat =
      CreateSeat(
        client,
        projectWithOutSwitchedSeats,
        oldOwnerSeat.copy(isOwner = false),
        internalDatabase = internalDatabase.databaseDef,
        ignoreDuplicateNameVerificationError = true
      )

    val updateProject = UpdateProject(client,
                                      project,
                                      updatedProject.copy(ownerId = newOwnerSeat.clientId.get),
                                      internalDatabase = internalDatabase.databaseDef,
                                      projectQueries = projectQueries)

    actions =
      List(deleteOldNewOwnerSeat, deleteOldOldOwnerSeat, addUpdatedNewOwnerSeat, addUpdatedOldOwnerSeat, updateProject, InvalidateSchema(updatedProject))

    actions
  }

  override def getReturnValue: Option[TransferOwnershipMutationPayload] =
    Some(TransferOwnershipMutationPayload(clientMutationId = args.clientMutationId, project = updatedProject, ownerEmail = newOwnerSeat.email))

}

case class TransferOwnershipMutationPayload(clientMutationId: Option[String], project: models.Project, ownerEmail: String) extends Mutation

case class TransferOwnershipInput(clientMutationId: Option[String], projectId: String, email: String)
