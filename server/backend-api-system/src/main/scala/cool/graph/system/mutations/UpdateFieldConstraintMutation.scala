package cool.graph.system.mutations

import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.models
import cool.graph.shared.models._
import cool.graph.system.mutactions.internal.{BumpProjectRevision, InvalidateSchema, UpdateFieldConstraint}
import cool.graph.{InternalProjectMutation, Mutaction}
import sangria.relay.Mutation
import scaldi.Injector

case class UpdateFieldConstraintMutation(
    client: models.Client,
    project: models.Project,
    args: UpdateFieldConstraintInput,
    projectDbsFn: models.Project => InternalAndProjectDbs
)(implicit inj: Injector)
    extends InternalProjectMutation[UpdateFieldConstraintMutationPayload] {

  val constraint: FieldConstraint = project.getFieldConstraintById_!(args.constraintId)

  val updatedConstraint: FieldConstraint = constraint match {
    case x: StringConstraint =>
      x.copy(
        equalsString = newValue(x.equalsString, args.equalsString),
        oneOfString = newOneOfValue(x.oneOfString, args.oneOfString),
        minLength = newValue(x.minLength, args.minLength),
        maxLength = newValue(x.maxLength, args.maxLength),
        startsWith = newValue(x.startsWith, args.startsWith),
        endsWith = newValue(x.endsWith, args.endsWith),
        includes = newValue(x.includes, args.includes),
        regex = newValue(x.regex, args.regex)
      )
    case x: NumberConstraint =>
      x.copy(
        equalsNumber = newValue(x.equalsNumber, args.oneOfNumber),
        oneOfNumber = newOneOfValue(x.oneOfNumber, args.oneOfNumber),
        min = newValue(x.min, args.min),
        max = newValue(x.max, args.max),
        exclusiveMin = newValue(x.exclusiveMin, args.exclusiveMin),
        exclusiveMax = newValue(x.exclusiveMax, args.exclusiveMax),
        multipleOf = newValue(x.multipleOf, args.multipleOf)
      )
    case x: BooleanConstraint =>
      x.copy(equalsBoolean = newValue(x.equalsBoolean, args.equalsBoolean))
    case x: ListConstraint =>
      x.copy(uniqueItems = newValue(x.uniqueItems, args.uniqueItems),
             minItems = newValue(x.minItems, args.minItems),
             maxItems = newValue(x.maxItems, args.maxItems))
  }

  private def newValue[A](oldValue: Option[A], input: Any): Option[A] = {
    input match {
      case None              => oldValue
      case Some(Some(valid)) => Some(valid.asInstanceOf[A])
      case Some(None)        => None
    }
  }

  private def newOneOfValue[A](oldValue: List[A], input: Any): List[A] = {
    input match {
      case None              => oldValue
      case Some(Some(valid)) => valid.asInstanceOf[List[A]]
      case Some(None)        => List.empty
    }
  }

  val field: Field                                      = project.getFieldById_!(constraint.fieldId)
  val updatedFieldConstraintList: List[FieldConstraint] = field.constraints.filter(_.id != updatedConstraint.id) :+ updatedConstraint
  val fieldWithUpdatedFieldConstraint: Field            = field.copy(constraints = updatedFieldConstraintList)
  val model: Model                                      = project.getModelByFieldId_!(field.id)
  val modelsWithUpdatedFieldConstraint: List[Model] = project.models.filter(_.id != model.id) :+ model.copy(
    fields = model.fields.filter(_.id != field.id) :+ fieldWithUpdatedFieldConstraint)
  val newProject: Project = project.copy(models = modelsWithUpdatedFieldConstraint)

  override def prepareActions(): List[Mutaction] = {
    actions = List(
      UpdateFieldConstraint(field = field, oldConstraint = constraint, constraint = updatedConstraint),
      BumpProjectRevision(project = project),
      InvalidateSchema(project)
    )
    actions
  }

  override def getReturnValue: Option[UpdateFieldConstraintMutationPayload] = {
    Some(UpdateFieldConstraintMutationPayload(args.clientMutationId, newProject, fieldWithUpdatedFieldConstraint, fieldWithUpdatedFieldConstraint.constraints))
  }
}

case class UpdateFieldConstraintMutationPayload(clientMutationId: Option[String], project: models.Project, field: Field, constraints: List[FieldConstraint])
    extends Mutation

case class UpdateFieldConstraintInput(clientMutationId: Option[String],
                                      constraintId: String,
                                      equalsString: Option[Option[Any]] = None,
                                      oneOfString: Option[Option[Any]] = None,
                                      minLength: Option[Option[Int]] = None,
                                      maxLength: Option[Option[Int]] = None,
                                      startsWith: Option[Option[Any]] = None,
                                      endsWith: Option[Option[Any]] = None,
                                      includes: Option[Option[Any]] = None,
                                      regex: Option[Option[Any]] = None,
                                      equalsNumber: Option[Option[Any]] = None,
                                      oneOfNumber: Option[Option[Any]] = None,
                                      min: Option[Option[Any]] = None,
                                      max: Option[Option[Any]] = None,
                                      exclusiveMin: Option[Option[Any]] = None,
                                      exclusiveMax: Option[Option[Any]] = None,
                                      multipleOf: Option[Option[Any]] = None,
                                      equalsBoolean: Option[Option[Any]] = None,
                                      uniqueItems: Option[Option[Any]] = None,
                                      minItems: Option[Option[Int]] = None,
                                      maxItems: Option[Option[Int]] = None)
    extends MutationInput
