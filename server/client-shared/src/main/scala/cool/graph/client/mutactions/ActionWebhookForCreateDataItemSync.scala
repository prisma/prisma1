package cool.graph.client.mutactions

import com.typesafe.scalalogging.LazyLogging
import cool.graph.Types.Id
import cool.graph.shared.errors.UserAPIErrors.UnsuccessfulSynchronousMutationCallback
import cool.graph._
import cool.graph.deprecated.actions.schemas.CreateSchema
import cool.graph.client.database.DataResolver
import cool.graph.client.schema.simple.SimpleSchemaModelObjectTypeBuilder
import cool.graph.deprecated.actions.{Event, MutationCallbackSchemaExecutor}
import cool.graph.shared.models.{Action, Model, Project}
import cool.graph.shared.errors.{SystemErrors, UserFacingError}
import cool.graph.webhook.WebhookCaller
import scaldi._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Success

case class ActionWebhookForCreateDataItemSync(model: Model, project: Project, nodeId: Id, action: Action, mutationId: Id, requestId: String)(
    implicit inj: Injector)
    extends ActionWebhookMutaction
    with Injectable
    with LazyLogging {

  override def execute: Future[MutactionExecutionResult] = {

    val webhookCaller = inject[WebhookCaller]

    val payload: Future[Event] =
      new MutationCallbackSchemaExecutor(
        project,
        model,
        new CreateSchema(model = model, modelObjectTypes = new SimpleSchemaModelObjectTypeBuilder(project = project), project = project).build(),
        nodeId,
        action.triggerMutationModel.get.fragment,
        action.handlerWebhook.get.url,
        mutationId
      ).execute

    payload
      .flatMap(
        payload =>
          webhookCaller
            .call(payload.url, payload.payload.map(_.compactPrint).getOrElse(""))
            .map(wasSuccess =>
              wasSuccess match {
                case true => MutactionExecutionSuccess()
                case false =>
                  throw new UnsuccessfulSynchronousMutationCallback()
            }))
      .recover {
        case x: UserFacingError => throw x
        case x =>
          SystemErrors.UnknownExecutionError(x.getMessage, x.getStackTrace.map(_.toString).mkString(", "))
      }
  }
}
