package cool.graph.system.schema.types

import cool.graph.shared.models
import cool.graph.system.SystemUserContext
import cool.graph.system.schema.types.Model.ModelContext
import cool.graph.system.schema.types.Relation.RelationContext
import sangria.relay.Node
import sangria.schema._

object RelationPermission {

  case class RelationPermissionContext(project: models.Project, relationPermission: models.RelationPermission) extends Node {
    def id = relationPermission.id
  }
  lazy val Type: ObjectType[SystemUserContext, RelationPermissionContext] =
    ObjectType(
      "RelationPermission",
      "This is a relation permission",
      interfaces[SystemUserContext, RelationPermissionContext](nodeInterface),
      () =>
        idField[SystemUserContext, RelationPermissionContext] ::
          fields[SystemUserContext, RelationPermissionContext](
          Field("ruleWebhookUrl", OptionType(StringType), resolve = _.value.relationPermission.ruleWebhookUrl),
          Field("rule", Rule.Type, resolve = _.value.relationPermission.rule),
          Field("ruleName", OptionType(StringType), resolve = _.value.relationPermission.ruleName),
          Field("ruleGraphQuery", OptionType(StringType), resolve = _.value.relationPermission.ruleGraphQuery),
          Field("isActive", BooleanType, resolve = _.value.relationPermission.isActive),
          Field("connect", BooleanType, resolve = _.value.relationPermission.connect),
          Field("disconnect", BooleanType, resolve = _.value.relationPermission.disconnect),
          Field("userType", UserType.Type, resolve = _.value.relationPermission.userType),
          Field("description", OptionType(StringType), resolve = _.value.relationPermission.description),
          Field(
            "relation",
            RelationType,
            resolve = ctx => {
              val project  = ctx.value.project
              val relation = project.getRelationByRelationPermissionId(ctx.value.id).get

              RelationContext(project, relation)
            }
          )
      )
    )
}
