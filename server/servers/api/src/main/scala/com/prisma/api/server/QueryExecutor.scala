package com.prisma.api.server

import com.prisma.api.ApiDependencies
import com.prisma.api.schema.{ApiUserContext, UserFacingError}
import com.prisma.messagebus.pubsub.{Everything, Message}
import com.prisma.sangria.utils.ErrorHandler
import com.prisma.shared.messages.SchemaInvalidatedMessage
import com.prisma.shared.models.Project
import play.api.libs.json.JsValue
import sangria.ast.Document
import sangria.execution.{Executor, QueryAnalysisError}
import sangria.schema.Schema
import sangria.validation.{QueryValidator, Violation}

import scala.collection.immutable.Seq
import scala.concurrent.Future

case class QueryExecutor()(implicit apiDependencies: ApiDependencies) {
  import apiDependencies.system.dispatcher
  import com.prisma.api.server.JsonMarshalling._

  val queryValidationCache = apiDependencies.cacheFactory.lfu[(String, Document), Vector[Violation]](sangriaMinimumCacheSize, sangriaMaximumCacheSize)

  apiDependencies.invalidationSubscriber.subscribe(Everything, (msg: Message[SchemaInvalidatedMessage]) => {
    queryValidationCache.removeAll(key => key._1 == msg.topic)
  })

  def execute(
      requestId: String,
      queryString: String,
      queryAst: Document,
      variables: JsValue,
      operationName: Option[String],
      project: Project,
      schema: Schema[ApiUserContext, Unit]
  ): Future[JsValue] = {
    val context = ApiUserContext(clientId = "clientId")
    val errorHandler = ErrorHandler(
      requestId,
      "POST",
      "",
      Seq.empty,
      queryString,
      variables,
      apiDependencies.reporter,
      projectId = Some(project.id),
      errorCodeExtractor = errorExtractor
    )

    val queryValidator = new QueryValidator {
      override def validateQuery(schema: Schema[_, _], queryAst: Document): Vector[Violation] = {
        queryValidationCache.getOrUpdate((project.id, queryAst), () => QueryValidator.default.validateQuery(schema, queryAst))
      }
    }

    val result: Future[JsValue] = Executor.execute(
      schema = schema,
      queryAst = queryAst,
      userContext = context,
      variables = variables,
      exceptionHandler = errorHandler.sangriaExceptionHandler,
      operationName = operationName,
      deferredResolver = apiDependencies.deferredResolverProvider(project),
      queryValidator = queryValidator
    )

    result.recover {
      case e: QueryAnalysisError => e.resolveError
    }
  }

  def errorExtractor(t: Throwable): Option[Int] = t match {
    case e: UserFacingError => Some(e.code)
    case _                  => None
  }
}
