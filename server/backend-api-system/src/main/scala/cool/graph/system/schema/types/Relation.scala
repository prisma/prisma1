package cool.graph.system.schema.types

import cool.graph.shared.models
import cool.graph.system.SystemUserContext
import cool.graph.system.schema.types.Model.ModelContext
import cool.graph.system.schema.types.RelationPermission.RelationPermissionContext
import cool.graph.system.schema.types._Field.FieldContext
import sangria.relay._
import sangria.schema._

object Relation {
  case class RelationContext(project: models.Project, relation: models.Relation) extends Node {
    override def id: String = relation.id
  }

  lazy val Type: ObjectType[SystemUserContext, RelationContext] = ObjectType[SystemUserContext, RelationContext](
    "Relation",
    "This is a relation",
    interfaces[SystemUserContext, RelationContext](nodeInterface),
    idField[SystemUserContext, RelationContext] ::
      fields[SystemUserContext, RelationContext](
      Field(
        "leftModel",
        ModelType,
        resolve = ctx => {
          val project = ctx.value.project
          val model   = project.getModelById_!(ctx.value.relation.modelAId)

          ModelContext(project, model)
        }
      ),
      Field(
        "fieldOnLeftModel",
        FieldType,
        resolve = ctx => {
          val project = ctx.value.project
          val field   = ctx.value.relation.getModelAField_!(project)

          FieldContext(project, field)
        }
      ),
      Field(
        "rightModel",
        ModelType,
        resolve = ctx => {
          val project = ctx.value.project
          val model   = project.getModelById_!(ctx.value.relation.modelBId)

          ModelContext(project, model)
        }
      ),
      Field(
        "fieldOnRightModel",
        FieldType,
        resolve = ctx => {
          val project           = ctx.value.project
          val relation          = ctx.value.relation
          val fieldOnRightModel = relation.getModelBField_!(project)

          FieldContext(project, fieldOnRightModel)
        }
      ),
      Field(
        "permissions",
        relationPermissionConnection,
        arguments = Connection.Args.All,
        resolve = ctx => {
          val permissions = ctx.value.relation.permissions
            .sortBy(_.id)
            .map(relationPermission => RelationPermissionContext(ctx.value.project, relationPermission))

          Connection.connectionFromSeq(permissions, ConnectionArgs(ctx))
        }
      ),
      Field("name", StringType, resolve = ctx => ctx.value.relation.name),
      Field("description", OptionType(StringType), resolve = ctx => ctx.value.relation.description),
      Field("fieldMirrors", ListType(RelationFieldMirrorType), resolve = ctx => ctx.value.relation.fieldMirrors),
      Field("permissionSchema", StringType, resolve = ctx => {
        ctx.ctx.getRelationPermissionSchema(ctx.value.project, ctx.value.id)
      }),
      Field(
        "permissionQueryArguments",
        ListType(PermissionQueryArgument.Type),
        resolve = ctx => {
          PermissionQueryArguments.getRelationArguments(ctx.value.relation, project = ctx.value.project)
        }
      )
    )
  )
}
