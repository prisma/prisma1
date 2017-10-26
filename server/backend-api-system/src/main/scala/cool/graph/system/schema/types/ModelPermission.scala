package cool.graph.system.schema.types

import cool.graph.shared.models
import cool.graph.system.SystemUserContext
import cool.graph.system.schema.types.Model.ModelContext
import sangria.relay.Node
import sangria.schema._

object ModelPermission {

  case class ModelPermissionContext(project: models.Project, modelPermission: models.ModelPermission) extends Node {
    def id = modelPermission.id
  }
  lazy val Type: ObjectType[SystemUserContext, ModelPermissionContext] =
    ObjectType(
      "ModelPermission",
      "This is a model permission",
      interfaces[SystemUserContext, ModelPermissionContext](nodeInterface),
      () =>
        idField[SystemUserContext, ModelPermissionContext] ::
          fields[SystemUserContext, ModelPermissionContext](
          Field("fieldIds", ListType(StringType), resolve = _.value.modelPermission.fieldIds),
          Field("ruleWebhookUrl", OptionType(StringType), resolve = _.value.modelPermission.ruleWebhookUrl),
          Field("rule", Rule.Type, resolve = _.value.modelPermission.rule),
          Field("ruleName", OptionType(StringType), resolve = _.value.modelPermission.ruleName),
          Field("ruleGraphQuery", OptionType(StringType), resolve = _.value.modelPermission.ruleGraphQuery),
          Field("applyToWholeModel", BooleanType, resolve = _.value.modelPermission.applyToWholeModel),
          Field("isActive", BooleanType, resolve = _.value.modelPermission.isActive),
          Field("operation", Operation.Type, resolve = _.value.modelPermission.operation),
          Field("userType", UserType.Type, resolve = _.value.modelPermission.userType),
          Field("description", OptionType(StringType), resolve = _.value.modelPermission.description),
          Field(
            "model",
            ModelType,
            resolve = ctx => {
              val project = ctx.value.project
              val model   = project.getModelByModelPermissionId(ctx.value.id).get

              ModelContext(project, model)
            }
          )
      )
    )
}
