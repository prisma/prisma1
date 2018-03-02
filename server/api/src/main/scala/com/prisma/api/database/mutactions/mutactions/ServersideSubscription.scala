package com.prisma.api.database.mutactions.mutactions

import com.prisma.api.ApiDependencies
import com.prisma.api.database.DataItem
import com.prisma.api.database.mutactions.{ClientSqlMutaction, Mutaction, MutactionExecutionResult, MutactionExecutionSuccess}
import com.prisma.subscriptions.schema.QueryTransformer
import com.prisma.subscriptions.{SubscriptionExecutor, Webhook}
import com.prisma.shared.models.IdType.Id
import com.prisma.shared.models.ModelMutationType.ModelMutationType
import com.prisma.shared.models._
import sangria.parser.QueryParser
import spray.json.{JsValue, _}

import scala.concurrent.Future

//todo this does not handle upsert

object ServerSideSubscription {
  def extractFromMutactions(
      project: Project,
      mutactions: Seq[ClientSqlMutaction],
      requestId: Id
  )(implicit apiDependencies: ApiDependencies): Seq[ServerSideSubscription] = {
    val createMutactions = mutactions.collect { case x: CreateDataItem => x }
    val updateMutactions = mutactions.collect { case x: UpdateDataItem => x }
    val deleteMutactions = mutactions.collect { case x: DeleteDataItem => x }

    extractFromCreateMutactions(project, createMutactions, requestId) ++
      extractFromUpdateMutactions(project, updateMutactions, requestId) ++
      extractFromDeleteMutactions(project, deleteMutactions, requestId)
  }

  def extractFromCreateMutactions(
      project: Project,
      mutactions: Seq[CreateDataItem],
      requestId: Id
  )(implicit apiDependencies: ApiDependencies): Seq[ServerSideSubscription] = {
    for {
      mutaction <- mutactions
      sssFn     <- serverSideSubscriptionFunctionsFor(project, mutaction.model, ModelMutationType.Deleted)
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

  def extractFromUpdateMutactions(
      project: Project,
      mutactions: Seq[UpdateDataItem],
      requestId: Id
  )(implicit apiDependencies: ApiDependencies): Seq[ServerSideSubscription] = {
    for {
      mutaction <- mutactions
      sssFn     <- serverSideSubscriptionFunctionsFor(project, mutaction.model, ModelMutationType.Deleted)
    } yield {
      ServerSideSubscription(
        project,
        mutaction.model,
        ModelMutationType.Updated,
        sssFn,
        nodeId = mutaction.id,
        requestId = requestId,
        updatedFields = Some(mutaction.namesOfUpdatedFields.toList),
        previousValues = Some(mutaction.previousValues)
      )
    }

  }

  def extractFromDeleteMutactions(
      project: Project,
      mutactions: Seq[DeleteDataItem],
      requestId: Id
  )(implicit apiDependencies: ApiDependencies): Seq[ServerSideSubscription] = {
    for {
      mutaction <- mutactions
      sssFn     <- serverSideSubscriptionFunctionsFor(project, mutaction.path.root.model, ModelMutationType.Deleted)
    } yield {
      ServerSideSubscription(
        project,
        mutaction.path.root.model,
        ModelMutationType.Deleted,
        sssFn,
        nodeId = mutaction.id,
        requestId = requestId,
        previousValues = Some(mutaction.previousValues)
      )
    }
  }

  private def serverSideSubscriptionFunctionsFor(project: Project, model: Model, mutationType: ModelMutationType) = {
    def isServerSideSubscriptionForModelAndMutationType(function: ServerSideSubscriptionFunction): Boolean = {
      val queryDoc             = QueryParser.parse(function.query).get
      val modelNameInQuery     = QueryTransformer.getModelNameFromSubscription(queryDoc).get
      val mutationTypesInQuery = QueryTransformer.getMutationTypesFromSubscription(queryDoc)
      model.name == modelNameInQuery && mutationTypesInQuery.contains(mutationType)
    }
    project.serverSideSubscriptionFunctions.filter(isServerSideSubscriptionForModelAndMutationType)
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
)(implicit apiDependencies: ApiDependencies)
    extends Mutaction {
  import scala.concurrent.ExecutionContext.Implicits.global

  val webhookPublisher = apiDependencies.webhookPublisher

  override def execute: Future[MutactionExecutionResult] = {
    for {
      result <- executeQuery()
    } yield {
      result match {
        case Some(JsObject(fields)) if fields.contains("data") =>
          function.delivery match {
            case fn: WebhookDelivery =>
              val webhook = Webhook(
                projectId = project.id,
                functionName = function.name,
                requestId = requestId,
                url = fn.url,
                payload = JsObject(fields).compactPrint,
                id = requestId,
                headers = fn.headers.toMap
              )
              webhookPublisher.publish(webhook)
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
