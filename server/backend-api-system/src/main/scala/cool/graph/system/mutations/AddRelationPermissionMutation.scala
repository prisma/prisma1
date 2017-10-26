package cool.graph.system.mutations

import _root_.akka.actor.ActorSystem
import _root_.akka.stream.ActorMaterializer
import cool.graph._
import cool.graph.cuid.Cuid
import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.models
import cool.graph.shared.models.{Project, Relation}
import cool.graph.system.mutactions.internal.{BumpProjectRevision, CreateRelationPermission, InvalidateSchema}
import sangria.relay.Mutation
import scaldi.Injector

case class AddRelationPermissionMutation(
    client: models.Client,
    project: models.Project,
    relation: models.Relation,
    args: AddRelationPermissionInput,
    projectDbsFn: models.Project => InternalAndProjectDbs
)(
    implicit inj: Injector,
    actorSystem: ActorSystem
) extends InternalProjectMutation[AddRelationPermissionMutationPayload] {

  //at the moment the console sends empty strings, these would cause problems for the rendering of the clientInterchange
  val ruleName: Option[String] = args.ruleName match {
    case Some("") => None
    case x        => x
  }

  val newRelationPermission = models.RelationPermission(
    id = Cuid.createCuid(),
    connect = args.connect,
    disconnect = args.disconnect,
    userType = args.userType,
    rule = args.rule,
    ruleName = ruleName,
    ruleGraphQuery = args.ruleGraphQuery,
    ruleGraphQueryFilePath = args.ruleGraphQueryFilePath,
    ruleWebhookUrl = args.ruleWebhookUrl,
    description = args.description,
    isActive = args.isActive
  )

  val updatedRelation: Relation = relation.copy(permissions = relation.permissions :+ newRelationPermission)

  val updatedProject: Project = project.copy(relations = project.relations.filter(_.id != updatedRelation.id) :+ updatedRelation)

  override def prepareActions(): List[Mutaction] = {

//    newRelationPermission.ruleGraphQuery.foreach { query =>
//      val queriesWithSameOpCount = relation.permissions.count(_.operation == newRelationPermission.operation)
//
//      val queryName = newRelationPermission.ruleName match {
//        case Some(nameForRule) => nameForRule
//        case None              => QueryPermissionHelper.alternativeNameFromOperationAndInt(newRelationPermission.operation, queriesWithSameOpCount)
//      }
//
//      val args         = QueryPermissionHelper.permissionQueryArgsFromRelation(relation, project)
//      val treatedQuery = QueryPermissionHelper.prependNameAndRenderQuery(query, queryName: String, args: List[(String, String)])
//
//      val violations = QueryPermissionHelper.validatePermissionQuery(treatedQuery, project)
//      if (violations.nonEmpty)
//        actions ++= List(InvalidInput(PermissionQueryIsInvalid(violations.mkString(""), newRelationPermission.ruleName.getOrElse(newRelationPermission.id))))
//    }

    actions :+= CreateRelationPermission(project = project, relation = relation, permission = newRelationPermission)

    actions :+= BumpProjectRevision(project = project)

    actions :+= InvalidateSchema(project = project)

    actions
  }

  override def getReturnValue: Option[AddRelationPermissionMutationPayload] = {
    Some(
      AddRelationPermissionMutationPayload(
        clientMutationId = args.clientMutationId,
        project = updatedProject,
        relation = updatedRelation,
        relationPermission = newRelationPermission
      ))
  }
}

case class AddRelationPermissionMutationPayload(clientMutationId: Option[String],
                                                project: models.Project,
                                                relation: models.Relation,
                                                relationPermission: models.RelationPermission)
    extends Mutation

case class AddRelationPermissionInput(clientMutationId: Option[String],
                                      relationId: String,
                                      connect: Boolean,
                                      disconnect: Boolean,
                                      userType: models.UserType.Value,
                                      rule: models.CustomRule.Value,
                                      ruleName: Option[String],
                                      ruleGraphQuery: Option[String],
                                      ruleWebhookUrl: Option[String],
                                      description: Option[String],
                                      isActive: Boolean,
                                      ruleGraphQueryFilePath: Option[String] = None)
