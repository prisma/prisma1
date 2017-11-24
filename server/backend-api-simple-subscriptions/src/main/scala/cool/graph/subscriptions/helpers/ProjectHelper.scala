package cool.graph.subscriptions.helpers

import akka.actor.ActorSystem
import cool.graph.client.{ApiFeatureMetric, ClientInjector, FeatureMetric}
import cool.graph.shared.models.ProjectWithClientId

import scala.concurrent.{ExecutionContext, Future}

object ProjectHelper {
  def resolveProject(projectId: String)(implicit injector: ClientInjector, as: ActorSystem, ec: ExecutionContext): Future[ProjectWithClientId] = {
    val schemaFetcher = injector.projectSchemaFetcher

    schemaFetcher.fetch(projectId).map {
      case None =>
        sys.error(s"ProjectHelper: Could not resolve project with id: $projectId")

      case Some(project: ProjectWithClientId) =>
        val apiMetricActor = injector.featureMetricActor
        val testableTime   = injector.testableTime

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
