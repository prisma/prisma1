package cool.graph.client.mutactions

import com.amazonaws.services.kinesis.model.PutRecordResult
import com.typesafe.scalalogging.LazyLogging
import cool.graph.Types.Id
import cool.graph._
import cool.graph.client.ClientInjector
import cool.graph.client.database.{DeferredResolverProvider, SimpleManyModelDeferredResolver, SimpleToManyDeferredResolver}
import cool.graph.client.schema.simple.SimpleSchemaModelObjectTypeBuilder
import cool.graph.shared.algolia.AlgoliaEventJsonProtocol._
import cool.graph.shared.algolia.schemas.AlgoliaSchema
import cool.graph.shared.algolia.{AlgoliaContext, AlgoliaEvent}
import cool.graph.shared.errors.SystemErrors
import cool.graph.shared.logging.{LogData, LogKey}
import cool.graph.shared.models.{AlgoliaSyncQuery, Model, Project, SearchProviderAlgolia}
import cool.graph.shared.schema.JsonMarshalling._
import sangria.ast.Document
import sangria.execution.Executor
import sangria.parser.QueryParser
import spray.json.{JsString, _}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

case class SyncDataItemToAlgolia(
    model: Model,
    project: Project,
    nodeId: Id,
    syncQuery: AlgoliaSyncQuery,
    searchProviderAlgolia: SearchProviderAlgolia,
    requestId: String,
    operation: String
)(implicit injector: ClientInjector)
    extends Mutaction
    with LazyLogging {

  override def execute: Future[MutactionExecutionResult] = {
    searchProviderAlgolia.isEnabled match {
      case false =>
        Future.successful(MutactionExecutionSuccess())
      case true =>
        val algoliaSyncPublisher = injector.kinesisAlgoliaSyncQueriesPublisher
        implicit val dispatcher  = injector.dispatcher

        val parsedGraphQLQuery = QueryParser.parse(syncQuery.fragment)
        val queryResultFuture: Future[Option[JsValue]] =
          parsedGraphQLQuery match {
            case Success(validQueryAst) =>
              operation match {
                case "delete" => Future.successful(Some("".toJson))
                case _        => performQueryWith(validQueryAst).map(_.map((dataMap: JsValue) => cleanAndAddObjectIdForAlgolia(dataMap)))
              }

            case Failure(error) =>
              Future.successful(Some(JsObject("error" -> JsString(error.getMessage))))
          }

        val payloadFuture = queryResultFuture
          .map {
            case Some(queryResult) =>
              val formattedPayload = stringifyAndListifyPayload(queryResult)
              val event            = algoliaEventFor(formattedPayload).toJson.compactPrint
              val publisherResult  = algoliaSyncPublisher.putRecord(event)
              logMutaction(publisherResult)

            case None => ()
          }

        payloadFuture.map(_ => MutactionExecutionSuccess()).recover {
          case x => SystemErrors.UnknownExecutionError(x.getMessage, "")
        }
    }
  }

  private def cleanAndAddObjectIdForAlgolia(rawQueryResult: JsValue): JsObject = {
    //grabbing "node" here couples us to the AlgoliaSchema, be aware
    val resultWithoutNode = rawQueryResult.asJsObject.fields.get("node").toJson.asJsObject
    val algoliaId         = JsObject("objectID" -> JsString(nodeId))
    val combinedFields    = resultWithoutNode.fields ++ algoliaId.fields

    JsObject(combinedFields)
  }

  private def stringifyAndListifyPayload(value: JsValue): String = s"[${value.compactPrint}]"

  private def performQueryWith(queryAst: Document)(implicit ec: ExecutionContext): Future[Option[JsValue]] = {
    implicit val inj = injector.toScaldi

    Executor
      .execute(
        schema = new AlgoliaSchema(
          project = project,
          model = model,
          modelObjectTypes = new SimpleSchemaModelObjectTypeBuilder(project = project)(injector.toScaldi)
        ).build(),
        queryAst = queryAst,
        userContext = AlgoliaContext(
          project = project,
          requestId = "",
          nodeId = nodeId,
          log = (x: String) => logger.info(x)
        ),
        deferredResolver = new DeferredResolverProvider(
          new SimpleToManyDeferredResolver,
          new SimpleManyModelDeferredResolver,
          skipPermissionCheck = true
        )
      )
      .map { response =>
        val JsObject(fields) = response
        val payload: JsValue = fields("data")

        val mutationResultValue =
          payload.asJsObject.fields.head._2

        mutationResultValue match {
          case JsNull => None
          case _      => Some(payload)
        }
      }
  }
  private def algoliaEventFor(payload: String): AlgoliaEvent = {
    AlgoliaEvent(
      indexName = syncQuery.indexName,
      applicationId = searchProviderAlgolia.applicationId,
      apiKey = searchProviderAlgolia.apiKey,
      operation = operation,
      nodeId = nodeId,
      requestId = requestId,
      queryResult = payload
    )
  }

  private def logMutaction(result: PutRecordResult) = {
    logger.info(
      LogData(LogKey.AlgoliaSyncQuery,
              requestId,
              payload = Some(Map("kinesis" -> Map("sequence_number" -> result.getSequenceNumber, "shard_id" -> result.getShardId)))).json
    )
  }
}
