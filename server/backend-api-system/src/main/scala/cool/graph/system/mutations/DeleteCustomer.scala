package cool.graph.system.mutations

import cool.graph.shared.database.InternalDatabase
import cool.graph.shared.errors.SystemErrors.InvalidClientId
import cool.graph.shared.models
import cool.graph.shared.models.Client
import cool.graph.shared.mutactions.InvalidInput
import cool.graph.system.mutactions.client.DeleteClientDatabaseForProject
import cool.graph.system.mutactions.internal.DeleteClient
import cool.graph.{InternalMutation, Mutaction}
import sangria.relay.Mutation
import scaldi.Injector

case class DeleteCustomerMutation(
    client: Client,
    args: DeleteCustomerInput,
    internalDatabase: InternalDatabase
)(implicit inj: Injector)
    extends InternalMutation[DeleteCustomerMutationPayload] {

  override def prepareActions(): List[Mutaction] = {

    actions = if (client.id != args.customerId) {
      List(InvalidInput(InvalidClientId(args.customerId)))
    } else {
      client.projects.map(project => DeleteClientDatabaseForProject(project.id)) ++ List(DeleteClient(client))
    }

    actions
  }

  override def getReturnValue(): Option[DeleteCustomerMutationPayload] = {
    Some(DeleteCustomerMutationPayload(clientMutationId = args.clientMutationId, customer = client))
  }
}

case class DeleteCustomerMutationPayload(clientMutationId: Option[String], customer: models.Client) extends Mutation

case class DeleteCustomerInput(clientMutationId: Option[String], customerId: String)
