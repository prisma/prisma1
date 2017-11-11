package cool.graph.client.mutactions

import com.typesafe.scalalogging.LazyLogging
import cool.graph.Types.Id
import cool.graph._
import cool.graph.client.schema.simple.SimpleSchemaModelObjectTypeBuilder
import cool.graph.deprecated.actions.schemas._
import cool.graph.deprecated.actions.{Event, MutationCallbackSchemaExecutor}
import cool.graph.messagebus.QueuePublisher
import cool.graph.shared.errors.SystemErrors
import cool.graph.shared.models.{Action, Model, Project}
import cool.graph.webhook.Webhook
import scaldi._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Success

case class ActionWebhookForDeleteDataItemAsync(model: Model, project: Project, nodeId: Id, action: Action, mutationId: Id, requestId: String)(
    implicit inj: Injector)
    extends ActionWebhookForDeleteDataItem
    with Injectable
    with LazyLogging {

  // note: as the node is being deleted we need to resolve the query before executing this mutaction.
  // This is different than the normal execution flow for mutactions, so please be careful!
  var data: Option[Webhook]               = None
  var prepareDataError: Option[Exception] = None

  def prepareData: Future[Event] = {

    val payload: Future[Event] =
      new MutationCallbackSchemaExecutor(
        project,
        model,
        new DeleteSchema(model = model, modelObjectTypes = new SimpleSchemaModelObjectTypeBuilder(project = project), project = project).build(),
        nodeId,
        action.triggerMutationModel.get.fragment,
        action.handlerWebhook.get.url,
        mutationId
      ).execute

    payload.andThen({
      case Success(event) =>
        val whPayload = event.payload.map(p => p.compactPrint).getOrElse("")
        data = Some(Webhook(project.id, "", requestId, event.url, whPayload, event.id, Map.empty))
    })
  }

  override def execute: Future[MutactionExecutionResult] = {

    prepareDataError match {
      case Some(x) =>
        SystemErrors.UnknownExecutionError(x.getMessage, x.getStackTrace.map(_.toString).mkString(", "))
        Future.successful(MutactionExecutionSuccess())

      case None =>
        require(data.nonEmpty, "prepareData should be invoked and awaited before executing this mutaction")

        val webhookPublisher = inject[QueuePublisher[Webhook]](identified by "webhookPublisher")
        webhookPublisher.publish(data.get)

        Future.successful(MutactionExecutionSuccess())
    }
  }
}
