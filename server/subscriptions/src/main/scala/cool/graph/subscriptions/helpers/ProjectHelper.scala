package cool.graph.subscriptions.helpers

import akka.actor.ActorSystem
import cool.graph.shared.models.ProjectWithClientId
import cool.graph.subscriptions.SubscriptionDependencies

import scala.concurrent.{ExecutionContext, Future}

object ProjectHelper {
  def resolveProject(projectId: String)(implicit dependencies: SubscriptionDependencies, as: ActorSystem, ec: ExecutionContext): Future[ProjectWithClientId] = {
    dependencies.projectFetcher.fetch(projectId).map {
      case None =>
        sys.error(s"ProjectHelper: Could not resolve project with id: $projectId")

      case Some(project: ProjectWithClientId) =>
        project
    }
  }
}
