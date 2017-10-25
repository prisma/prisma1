package cool.graph.client.mutactions

import com.amazonaws.services.kinesis.model.PutRecordResult
import com.typesafe.scalalogging.LazyLogging
import cool.graph._
import cool.graph.client.database.{DeferredResolverProvider, SimpleManyModelDeferredResolver, SimpleToManyDeferredResolver}
import cool.graph.client.schema.simple.SimpleSchemaModelObjectTypeBuilder
import cool.graph.shared.algolia.schemas.AlgoliaFullModelSchema
import cool.graph.shared.algolia.{AlgoliaEvent, AlgoliaFullModelContext}
import cool.graph.shared.errors.SystemErrors
import cool.graph.shared.externalServices.KinesisPublisher
import cool.graph.shared.logging.{LogData, LogKey}
import cool.graph.shared.models.{AlgoliaSyncQuery, Model, Project, SearchProviderAlgolia}
import cool.graph.shared.schema.JsonMarshalling._
import cool.graph.util.json.SprayJsonExtensions
import sangria.ast._
import sangria.execution.Executor
import sangria.parser.QueryParser
import scaldi.{Injectable, Injector}
import spray.json.{JsString, _}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

case class SyncModelToAlgolia(
    model: Model,
    project: Project,
    syncQuery: AlgoliaSyncQuery,
    searchProviderAlgolia: SearchProviderAlgolia,
    requestId: String
)(implicit inj: Injector)
    extends Mutaction
    with Injectable
    with LazyLogging
    with SprayJsonExtensions {

  import cool.graph.shared.algolia.AlgoliaEventJsonProtocol._
  import cool.graph.utils.`try`.TryExtensions._

  val algoliaSyncPublisher: KinesisPublisher = inject[KinesisPublisher](identified by "kinesisAlgoliaSyncQueriesPublisher")
  implicit val dispatcher: ExecutionContext  = inject[ExecutionContext](identified by "dispatcher")

  override def execute: Future[MutactionExecutionResult] = {
    if (!searchProviderAlgolia.isEnabled) {
      Future.successful(MutactionExecutionSuccess())
    } else {
      syncItemsForQueryToAlgolia(syncQuery.fragment).recover {
        case x => SystemErrors.UnknownExecutionError(x.getMessage, "")
      }
    }
  }

  private def syncItemsForQueryToAlgolia(query: String): Future[MutactionExecutionSuccess] = {
    for {
      enhancedQuery <- parseAndEnhanceSynQuery(query).toFuture
      result        <- performQueryWith(enhancedQuery)
      dataList      = result.pathAsSeq("data.node").toList
      enhancedList = dataList.map { rawRow =>
        cleanAndAddObjectIdForAlgolia(rawRow)
      }
      payload = enhancedList.map { item =>
        val formattedPayload = stringifyAndListifyPayload(item._2)
        algoliaEventFor(formattedPayload, item._1).toJson.compactPrint
      }

    } yield {
      payload.foreach { payload =>
        val publisherResult = algoliaSyncPublisher.putRecord(payload)
        logMutaction(publisherResult)
      }
      MutactionExecutionSuccess()
    }
  }

  private def parseAndEnhanceSynQuery(query: String): Try[Document] = {
    QueryParser.parse(syncQuery.fragment).map { queryAst =>
      val modifiedDefinitions = queryAst.definitions.map {
        case x: OperationDefinition => x.copy(selections = addIdFieldToNodeSelections(x.selections))
        case y: FragmentDefinition  => y.copy(selections = addIdFieldToNodeSelections(y.selections))
        case z                      => z
      }
      val queryWithAddedIdSelection = queryAst.copy(definitions = modifiedDefinitions)
      queryWithAddedIdSelection
    }
  }

  private def addIdFieldToNodeSelections(selections: Vector[Selection]): Vector[Selection] = selections map {
    case f: Field if f.name == "node" =>
      f.copy(selections = f.selections :+ Field(None, "id", Vector.empty, Vector.empty, Vector.empty))
    case x => x
  }

  private def performQueryWith(queryAst: Document): Future[JsValue] = {
    val schema = new AlgoliaFullModelSchema(
      project = project,
      model = model,
      modelObjectTypes = new SimpleSchemaModelObjectTypeBuilder(project = project)
    ).build()

    val userContext = AlgoliaFullModelContext(
      project = project,
      requestId = "",
      log = (x: String) => logger.info(x)
    )

    Executor.execute(
      schema,
      queryAst,
      userContext,
      deferredResolver = new DeferredResolverProvider(new SimpleToManyDeferredResolver, new SimpleManyModelDeferredResolver, skipPermissionCheck = true)
    )
  }

  private def cleanAndAddObjectIdForAlgolia(rawQueryResult: JsValue): (String, JsObject) = {
    val jsObject      = rawQueryResult.asJsObject
    val nodeId        = jsObject.pathAsString("id")
    val objectIdField = "objectID" -> JsString(nodeId)

    (nodeId, JsObject(jsObject.fields + objectIdField))
  }

  private def stringifyAndListifyPayload(value: JsValue): String = s"[${value.compactPrint}]"

  private def algoliaEventFor(payload: String, nodeId: String): AlgoliaEvent = {
    AlgoliaEvent(
      indexName = syncQuery.indexName,
      applicationId = searchProviderAlgolia.applicationId,
      apiKey = searchProviderAlgolia.apiKey,
      operation = "create",
      nodeId = nodeId,
      requestId = requestId,
      queryResult = payload
    )
  }

  private def logMutaction(result: PutRecordResult) = {
    logger.info(
      LogData(
        key = LogKey.AlgoliaSyncQuery,
        requestId = requestId,
        payload = Some(Map("kinesis" -> Map("sequence_number" -> result.getSequenceNumber, "shard_id" -> result.getShardId)))
      ).json
    )
  }
}
