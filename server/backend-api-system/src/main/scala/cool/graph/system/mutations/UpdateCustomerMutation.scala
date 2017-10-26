package cool.graph.system.mutations

import cool.graph.shared.database.InternalDatabase
import cool.graph.shared.models
import cool.graph.shared.models.Client
import cool.graph.system.mutactions.internal.{UpdateClient, UpdateCustomerInAuth0}
import cool.graph.{InternalMutation, Mutaction}
import sangria.relay.Mutation
import scaldi.{Injectable, Injector}

case class UpdateCustomerMutation(
    client: Client,
    args: UpdateClientInput,
    internalDatabase: InternalDatabase
)(implicit inj: Injector)
    extends InternalMutation[UpdateClientMutationPayload]
    with Injectable {

  var updatedClient: Option[models.Client] = None

  def mergeInputValuesToClient(existingClient: Client, updateValues: UpdateClientInput): Client = {
    existingClient.copy(
      name = updateValues.name.getOrElse(existingClient.name),
      email = updateValues.email.getOrElse(existingClient.email)
    )
  }

  override def prepareActions(): List[Mutaction] = {

    updatedClient = Some(mergeInputValuesToClient(client, args))

    val updateModel = UpdateClient(oldClient = client, client = updatedClient.get)

    val updateAuth0 = client.isAuth0IdentityProviderEmail match {
      case true  => List(UpdateCustomerInAuth0(oldClient = client, client = updatedClient.get))
      case false => List()
    }

    actions = List(updateModel) ++ updateAuth0
    actions
  }

  override def getReturnValue(): Option[UpdateClientMutationPayload] = {
    Some(new UpdateClientMutationPayload(clientMutationId = args.clientMutationId, client = updatedClient.get))
  }
}

case class UpdateClientMutationPayload(clientMutationId: Option[String], client: Client) extends Mutation

case class UpdateClientInput(clientMutationId: Option[String], name: Option[String], email: Option[String])
