package cool.graph.deprecated.actions.schemas

import akka.actor.ActorRef
import cool.graph.RequestContextTrait
import cool.graph.client.database.ProjectDataresolver
import cool.graph.cloudwatch.Cloudwatch
import cool.graph.shared.models.Project
import scaldi.{Injectable, Injector}

case class ActionUserContext(project: Project, requestId: String, nodeId: String, mutation: MutationMetaData, log: Function[String, Unit])(
    implicit inj: Injector)
    extends RequestContextTrait
    with Injectable {

  override val projectId: Option[String] = Some(project.id)
  override val clientId                  = project.ownerId
  override val requestIp                 = "mutation-callback-ip"

  val cloudwatch =
    inject[Cloudwatch]("cloudwatch")

  val dataResolver = {
    val resolver = new ProjectDataresolver(project = project, requestContext = this)
    resolver.enableMasterDatabaseOnlyMode
    resolver
  }

}
