package cool.graph.client.server

import cool.graph.client.UserContext
import cool.graph.shared.models.Project
import sangria.execution.Executor
import sangria.introspection.introspectionQuery
import sangria.schema.Schema
import scaldi.Injector
import spray.json.JsValue

import scala.concurrent.{ExecutionContext, Future}

case class IntrospectionQueryHandler(
    project: Project,
    schema: Schema[UserContext, Unit],
    onFailureCallback: PartialFunction[Throwable, Any],
    log: String => Unit
)(implicit inj: Injector, ec: ExecutionContext) {

  def handle(requestId: String, requestIp: String, clientId: String): Future[JsValue] = {
    import cool.graph.shared.schema.JsonMarshalling._
    val context = UserContext.load(
      project = project,
      requestId = requestId,
      requestIp = requestIp,
      clientId = clientId,
      log = log
    )

    val result = Executor.execute(
      schema = schema,
      queryAst = introspectionQuery,
      userContext = context
    )
    result.onFailure(onFailureCallback)
    result
  }
}
