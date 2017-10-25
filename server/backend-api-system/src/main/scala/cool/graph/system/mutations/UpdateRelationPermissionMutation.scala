package cool.graph.system.mutations

import _root_.akka.actor.ActorSystem
import cool.graph._
import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.models
import cool.graph.system.mutactions.internal.{BumpProjectRevision, InvalidateSchema, UpdateRelationPermission}
import sangria.relay.Mutation
import scaldi.Injector

case class UpdateRelationPermissionMutation(
    client: models.Client,
    project: models.Project,
    relation: models.Relation,
    relationPermission: models.RelationPermission,
    args: UpdateRelationPermissionInput,
    projectDbsFn: models.Project => InternalAndProjectDbs
)(
    implicit inj: Injector,
    actorSystem: ActorSystem
) extends InternalProjectMutation[UpdateRelationPermissionMutationPayload] {

  val updatedRelationPermission =
    models.RelationPermission(
      id = relationPermission.id,
      connect = args.connect.getOrElse(relationPermission.connect),
      disconnect = args.disconnect.getOrElse(relationPermission.disconnect),
      userType = args.userType.getOrElse(relationPermission.userType),
      rule = args.rule.getOrElse(relationPermission.rule),
      ruleName = args.ruleName match {
        case None => relationPermission.ruleName
        case x    => x
      },
      ruleGraphQuery = args.ruleGraphQuery match {
        case None => relationPermission.ruleGraphQuery
        case x    => x
      },
      ruleGraphQueryFilePath = args.ruleGraphQueryFilePath match {
        case None => relationPermission.ruleGraphQueryFilePath
        case x    => x
      },
      ruleWebhookUrl = args.ruleWebhookUrl match {
        case None => relationPermission.ruleWebhookUrl
        case x    => x
      },
      description = args.description match {
        case None => relationPermission.description
        case x    => x
      },
      isActive = args.isActive.getOrElse(relationPermission.isActive)
    )

  override def prepareActions(): List[Mutaction] = {

//    updatedRelationPermission.ruleGraphQuery.foreach { query =>
//      val queriesWithSameOpCount = relation.permissions.count(_.operation == updatedRelationPermission.operation) // Todo this count may be wrong
//
//      val queryName = updatedRelationPermission.ruleName match {
//        case Some(nameForRule) => nameForRule
//        case None              => QueryPermissionHelper.alternativeNameFromOperationAndInt(updatedRelationPermission.operation, queriesWithSameOpCount)
//      }
//
//      val args         = QueryPermissionHelper.permissionQueryArgsFromRelation(relation, project)
//      val treatedQuery = QueryPermissionHelper.prependNameAndRenderQuery(query, queryName: String, args: List[(String, String)])
//
//      val violations = QueryPermissionHelper.validatePermissionQuery(treatedQuery, project)
//      if (violations.nonEmpty)
//        actions ++= List(
//          InvalidInput(PermissionQueryIsInvalid(violations.mkString(""), updatedRelationPermission.ruleName.getOrElse(updatedRelationPermission.id))))
//    }

    actions :+= UpdateRelationPermission(relation = relation, oldPermission = relationPermission, permission = updatedRelationPermission)

    actions :+= BumpProjectRevision(project = project)

    actions :+= InvalidateSchema(project = project)

    actions
  }

  override def getReturnValue: Option[UpdateRelationPermissionMutationPayload] = {
    Some(
      UpdateRelationPermissionMutationPayload(
        clientMutationId = args.clientMutationId,
        project = project,
        relation = relation.copy(permissions = relation.permissions :+ updatedRelationPermission),
        relationPermission = updatedRelationPermission
      ))
  }
}

case class UpdateRelationPermissionMutationPayload(clientMutationId: Option[String],
                                                   project: models.Project,
                                                   relation: models.Relation,
                                                   relationPermission: models.RelationPermission)
    extends Mutation

case class UpdateRelationPermissionInput(clientMutationId: Option[String],
                                         id: String,
                                         connect: Option[Boolean],
                                         disconnect: Option[Boolean],
                                         userType: Option[models.UserType.Value],
                                         rule: Option[models.CustomRule.Value],
                                         ruleName: Option[String],
                                         ruleGraphQuery: Option[String],
                                         ruleWebhookUrl: Option[String],
                                         description: Option[String],
                                         isActive: Option[Boolean],
                                         ruleGraphQueryFilePath: Option[String] = None)
