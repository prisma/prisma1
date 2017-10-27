package cool.graph.shared.algolia

import cool.graph.RequestContextTrait
import cool.graph.client.database.ProjectDataresolver
import cool.graph.cloudwatch.Cloudwatch
import cool.graph.shared.models.Project
import scaldi.{Injectable, Injector}

case class AlgoliaContext(project: Project, requestId: String, nodeId: String, log: Function[String, Unit])(implicit inj: Injector)
    extends RequestContextTrait
    with Injectable {

  override val projectId: Option[String] = Some(project.id)
  override val clientId                  = project.ownerId
  override val requestIp                 = "algolia-ip"

  val cloudwatch = inject[Cloudwatch]("cloudwatch")

  val dataResolver = {
    val resolver = new ProjectDataresolver(project = project, requestContext = this)
    resolver.enableMasterDatabaseOnlyMode
    resolver
  }

}

case class AlgoliaFullModelContext(project: Project, requestId: String, log: Function[String, Unit])(implicit inj: Injector)
    extends RequestContextTrait
    with Injectable {

  override val projectId: Option[String] = Some(project.id)
  override val clientId                  = project.ownerId
  override val requestIp                 = "mutation-callback-ip"

  val cloudwatch = inject[Cloudwatch]("cloudwatch")

  // using the readonly replica here is fine as this doesn't happen in response to data changes
  val dataResolver =
    new ProjectDataresolver(project = project, requestContext = this)
}
