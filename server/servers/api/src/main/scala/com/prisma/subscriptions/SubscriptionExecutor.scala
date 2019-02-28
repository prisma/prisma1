package com.prisma.subscriptions

import akka.http.scaladsl.model.HttpRequest
import com.prisma.api.ApiDependencies
import com.prisma.api.connector.PrismaNode
import com.prisma.api.resolver.DeferredResolverImpl
import com.prisma.api.schema.UserFacingError
import com.prisma.gc_values.IdGCValue
import com.prisma.sangria.utils.ErrorHandler
import com.prisma.shared.models.ModelMutationType.ModelMutationType
import com.prisma.shared.models._
import com.prisma.subscriptions.schema.{QueryTransformer, SubscriptionSchema, VariablesTransformer}
import play.api.libs.json._
import sangria.ast.Document
import sangria.execution.Executor
import sangria.parser.QueryParser
import sangria.renderer.QueryRenderer

import scala.concurrent.{ExecutionContext, Future}

object SubscriptionExecutor {
  def execute(
      project: Project,
      model: Model,
      mutationType: ModelMutationType,
      previousValues: Option[PrismaNode],
      updatedFields: Option[List[String]],
      query: String,
      variables: JsValue,
      nodeId: IdGCValue,
      requestId: String,
      operationName: Option[String],
      skipPermissionCheck: Boolean,
      alwaysQueryMasterDatabase: Boolean
  )(implicit dependencies: ApiDependencies, ec: ExecutionContext): Future[Option[JsValue]] = {

    val queryAst = QueryParser.parse(query).get

    execute(
      project = project,
      model = model,
      mutationType = mutationType,
      previousValues = previousValues,
      updatedFields = updatedFields,
      query = queryAst,
      variables = variables,
      nodeId = nodeId,
      requestId = requestId,
      operationName = operationName,
      skipPermissionCheck = skipPermissionCheck,
      alwaysQueryMasterDatabase = alwaysQueryMasterDatabase
    )
  }

  def execute(
      project: Project,
      model: Model,
      mutationType: ModelMutationType,
      previousValues: Option[PrismaNode],
      updatedFields: Option[List[String]],
      query: Document,
      variables: JsValue,
      nodeId: IdGCValue,
      requestId: String,
      operationName: Option[String],
      skipPermissionCheck: Boolean,
      alwaysQueryMasterDatabase: Boolean
  )(implicit dependencies: ApiDependencies, ec: ExecutionContext): Future[Option[JsValue]] = {
    import com.prisma.api.server.JsonMarshalling._

    val updatedFieldsSet                       = updatedFields.getOrElse(List.empty).toSet
    val internalSchema                         = SubscriptionSchema(model, project, updatedFields, mutationType, previousValues).build()
    val (filtersMatch, transformedQuery)       = QueryTransformer.evaluateInMemoryFilters(query, mutationType, updatedFieldsSet)
    val (variablesMatch, transformedVariables) = VariablesTransformer.evaluateInMemoryFilters(variables, mutationType, updatedFieldsSet)

    val context = SubscriptionUserContext(
      nodeId = nodeId,
      requestId = requestId,
      project = project,
      log = x => println(x),
      queryAst = Some(transformedQuery)
    )

    val dataResolver = if (alwaysQueryMasterDatabase) dependencies.masterDataResolver(project) else dependencies.dataResolver(project)

    def errorExtractor(t: Throwable): Option[Int] = t match {
      case e: UserFacingError => Some(e.code)
      case _                  => None
    }

    val sangriaHandler = ErrorHandler(
      requestId,
      method = "",
      uri = "",
      headers = Vector.empty,
      query.renderPretty,
      variables,
      dependencies.reporter,
      Some(project.id),
      errorCodeExtractor = errorExtractor
    ).sangriaExceptionHandler

    if (filtersMatch && variablesMatch) {
      Executor
        .execute(
          schema = internalSchema,
          queryAst = transformedQuery,
          userContext = context,
          variables = transformedVariables,
          exceptionHandler = sangriaHandler,
          operationName = operationName,
          deferredResolver = new DeferredResolverImpl(dataResolver)
        )
        .map { result =>
          val lookup = result.as[JsObject] \ "data" \ camelCase(model.name)
          if (lookup.validate[JsValue].get != JsNull) Some(result) else None
        }
    } else {
      Future.successful(None)
    }
  }

  def camelCase(string: String): String = Character.toLowerCase(string.charAt(0)) + string.substring(1)
}
