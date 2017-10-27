package cool.graph.client.mutactions

import cool.graph.Types.Id
import cool.graph._
import cool.graph.client.requestPipeline.FunctionExecutor
import cool.graph.messagebus.QueuePublisher
import cool.graph.shared.functions.EndpointResolver
import cool.graph.shared.models.ModelMutationType.ModelMutationType
import cool.graph.shared.models._
import cool.graph.subscriptions.SubscriptionExecutor
import cool.graph.webhook.Webhook
import scaldi.{Injectable, Injector}
import spray.json.{JsValue, _}
import cool.graph.utils.future.FutureUtils._

import scala.concurrent.Future
import scala.util.{Failure, Success}

object ServerSideSubscription {
  def extractFromMutactions(project: Project, mutactions: Seq[ClientSqlMutaction], requestId: Id)(implicit inj: Injector): Seq[ServerSideSubscription] = {
    val createMutactions = mutactions.collect { case x: CreateDataItem => x }
    val updateMutactions = mutactions.collect { case x: UpdateDataItem => x }
    val deleteMutactions = mutactions.collect { case x: DeleteDataItem => x }

    extractFromCreateMutactions(project, createMutactions, requestId) ++
      extractFromUpdateMutactions(project, updateMutactions, requestId) ++
      extractFromDeleteMutactions(project, deleteMutactions, requestId)
  }

  def extractFromCreateMutactions(project: Project, mutactions: Seq[CreateDataItem], requestId: Id)(implicit inj: Injector): Seq[ServerSideSubscription] = {
    for {
      mutaction <- mutactions
      sssFn     <- project.serverSideSubscriptionFunctionsFor(mutaction.model, ModelMutationType.Created)
    } yield {
      ServerSideSubscription(
        project,
        mutaction.model,
        ModelMutationType.Created,
        sssFn,
        nodeId = mutaction.id,
        requestId = requestId
      )
    }
  }

  def extractFromUpdateMutactions(project: Project, mutactions: Seq[UpdateDataItem], requestId: Id)(implicit inj: Injector): Seq[ServerSideSubscription] = {
    for {
      mutaction <- mutactions
      sssFn     <- project.serverSideSubscriptionFunctionsFor(mutaction.model, ModelMutationType.Updated)
    } yield {
      ServerSideSubscription(
        project,
        mutaction.model,
        ModelMutationType.Updated,
        sssFn,
        nodeId = mutaction.id,
        requestId = requestId,
        updatedFields = Some(mutaction.namesOfUpdatedFields),
        previousValues = Some(mutaction.previousValues)
      )
    }

  }

  def extractFromDeleteMutactions(project: Project, mutactions: Seq[DeleteDataItem], requestId: Id)(implicit inj: Injector): Seq[ServerSideSubscription] = {
    for {
      mutaction <- mutactions
      sssFn     <- project.serverSideSubscriptionFunctionsFor(mutaction.model, ModelMutationType.Deleted)
    } yield {
      ServerSideSubscription(
        project,
        mutaction.model,
        ModelMutationType.Deleted,
        sssFn,
        nodeId = mutaction.id,
        requestId = requestId,
        previousValues = Some(mutaction.previousValues)
      )
    }
  }
}

case class ServerSideSubscription(
    project: Project,
    model: Model,
    mutationType: ModelMutationType,
    function: ServerSideSubscriptionFunction,
    nodeId: Id,
    requestId: String,
    updatedFields: Option[List[String]] = None,
    previousValues: Option[DataItem] = None
)(implicit inj: Injector)
    extends Mutaction
    with Injectable {
  import scala.concurrent.ExecutionContext.Implicits.global

  val webhookPublisher = inject[QueuePublisher[Webhook]](identified by "webhookPublisher")

  override def execute: Future[MutactionExecutionResult] = {
    for {
      result <- executeQuery()
    } yield {
      result match {
        case Some(JsObject(fields)) if fields.contains("data") =>
          val endpointResolver          = inject[EndpointResolver](identified by "endpointResolver")
          val context: Map[String, Any] = FunctionExecutor.createEventContext(project, "", headers = Map.empty, None, endpointResolver)
          val event                     = JsObject(fields + ("context" -> AnyJsonFormat.write(context)))
          val json                      = event.compactPrint

          function.delivery match {
            case fn: HttpFunction =>
              val webhook = Webhook(project.id, function.id, requestId, fn.url, json, requestId, fn.headers.toMap)
              webhookPublisher.publish(webhook)

            case fn: ManagedFunction =>
              new FunctionExecutor().syncWithLoggingAndErrorHandling_!(function, json, project, requestId)

            case _ =>
          }

        case _ =>
      }

      MutactionExecutionSuccess()
    }
  }

  def executeQuery(): Future[Option[JsValue]] = {
    SubscriptionExecutor.execute(
      project = project,
      model = model,
      mutationType = mutationType,
      previousValues = previousValues,
      updatedFields = updatedFields,
      query = function.query,
      variables = JsObject.empty,
      nodeId = nodeId,
      clientId = project.ownerId,
      authenticatedRequest = None,
      requestId = s"subscription:server_side:${project.id}",
      operationName = None,
      skipPermissionCheck = true,
      alwaysQueryMasterDatabase = true
    )
  }

  implicit object AnyJsonFormat extends JsonFormat[Any] {
    def write(x: Any): JsValue = x match {
      case m: Map[_, _] =>
        JsObject(m.asInstanceOf[Map[String, Any]].mapValues(write))
      case l: List[Any]   => JsArray(l.map(write).toVector)
      case l: Vector[Any] => JsArray(l.map(write))
      case l: Seq[Any]    => JsArray(l.map(write).toVector)
      case n: Int         => JsNumber(n)
      case n: Long        => JsNumber(n)
      case n: BigDecimal  => JsNumber(n)
      case n: Double      => JsNumber(n)
      case s: String      => JsString(s)
      case true           => JsTrue
      case false          => JsFalse
      case v: JsValue     => v
      case null           => JsNull
      case r              => JsString(r.toString)
    }

    def read(x: JsValue): Any = {
      x match {
        case l: JsArray   => l.elements.map(read).toList
        case m: JsObject  => m.fields.mapValues(read)
        case s: JsString  => s.value
        case n: JsNumber  => n.value
        case b: JsBoolean => b.value
        case JsNull       => null
        case _            => sys.error("implement all scalar types!")
      }
    }
  }
}
