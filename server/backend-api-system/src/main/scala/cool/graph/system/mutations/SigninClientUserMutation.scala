package cool.graph.system.mutations

import java.util.concurrent.TimeUnit

import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.models.{Client, Project}
import cool.graph.system.authorization.SystemAuth2
import cool.graph.{DataItem, InternalProjectMutation, Mutaction}
import sangria.relay.Mutation
import scaldi.{Injectable, Injector}
import spray.json.DefaultJsonProtocol._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

case class SigninClientUserMutation(
    client: Client,
    project: Project,
    args: SigninClientUserInput,
    projectDbsFn: Project => InternalAndProjectDbs
)(implicit inj: Injector)
    extends InternalProjectMutation[SigninClientUserMutationPayload]
    with Injectable {

  override def prepareActions(): List[Mutaction] = {
    actions
  }

  override def getReturnValue: Option[SigninClientUserMutationPayload] = {

    val auth = SystemAuth2()
    val token = Await.result(auth.loginUser(project, DataItem(id = args.clientUserId, userData = Map()), authData = Some("SigninClientUserMutation")),
                             Duration(5, TimeUnit.SECONDS))

    Some(SigninClientUserMutationPayload(clientMutationId = args.clientMutationId, token = token))
  }
}

case class SigninClientUserMutationPayload(clientMutationId: Option[String], token: String) extends Mutation

case class SigninClientUserInput(clientMutationId: Option[String], projectId: String, clientUserId: String)
