package cool.graph.client

import cool.graph.client.database.ProjectDataresolver
import cool.graph.cloudwatch.Cloudwatch
import cool.graph.shared.models.{AuthenticatedRequest, AuthenticatedUser, Project, ProjectWithClientId}
import cool.graph.RequestContextTrait
import sangria.ast.Document
import scaldi.{Injectable, Injector}

case class UserContext(project: Project,
                       authenticatedRequest: Option[AuthenticatedRequest],
                       requestId: String,
                       requestIp: String,
                       clientId: String,
                       log: Function[String, Unit],
                       override val queryAst: Option[Document] = None,
                       alwaysQueryMasterDatabase: Boolean = false)(implicit inj: Injector)
    extends RequestContextTrait
    with UserContextTrait
    with Injectable {
  override val projectId: Option[String] = Some(project.id)

  val userId = authenticatedRequest.map(_.id)

  val cloudwatch = inject[Cloudwatch]("cloudwatch")

  val queryDataResolver =
    new ProjectDataresolver(project = project, requestContext = this)

  val mutationDataresolver = {
    val resolver = new ProjectDataresolver(project = project, requestContext = this)
    resolver.enableMasterDatabaseOnlyMode
    resolver
  }

  def dataResolver =
    if (alwaysQueryMasterDatabase) {
      mutationDataresolver
    } else {
      queryDataResolver
    }
}

object UserContext {

  def load(
      project: Project,
      requestId: String,
      requestIp: String,
      clientId: String,
      log: Function[String, Unit],
      queryAst: Option[Document] = None
  )(implicit inj: Injector): UserContext = {

    UserContext(project, None, requestId, requestIp, clientId, log, queryAst = queryAst)
  }

  def fetchUserProjectWithClientId(
      project: ProjectWithClientId,
      authenticatedRequest: Option[AuthenticatedRequest],
      requestId: String,
      requestIp: String,
      log: Function[String, Unit],
      queryAst: Option[Document]
  )(implicit inj: Injector): UserContext = {
    fetchUser(project.project, authenticatedRequest, requestId, requestIp, project.clientId, log, queryAst)
  }

  def fetchUser(
      project: Project,
      authenticatedRequest: Option[AuthenticatedRequest],
      requestId: String,
      requestIp: String,
      clientId: String,
      log: Function[String, Unit],
      queryAst: Option[Document] = None
  )(implicit inj: Injector): UserContext = {
    val userContext = UserContext(project, authenticatedRequest, requestId, requestIp, clientId, log, queryAst = queryAst)

    if (authenticatedRequest.isDefined && authenticatedRequest.get.isInstanceOf[AuthenticatedUser]) {
      userContext.addFeatureMetric(FeatureMetric.Authentication)
    }

    userContext
  }
}

trait UserContextTrait {
  val project: Project
  val authenticatedRequest: Option[AuthenticatedRequest]
  val requestId: String
  val clientId: String
  val log: Function[String, Unit]
  val queryAst: Option[Document] = None
}
