package cool.graph.subscriptions

import cool.graph.deprecated.actions.schemas.MutationMetaData
import cool.graph.client.database.{DeferredResolverProvider, SimpleManyModelDeferredResolver, SimpleToManyDeferredResolver}
import cool.graph.shared.models.ModelMutationType.ModelMutationType
import cool.graph.shared.models._
import cool.graph.subscriptions.schemas.{QueryTransformer, SubscriptionSchema}
import cool.graph.util.ErrorHandlerFactory
import cool.graph.{DataItem, FieldMetricsMiddleware}
import sangria.ast.Document
import sangria.execution.{Executor, Middleware}
import sangria.parser.QueryParser
import sangria.renderer.QueryRenderer
import scaldi.Injector
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

object SubscriptionExecutor {
  def execute(project: Project,
              model: Model,
              mutationType: ModelMutationType,
              previousValues: Option[DataItem],
              updatedFields: Option[List[String]],
              query: String,
              variables: spray.json.JsValue,
              nodeId: String,
              clientId: String,
              authenticatedRequest: Option[AuthenticatedRequest],
              requestId: String,
              operationName: Option[String],
              skipPermissionCheck: Boolean,
              alwaysQueryMasterDatabase: Boolean)(implicit inj: Injector, ec: ExecutionContext): Future[Option[JsValue]] = {

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
      clientId = clientId,
      authenticatedRequest = authenticatedRequest,
      requestId = requestId,
      operationName = operationName,
      skipPermissionCheck = skipPermissionCheck,
      alwaysQueryMasterDatabase = alwaysQueryMasterDatabase
    )
  }

  def execute(project: Project,
              model: Model,
              mutationType: ModelMutationType,
              previousValues: Option[DataItem],
              updatedFields: Option[List[String]],
              query: Document,
              variables: spray.json.JsValue,
              nodeId: String,
              clientId: String,
              authenticatedRequest: Option[AuthenticatedRequest],
              requestId: String,
              operationName: Option[String],
              skipPermissionCheck: Boolean,
              alwaysQueryMasterDatabase: Boolean)(implicit inj: Injector, ec: ExecutionContext): Future[Option[JsValue]] = {
    import cool.graph.shared.schema.JsonMarshalling._
    import cool.graph.util.json.Json._

    val schema       = SubscriptionSchema(model, project, updatedFields, mutationType, previousValues).build()
    val errorHandler = ErrorHandlerFactory(println)
    val unhandledErrorLogger = errorHandler.unhandledErrorHandler(
      requestId = requestId,
      projectId = Some(project.id)
    )

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
      mutation = MutationMetaData(id = "", _type = ""),
      authenticatedRequest = authenticatedRequest,
      requestId = requestId,
      project = project,
      clientId = clientId,
      log = x => println(x),
      queryAst = Some(actualQuery)
    )
    if (alwaysQueryMasterDatabase) {
      context.dataResolver.enableMasterDatabaseOnlyMode
    }

    val sangriaHandler = errorHandler.sangriaHandler(
      requestId = requestId,
      query = QueryRenderer.render(actualQuery),
      variables = spray.json.JsObject.empty,
      clientId = None,
      projectId = Some(project.id)
    )

    Executor
      .execute(
        schema = schema,
        queryAst = actualQuery,
        variables = variables,
        userContext = context,
        exceptionHandler = sangriaHandler,
        operationName = operationName,
        deferredResolver =
          new DeferredResolverProvider(new SimpleToManyDeferredResolver, new SimpleManyModelDeferredResolver, skipPermissionCheck = skipPermissionCheck),
        middleware = List[Middleware[SubscriptionUserContext]](new FieldMetricsMiddleware)
      )
      .map { result =>
        if (result.pathAs[JsValue](s"data.${model.name}") != JsNull) {
          Some(result)
        } else {
          None
        }
      }
  }
}
