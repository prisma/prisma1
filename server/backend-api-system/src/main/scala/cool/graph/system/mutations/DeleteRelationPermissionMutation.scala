package cool.graph.system.mutations

import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.models._
import cool.graph.system.mutactions.internal.{BumpProjectRevision, DeleteRelationPermission, InvalidateSchema}
import cool.graph.{InternalProjectMutation, Mutaction}
import sangria.relay.Mutation
import scaldi.Injector

case class DeleteRelationPermissionMutation(client: Client,
                                            project: Project,
                                            relation: Relation,
                                            relationPermission: RelationPermission,
                                            args: DeleteRelationPermissionInput,
                                            projectDbsFn: Project => InternalAndProjectDbs)(implicit inj: Injector)
    extends InternalProjectMutation[DeleteRelationPermissionMutationPayload] {

  val newRelation: Relation = relation.copy(permissions = relation.permissions.filter(_.id != relationPermission.id))

  val updatedProject: Project = project.copy(relations = project.relations.map {
    case r if r.id == newRelation.id => newRelation
    case r                           => r
  })

  override def prepareActions(): List[Mutaction] = {

    actions :+= DeleteRelationPermission(project, relation = relation, permission = relationPermission)

    actions :+= BumpProjectRevision(project = project)

    actions :+= InvalidateSchema(project = project)

    actions
  }

  override def getReturnValue: Option[DeleteRelationPermissionMutationPayload] = {

    Some(
      DeleteRelationPermissionMutationPayload(
        clientMutationId = args.clientMutationId,
        relation = newRelation,
        relationPermission = relationPermission,
        project = updatedProject
      ))
  }
}

case class DeleteRelationPermissionMutationPayload(clientMutationId: Option[String],
                                                   relation: Relation,
                                                   relationPermission: RelationPermission,
                                                   project: Project)
    extends Mutation

case class DeleteRelationPermissionInput(clientMutationId: Option[String], relationPermissionId: String)
