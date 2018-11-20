package com.prisma.api.server

import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.complete
import com.prisma.api.ApiDependencies
import com.prisma.api.schema.{ApiUserContext, UserFacingError}
import com.prisma.cache.Cache
import com.prisma.messagebus.pubsub.{Everything, Message}
import com.prisma.sangria.utils.ErrorHandler
import com.prisma.shared.messages.SchemaInvalidatedMessage
import play.api.libs.json.{JsArray, JsValue}
import sangria.ast.Document
import sangria.execution.{Executor, QueryAnalysisError}
import sangria.schema.Schema
import sangria.validation.{QueryValidator, Violation}

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
  import com.prisma.api.server.JsonMarshalling._

  val queryValidationCache = apiDependencies.cacheFactory.lfu[(String, Document), Vector[Violation]](sangriaMinimumCacheSize, sangriaMaximumCacheSize)

  apiDependencies.invalidationSubscriber.subscribe(Everything, (msg: Message[SchemaInvalidatedMessage]) => {
    queryValidationCache.removeAll(key => key._1 == msg.topic)
  })

  override def handle(graphQlRequest: GraphQlRequest): Future[(StatusCode, JsValue)] = {
    val jsonResult = if (!graphQlRequest.isBatch) {
      handleQuery(request = graphQlRequest, query = graphQlRequest.queries.head)
    } else {
      val results: Seq[Future[JsValue]] = graphQlRequest.queries.map(query => handleQuery(graphQlRequest, query))
      Future.sequence(results).map(results => JsArray(results.toVector))
    }
    jsonResult.map(OK -> _)
  }

  def errorExtractor(t: Throwable): Option[Int] = t match {
    case e: UserFacingError => Some(e.code)
    case _                  => None
  }

  def handleQuery(
      request: GraphQlRequest,
      query: GraphQlQuery
  ): Future[JsValue] = {
    val context = ApiUserContext(clientId = "clientId")
    val errorHandler = ErrorHandler(
      request.id,
      "POST",
      "",
      Seq.empty,
      query.queryString,
      query.variables,
      apiDependencies.reporter,
      projectId = Some(request.project.id),
      errorCodeExtractor = errorExtractor
    )

    val queryValidator = new QueryValidator {
      override def validateQuery(schema: Schema[_, _], queryAst: Document): Vector[Violation] = {
        queryValidationCache.getOrUpdate((request.project.id, queryAst), () => QueryValidator.default.validateQuery(request.schema, query.query))
      }
    }

    val result: Future[JsValue] = Executor.execute(
      schema = request.schema,
      queryAst = query.query,
      userContext = context,
      variables = query.variables,
      exceptionHandler = errorHandler.sangriaExceptionHandler,
      operationName = query.operationName,
      deferredResolver = apiDependencies.deferredResolverProvider(request.project),
      queryValidator = queryValidator
    )

    result.recover {
      case e: QueryAnalysisError => e.resolveError
      case e: UserFacingError    => JsonErrorHelper.errorJson(request.id, e.getMessage, e.code)
    }
  }

  override def healthCheck: Future[Unit] = Future.unit
}
