package cool.graph.client.mutactions

import com.typesafe.scalalogging.LazyLogging
import cool.graph.Types.Id
import cool.graph._
import cool.graph.client.schema.simple.SimpleSchemaModelObjectTypeBuilder
import cool.graph.deprecated.actions.schemas._
import cool.graph.deprecated.actions.{Event, MutationCallbackSchemaExecutor}
import cool.graph.shared.errors.UserAPIErrors.UnsuccessfulSynchronousMutationCallback
import cool.graph.shared.errors.{SystemErrors, UserFacingError}
import cool.graph.shared.models.{Action, Model, Project}
import cool.graph.webhook.WebhookCaller
import scaldi._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class ActionWebhookForUpdateDataItemSync(model: Model,
                                              project: Project,
                                              nodeId: Id,
                                              action: Action,
                                              updatedFields: List[String],
                                              mutationId: Id,
                                              requestId: String,
                                              previousValues: DataItem)(implicit inj: Injector)
    extends ActionWebhookMutaction
    with Injectable
    with LazyLogging {

  override def execute: Future[MutactionExecutionResult] = {

    val webhookCaller = inject[WebhookCaller]

    val payload: Future[Event] =
      new MutationCallbackSchemaExecutor(
        project,
        model,
        new UpdateSchema(
          model = model,
          modelObjectTypes = new SimpleSchemaModelObjectTypeBuilder(project = project),
          project = project,
          updatedFields = updatedFields,
          previousValues = previousValues
        ).build(),
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
            .map {
              case true  => MutactionExecutionSuccess()
              case false => throw UnsuccessfulSynchronousMutationCallback()
          })
      .recover {
        case x: UserFacingError => throw x
        case x                  => SystemErrors.UnknownExecutionError(x.getMessage, x.getStackTrace.map(_.toString).mkString(", "))
      }
  }
}
