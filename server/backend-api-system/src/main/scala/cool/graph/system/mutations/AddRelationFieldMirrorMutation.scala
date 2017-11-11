package cool.graph.system.mutations

import cool.graph.cuid.Cuid
import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.models
import cool.graph.system.mutactions.client.{CreateRelationFieldMirrorColumn, PopulateRelationFieldMirrorColumn}
import cool.graph.system.mutactions.internal.{BumpProjectRevision, CreateRelationFieldMirror, InvalidateSchema}
import cool.graph.{InternalProjectMutation, Mutaction}
import sangria.relay.Mutation
import scaldi.Injector

case class AddRelationFieldMirrorMutation(client: models.Client,
                                          project: models.Project,
                                          relation: models.Relation,
                                          args: AddRelationFieldMirrorInput,
                                          projectDbsFn: models.Project => InternalAndProjectDbs)(implicit inj: Injector)
    extends InternalProjectMutation[AddRelationFieldMirrorPayload] {

  val newRelationFieldMirror = models.RelationFieldMirror(id = Cuid.createCuid(), fieldId = args.fieldId, relationId = args.relationId)

  override def prepareActions(): List[Mutaction] = {

    val addFieldMirror = CreateRelationFieldMirror(project = project, relationFieldMirror = newRelationFieldMirror)

    val field = project.getFieldById_!(args.fieldId)

    val addColumn = CreateRelationFieldMirrorColumn(
      project = project,
      relation = relation,
      field = field
    )

    val populateColumn = PopulateRelationFieldMirrorColumn(project, relation, field)

    actions = List(addFieldMirror, addColumn, populateColumn, BumpProjectRevision(project = project), InvalidateSchema(project = project))
    actions
  }

  override def getReturnValue(): Option[AddRelationFieldMirrorPayload] = {
    Some(
      AddRelationFieldMirrorPayload(
        clientMutationId = args.clientMutationId,
        project = project,
        relationFieldMirror = newRelationFieldMirror,
        relation = relation.copy(fieldMirrors = relation.fieldMirrors :+ newRelationFieldMirror)
      ))
  }
}

case class AddRelationFieldMirrorPayload(clientMutationId: Option[String],
                                         project: models.Project,
                                         relationFieldMirror: models.RelationFieldMirror,
                                         relation: models.Relation)
    extends Mutation

case class AddRelationFieldMirrorInput(clientMutationId: Option[String], fieldId: String, relationId: String)
