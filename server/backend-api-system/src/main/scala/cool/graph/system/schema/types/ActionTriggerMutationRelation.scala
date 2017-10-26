package cool.graph.system.schema.types

import cool.graph.shared.errors.UserInputErrors
import sangria.schema._
import cool.graph.shared.models
import cool.graph.shared.models.ModelParser
import cool.graph.system.SystemUserContext
import cool.graph.system.database.finder.ProjectFinder
import cool.graph.system.schema.types.Relation.RelationContext

import scala.concurrent.Future

object ActionTriggerMutationRelation {
  import scala.concurrent.ExecutionContext.Implicits.global

  def throwNotFound(item: String) = throw new UserInputErrors.NotFoundException(s"${item} not found")

  lazy val Type: ObjectType[SystemUserContext, models.ActionTriggerMutationRelation] =
    ObjectType(
      "ActionTriggerMutationRelation",
      "This is an ActionTriggerMutationRelation",
      interfaces[SystemUserContext, models.ActionTriggerMutationRelation](nodeInterface),
      idField[SystemUserContext, models.ActionTriggerMutationRelation] ::
        fields[SystemUserContext, models.ActionTriggerMutationRelation](
        Field("fragment", StringType, resolve = _.value.fragment),
        Field(
          "relation",
          RelationType,
          resolve = ctx => {
            val clientId                        = ctx.ctx.getClient.id
            val relationId                      = ctx.value.relationId
            val project: Future[models.Project] = ProjectFinder.loadByRelationId(clientId, relationId)(ctx.ctx.internalDatabase, ctx.ctx.projectResolver)
            project.map { project =>
              ModelParser
                .relation(project, relationId, ctx.ctx.injector)
                .map(rel => RelationContext(project, rel))
                .getOrElse(throwNotFound("Relation"))
            }
          }
        ),
        Field("mutationType", RelationMutationType.Type, resolve = _.value.mutationType)
      )
    )
}

object RelationMutationType {
  lazy val Type = EnumType(
    "ActionTriggerMutationModelRelationType",
    None,
    List(
      EnumValue(models.ActionTriggerMutationRelationMutationType.Add.toString, value = models.ActionTriggerMutationRelationMutationType.Add),
      EnumValue(models.ActionTriggerMutationRelationMutationType.Remove.toString, value = models.ActionTriggerMutationRelationMutationType.Remove)
    )
  )
}
