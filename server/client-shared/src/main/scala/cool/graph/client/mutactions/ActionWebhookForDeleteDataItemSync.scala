package cool.graph.client.mutactions

import com.typesafe.scalalogging.LazyLogging
import cool.graph.Types.Id
import cool.graph.shared.errors.UserAPIErrors.UnsuccessfulSynchronousMutationCallback
import cool.graph._
import cool.graph.deprecated.actions.schemas._
import cool.graph.client.database.DataResolver
import cool.graph.client.schema.simple.SimpleSchemaModelObjectTypeBuilder
import cool.graph.deprecated.actions.{Event, MutationCallbackSchemaExecutor}
import cool.graph.shared.models.{Action, Model, Project}
import cool.graph.shared.errors.SystemErrors
import cool.graph.webhook.WebhookCaller
import scaldi._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Success

abstract class ActionWebhookForDeleteDataItem extends ActionWebhookMutaction {
  def prepareData: Future[Event]
}

case class ActionWebhookForDeleteDataItemSync(model: Model, project: Project, nodeId: Id, action: Action, mutationId: Id, requestId: String)(
    implicit inj: Injector)
    extends ActionWebhookForDeleteDataItem
    with Injectable
    with LazyLogging {

  // note: as the node is being deleted we need to resolve the query before executing this mutaction.
  // This is different than the normal execution flow for mutactions, so please be careful!
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

    payload.andThen({ case Success(x) => data = Some(x) })
  }

  var data: Option[Event]                 = None
  var prepareDataError: Option[Exception] = None

  override def execute: Future[MutactionExecutionResult] = {

    prepareDataError match {
      case Some(x) =>
        SystemErrors.UnknownExecutionError(x.getMessage, x.getStackTrace.map(_.toString).mkString(", "))
        Future.successful(MutactionExecutionSuccess())

      case None =>
        data match {
          case None =>
            sys.error("prepareData should be invoked and awaited before executing this mutaction")

          case Some(event) =>
            val webhookCaller = inject[WebhookCaller]

            webhookCaller
              .call(event.url, event.payload.map(_.compactPrint).getOrElse(""))
              .map {
                case true  => MutactionExecutionSuccess()
                case false => throw UnsuccessfulSynchronousMutationCallback()
              }
        }
    }
  }
}
