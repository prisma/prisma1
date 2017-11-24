package cool.graph.client.mutactions

import com.typesafe.scalalogging.LazyLogging
import cool.graph.Types.Id
import cool.graph._
import cool.graph.client.ClientInjector
import cool.graph.client.schema.simple.SimpleSchemaModelObjectTypeBuilder
import cool.graph.deprecated.actions.schemas.CreateSchema
import cool.graph.deprecated.actions.{Event, MutationCallbackSchemaExecutor}
import cool.graph.shared.errors.UserAPIErrors.UnsuccessfulSynchronousMutationCallback
import cool.graph.shared.errors.{SystemErrors, UserFacingError}
import cool.graph.shared.models.{Action, Model, Project}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class ActionWebhookForCreateDataItemSync(model: Model, project: Project, nodeId: Id, action: Action, mutationId: Id, requestId: String)(
    implicit injector: ClientInjector)
    extends ActionWebhookMutaction
    with LazyLogging {

  override def execute: Future[MutactionExecutionResult] = {

    val webhookCaller = injector.webhookCaller
    implicit val inj  = injector.toScaldi

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
            .map {
              case true  => MutactionExecutionSuccess()
              case false => throw UnsuccessfulSynchronousMutationCallback()
          })
      .recover {
        case x: UserFacingError => throw x
        case x =>
          SystemErrors.UnknownExecutionError(x.getMessage, x.getStackTrace.map(_.toString).mkString(", "))
      }
  }
}
