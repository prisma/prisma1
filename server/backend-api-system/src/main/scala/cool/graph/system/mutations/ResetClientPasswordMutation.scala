package cool.graph.system.mutations

import cool.graph.shared.database.InternalDatabase
import cool.graph.shared.models.Client
import cool.graph.system.mutactions.internal.ResetClientPassword
import cool.graph.{InternalMutation, Mutaction}
import sangria.relay.Mutation
import scaldi.Injector

case class ResetClientPasswordMutation(
    client: Client,
    userToken: String,
    args: ResetClientPasswordInput,
    internalDatabase: InternalDatabase
)(implicit inj: Injector)
    extends InternalMutation[ResetClientPasswordMutationPayload] {

  override def prepareActions(): List[Mutaction] = {
    actions :+= ResetClientPassword(client = client, resetPasswordToken = args.resetPasswordToken, newPassword = args.newPassword)

    actions
  }

  override def getReturnValue(): Option[ResetClientPasswordMutationPayload] = {
    Some(new ResetClientPasswordMutationPayload(clientMutationId = args.clientMutationId, client = client, userToken = userToken))
  }
}

case class ResetClientPasswordMutationPayload(clientMutationId: Option[String], client: Client, userToken: String) extends Mutation

case class ResetClientPasswordInput(clientMutationId: Option[String], newPassword: String, resetPasswordToken: String)
