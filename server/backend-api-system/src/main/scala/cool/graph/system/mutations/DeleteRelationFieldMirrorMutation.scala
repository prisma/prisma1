package cool.graph.system.mutations

import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.models
import cool.graph.system.mutactions.internal.{BumpProjectRevision, DeleteRelationFieldMirror, InvalidateSchema}
import cool.graph.{InternalProjectMutation, Mutaction}
import sangria.relay.Mutation
import scaldi.Injector

case class DeleteRelationFieldMirrorMutation(client: models.Client,
                                             project: models.Project,
                                             relation: models.Relation,
                                             args: DeleteRelationFieldMirrorInput,
                                             projectDbsFn: models.Project => InternalAndProjectDbs)(implicit inj: Injector)
    extends InternalProjectMutation[DeleteRelationFieldMirrorPayload] {

  override def prepareActions(): List[Mutaction] = {
    val deleteRelationFieldMirrorToField =
      DeleteRelationFieldMirror(project = project, relationFieldMirror = relation.getRelationFieldMirrorById_!(args.relationFieldMirrorId))

    actions = List(deleteRelationFieldMirrorToField, BumpProjectRevision(project = project), InvalidateSchema(project = project))
    actions
  }

  override def getReturnValue: Option[DeleteRelationFieldMirrorPayload] = {
    Some(
      DeleteRelationFieldMirrorPayload(
        clientMutationId = args.clientMutationId,
        project = project,
        deletedId = args.relationFieldMirrorId,
        relation = relation.copy(fieldMirrors = relation.fieldMirrors.filter(_.id != args.relationFieldMirrorId))
      ))
  }
}

case class DeleteRelationFieldMirrorPayload(clientMutationId: Option[String], project: models.Project, deletedId: String, relation: models.Relation)
    extends Mutation

case class DeleteRelationFieldMirrorInput(clientMutationId: Option[String], relationFieldMirrorId: String)
