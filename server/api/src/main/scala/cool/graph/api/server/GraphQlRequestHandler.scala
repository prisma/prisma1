package cool.graph.client.server

import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model._
import cool.graph.api.ApiDependencies
import cool.graph.api.schema.ApiUserContext
import cool.graph.api.server.{ErrorHandler, GraphQlQuery, GraphQlRequest}
import sangria.execution.{Executor, QueryAnalysisError}
import spray.json.{JsArray, JsValue}

import scala.collection.immutable.Seq
import scala.concurrent.Future

trait GraphQlRequestHandler {
  def handle(graphQlRequest: GraphQlRequest): Future[(StatusCode, JsValue)]

  def healthCheck: Future[Unit]
}

case class GraphQlRequestHandlerImpl(
    log: String => Unit
)(implicit apiDependencies: ApiDependencies)
    extends GraphQlRequestHandler {

  import apiDependencies.system.dispatcher
  import cool.graph.api.server.JsonMarshalling._

  override def handle(graphQlRequest: GraphQlRequest): Future[(StatusCode, JsValue)] = {
    val jsonResult = if (!graphQlRequest.isBatch) {
      handleQuery(request = graphQlRequest, query = graphQlRequest.queries.head)
    } else {
      val results: Seq[Future[JsValue]] = graphQlRequest.queries.map(query => handleQuery(graphQlRequest, query))
      Future.sequence(results).map(results => JsArray(results.toVector))
    }
    jsonResult.map(OK -> _)
  }

  def handleQuery(
      request: GraphQlRequest,
      query: GraphQlQuery
  ): Future[JsValue] = {
    val context      = ApiUserContext(clientId = "clientId")
    val errorHandler = ErrorHandler(request.id)

    val result = Executor.execute(
      schema = request.schema,
      queryAst = query.query,
      userContext = context,
      variables = query.variables,
      exceptionHandler = errorHandler.sangriaExceptionHandler,
      operationName = query.operationName,
      deferredResolver = apiDependencies.deferredResolverProvider(request.project)
    )

    result.recover {
      case error: QueryAnalysisError =>
        error.resolveError

      case error: Throwable =>
        errorHandler.handle(error)._2
    }
  }

  override def healthCheck: Future[Unit] = Future.unit
}
