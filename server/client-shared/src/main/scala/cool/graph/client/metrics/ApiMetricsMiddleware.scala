package cool.graph.client.metrics

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.LazyLogging
import cool.graph.RequestContextTrait
import cool.graph.client.ApiFeatureMetric
import cool.graph.shared.externalServices.TestableTime
import sangria.execution._

class ApiMetricsMiddleware(
    testableTime: TestableTime,
    apiMetricActor: ActorRef
)(
    implicit system: ActorSystem,
    materializer: ActorMaterializer
) extends Middleware[RequestContextTrait]
    with LazyLogging {

  def afterQuery(queryVal: QueryVal, context: MiddlewareQueryContext[RequestContextTrait, _, _]) = {
    (context.ctx.requestIp, context.ctx.projectId, context.ctx.clientId) match {
      case (requestIp, Some(projectId), clientId) => {
        // todo: generate list of features

        apiMetricActor ! ApiFeatureMetric(requestIp, testableTime.DateTime, projectId, clientId, context.ctx.listFeatureMetrics, context.ctx.isFromConsole)
      }
      case _ => println("missing data for FieldMetrics")
    }
  }

  override type QueryVal = Unit

  override def beforeQuery(context: MiddlewareQueryContext[RequestContextTrait, _, _]): Unit = Unit
}
