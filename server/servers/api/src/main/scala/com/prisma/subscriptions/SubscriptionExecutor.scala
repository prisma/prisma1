package com.prisma.subscriptions

import akka.http.scaladsl.model.HttpRequest
import com.prisma.api.ApiDependencies
import com.prisma.api.connector.PrismaNode
import com.prisma.api.resolver.DeferredResolverProvider
import com.prisma.api.schema.UserFacingError
import com.prisma.sangria.utils.ErrorHandler
import com.prisma.shared.models.ModelMutationType.ModelMutationType
import com.prisma.shared.models._
import com.prisma.subscriptions.schema.{QueryTransformer, SubscriptionSchema}
import play.api.libs.json._
import sangria.ast.Document
import sangria.execution.Executor
import sangria.parser.QueryParser

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
      nodeId: String,
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
      nodeId: String,
      requestId: String,
      operationName: Option[String],
      skipPermissionCheck: Boolean,
      alwaysQueryMasterDatabase: Boolean
  )(implicit dependencies: ApiDependencies, ec: ExecutionContext): Future[Option[JsValue]] = {
    import com.prisma.api.server.JsonMarshalling._

    val schema = SubscriptionSchema(model, project, updatedFields, mutationType, previousValues).build()

    val actualQuery = {
      val mutationInEvaluated = if (mutationType == ModelMutationType.Updated) {
        val tmp = QueryTransformer.replaceMutationInFilter(query, mutationType).asInstanceOf[Document]
        QueryTransformer.replaceUpdatedFieldsInFilter(tmp, updatedFields.get.toSet).asInstanceOf[Document]
      } else {
        QueryTransformer.replaceMutationInFilter(query, mutationType).asInstanceOf[Document]
      }
      QueryTransformer.mergeBooleans(mutationInEvaluated).asInstanceOf[Document]
    }

    val context = SubscriptionUserContext(
      nodeId = nodeId,
      requestId = requestId,
      project = project,
      log = x => println(x),
      queryAst = Some(actualQuery)
    )
    val dataResolver = if (alwaysQueryMasterDatabase) {
      dependencies.masterDataResolver(project)
    } else {
      dependencies.dataResolver(project)
    }

    def errorExtractor(t: Throwable): Option[Int] = t match {
      case e: UserFacingError => Some(e.code)
      case _                  => None
    }

    val sangriaHandler = ErrorHandler(
      requestId,
      HttpRequest(),
      query.renderPretty,
      variables.toString,
      dependencies.reporter,
      Some(project.id),
      errorCodeExtractor = errorExtractor
    ).sangriaExceptionHandler

    Executor
      .execute(
        schema = schema,
        queryAst = actualQuery,
        userContext = context,
        variables = variables,
        exceptionHandler = sangriaHandler,
        operationName = operationName,
        deferredResolver = new DeferredResolverProvider(dataResolver)
      )
      .map { result =>
        val lookup = result.as[JsObject] \ "data" \ camelCase(model.name)
        if (lookup.validate[JsValue].get != JsNull) {
          Some(result)
        } else {
          None
        }
      }
  }

  def camelCase(string: String): String = Character.toLowerCase(string.charAt(0)) + string.substring(1)
}
