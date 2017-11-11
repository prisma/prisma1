package cool.graph.system

import com.typesafe.scalalogging.LazyLogging
import cool.graph.DataItem
import cool.graph.Types.Id
import cool.graph.client.schema.simple.SimpleSchemaModelObjectTypeBuilder
import cool.graph.deprecated.actions.schemas._
import cool.graph.shared.{ApiMatrixFactory}
import cool.graph.shared.models.{ActionTriggerMutationModelMutationType, ActionTriggerMutationRelationMutationType, ActionTriggerType, Project}
import sangria.execution.Executor
import sangria.introspection.introspectionQuery
import sangria.marshalling.sprayJson._
import sangria.schema.Schema
import scaldi.{Injectable, Injector}
import spray.json.JsObject

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class ActionSchemaPayload(
    triggerType: ActionTriggerType.Value,
    mutationModel: Option[ActionSchemaPayloadMutationModel],
    mutationRelation: Option[ActionSchemaPayloadMutationRelation]
)

case class ActionSchemaPayloadMutationModel(
    modelId: Id,
    mutationType: ActionTriggerMutationModelMutationType.Value
)

case class ActionSchemaPayloadMutationRelation(
    relationId: Id,
    mutationType: ActionTriggerMutationRelationMutationType.Value
)

class ActionSchemaResolver(implicit inj: Injector) extends Injectable with LazyLogging {

  def resolve(project: Project, payload: ActionSchemaPayload): Future[String] = {
    val apiMatrix = inject[ApiMatrixFactory].create(project)

    payload.triggerType match {
      case ActionTriggerType.MutationModel =>
        val model = apiMatrix.filterModel(project.getModelById_!(payload.mutationModel.get.modelId))

        model match {
          case None =>
            Future.successful(JsObject.empty.prettyPrint)
          case Some(model) =>
            val modelObjectTypes = new SimpleSchemaModelObjectTypeBuilder(project)

            val schema: Schema[ActionUserContext, Unit] =
              payload.mutationModel.get.mutationType match {
                case ActionTriggerMutationModelMutationType.Create =>
                  new CreateSchema(model = model, modelObjectTypes = modelObjectTypes, project = project).build()
                case ActionTriggerMutationModelMutationType.Update =>
                  new UpdateSchema(model = model,
                                   modelObjectTypes = modelObjectTypes,
                                   project = project,
                                   updatedFields = List(),
                                   previousValues = DataItem("dummy", Map())).build()
                case ActionTriggerMutationModelMutationType.Delete =>
                  new DeleteSchema(model = model, modelObjectTypes = modelObjectTypes, project = project).build()
              }

            Executor
              .execute(
                schema = schema,
                queryAst = introspectionQuery,
                userContext = ActionUserContext(
                  requestId = "",
                  project = project,
                  nodeId = model.id,
                  mutation = MutationMetaData(id = "", _type = ""),
                  log = (x: String) => logger.info(x)
                )
              )
              .map { response =>
                val JsObject(fields) = response
                fields("data").compactPrint
              }
        }
    }
  }
}
