package cool.graph.system.mutations

import cool.graph.shared.database.InternalDatabase
import cool.graph.shared.models
import cool.graph.shared.models.Client
import cool.graph.system.mutactions.internal.UpdateClientPassword
import cool.graph.{InternalMutation, Mutaction}
import sangria.relay.Mutation
import scaldi.Injector

case class UpdateClientPasswordMutation(
    client: Client,
    args: UpdateClientPasswordInput,
    internalDatabase: InternalDatabase
)(implicit inj: Injector)
    extends InternalMutation[UpdateClientPasswordMutationPayload] {

  var updatedClient: Option[models.Client] = None

  override def prepareActions(): List[Mutaction] = {
    val updateClientPassword = UpdateClientPassword(client = client, oldPassword = args.oldPassword, newPassword = args.newPassword)

    updatedClient = Some(client)
    actions = List(updateClientPassword)
    actions
  }

  override def getReturnValue(): Option[UpdateClientPasswordMutationPayload] = {
    Some(new UpdateClientPasswordMutationPayload(clientMutationId = args.clientMutationId, client = updatedClient.get))
  }
}

case class UpdateClientPasswordMutationPayload(clientMutationId: Option[String], client: Client) extends Mutation

case class UpdateClientPasswordInput(clientMutationId: Option[String], newPassword: String, oldPassword: String)
