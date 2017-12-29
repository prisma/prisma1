package cool.graph.subscriptions.resolving

import cool.graph.api.database.DataItem
import cool.graph.api.database.deferreds.DeferredResolverProvider
import cool.graph.api.schema.ApiUserContext
import cool.graph.api.server.{ErrorHandler, GraphQlRequest}
import cool.graph.shared.models.ModelMutationType.ModelMutationType
import cool.graph.shared.models._
import cool.graph.subscriptions.SubscriptionDependencies
import cool.graph.subscriptions.schemas.{QueryTransformer, SubscriptionSchema}
import cool.graph.util.json.SprayJsonExtensions
import sangria.ast.Document
import sangria.execution.Executor
import sangria.parser.QueryParser
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

object SubscriptionExecutor extends SprayJsonExtensions {
  def execute(
      project: Project,
      model: Model,
      mutationType: ModelMutationType,
      previousValues: Option[DataItem],
      updatedFields: Option[List[String]],
      query: String,
      variables: spray.json.JsValue,
      nodeId: String,
      clientId: String,
      requestId: String,
      operationName: Option[String],
      skipPermissionCheck: Boolean,
      alwaysQueryMasterDatabase: Boolean
  )(implicit dependencies: SubscriptionDependencies, ec: ExecutionContext): Future[Option[JsValue]] = {

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
      previousValues: Option[DataItem],
      updatedFields: Option[List[String]],
      query: Document,
      variables: spray.json.JsValue,
      nodeId: String,
      clientId: String,
      requestId: String,
      operationName: Option[String],
      skipPermissionCheck: Boolean,
      alwaysQueryMasterDatabase: Boolean
  )(implicit dependencies: SubscriptionDependencies, ec: ExecutionContext): Future[Option[JsValue]] = {
    import cool.graph.api.server.JsonMarshalling._

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

//    val context = SubscriptionUserContext(
//      nodeId = nodeId,
//      requestId = requestId,
//      project = project,
//      clientId = clientId,
//      log = x => println(x),
//      queryAst = Some(actualQuery)
//    )
    val dataResolver = if (alwaysQueryMasterDatabase) {
      dependencies.dataResolver(project).copy(useMasterDatabaseOnly = true)
    } else {
      dependencies.dataResolver(project)
    }

    val sangriaHandler = ErrorHandler(requestId).sangriaExceptionHandler

    Executor
      .execute(
        schema = schema,
        queryAst = actualQuery,
        userContext = ApiUserContext("bla"),
        variables = variables,
        exceptionHandler = sangriaHandler,
        operationName = operationName,
        deferredResolver = new DeferredResolverProvider(dataResolver)
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
