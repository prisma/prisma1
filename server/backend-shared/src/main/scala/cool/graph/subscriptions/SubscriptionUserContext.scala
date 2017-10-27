package cool.graph.subscriptions

import cool.graph.RequestContextTrait
import cool.graph.client.UserContextTrait
import cool.graph.deprecated.actions.schemas.MutationMetaData
import cool.graph.client.database.ProjectDataresolver
import cool.graph.cloudwatch.Cloudwatch
import cool.graph.shared.models.{AuthenticatedRequest, Project}
import sangria.ast.Document
import scaldi.{Injectable, Injector}

case class SubscriptionUserContext(nodeId: String,
                                   mutation: MutationMetaData,
                                   project: Project,
                                   authenticatedRequest: Option[AuthenticatedRequest],
                                   requestId: String,
                                   clientId: String,
                                   log: Function[String, Unit],
                                   override val queryAst: Option[Document] = None)(implicit inj: Injector)
    extends UserContextTrait
    with RequestContextTrait
    with Injectable {

  override val isSubscription: Boolean   = true
  override val projectId: Option[String] = Some(project.id)

  val cloudwatch = inject[Cloudwatch]("cloudwatch")

  val dataResolver =
    new ProjectDataresolver(project = project, requestContext = this)
  override val requestIp: String = "subscription-callback-ip" // todo: get the correct ip from server
}
