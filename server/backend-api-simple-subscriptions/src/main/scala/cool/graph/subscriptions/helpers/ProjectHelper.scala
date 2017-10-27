package cool.graph.subscriptions.helpers

import akka.actor.{ActorRef, ActorSystem}
import cool.graph.client.finder.ProjectFetcher
import cool.graph.client.{ApiFeatureMetric, FeatureMetric}
import cool.graph.shared.models.ProjectWithClientId
import cool.graph.shared.externalServices.TestableTime
import scaldi.Injector
import scaldi.akka.AkkaInjectable

import scala.concurrent.{ExecutionContext, Future}

object ProjectHelper extends AkkaInjectable {
  def resolveProject(projectId: String)(implicit inj: Injector, as: ActorSystem, ec: ExecutionContext): Future[ProjectWithClientId] = {
    val schemaFetcher = inject[ProjectFetcher](identified by "project-schema-fetcher")

    schemaFetcher.fetch(projectId).map {
      case None =>
        sys.error(s"ProjectHelper: Could not resolve project with id: $projectId")

      case Some(project: ProjectWithClientId) => {
        val apiMetricActor = inject[ActorRef](identified by "featureMetricActor")
        val testableTime   = inject[TestableTime]

        apiMetricActor ! ApiFeatureMetric(
          "",
          testableTime.DateTime,
          project.project.id,
          project.clientId,
          List(FeatureMetric.Subscriptions.toString),
          isFromConsole = false
        )

        project
      }
    }
  }
}
