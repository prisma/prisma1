package cool.graph.system.mutations

import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.models
import cool.graph.shared.models._
import cool.graph.system.mutactions.internal.{BumpProjectRevision, DeleteFieldConstraint, InvalidateSchema}
import cool.graph.{InternalProjectMutation, Mutaction}
import sangria.relay.Mutation
import scaldi.Injector

case class DeleteFieldConstraintMutation(client: models.Client,
                                         project: models.Project,
                                         args: DeleteFieldConstraintInput,
                                         projectDbsFn: models.Project => InternalAndProjectDbs)(implicit inj: Injector)
    extends InternalProjectMutation[DeleteFieldConstraintMutationPayload] {

  val constraint: FieldConstraint = project.getFieldConstraintById_!(args.constraintId)

  val field: Field = project.getFieldById_!(constraint.fieldId)

  val fieldWithoutConstraint: Field = field.copy(constraints = field.constraints.filter(_.id != constraint.id))
  val model: Model                  = project.models.find(_.fields.contains(field)).get
  val modelsWithoutConstraint: List[Model] = project.models.filter(_.id != model.id) :+ model.copy(
    fields = model.fields.filter(_.id != field.id) :+ fieldWithoutConstraint)
  val newProject: Project = project.copy(models = modelsWithoutConstraint)

  override def prepareActions(): List[Mutaction] = {
    actions = List(DeleteFieldConstraint(project, constraint), BumpProjectRevision(project = project), InvalidateSchema(project))
    actions
  }

  override def getReturnValue: Option[DeleteFieldConstraintMutationPayload] = {
    Some(DeleteFieldConstraintMutationPayload(args.clientMutationId, newProject, constraint))
  }
}

case class DeleteFieldConstraintMutationPayload(clientMutationId: Option[String], project: models.Project, constraint: FieldConstraint) extends Mutation

case class DeleteFieldConstraintInput(clientMutationId: Option[String], constraintId: String)
