package cool.graph.system.mutations

import cool.graph._
import cool.graph.cuid.Cuid
import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.errors.SystemErrors
import cool.graph.shared.models
import cool.graph.shared.models.FieldConstraintType.FieldConstraintType
import cool.graph.shared.models._
import cool.graph.shared.mutactions.InvalidInput
import cool.graph.system.mutactions.internal.{BumpProjectRevision, CreateFieldConstraint, InvalidateSchema}
import sangria.relay.Mutation
import scaldi.Injector

case class AddFieldConstraintMutation(client: models.Client,
                                      project: models.Project,
                                      args: AddFieldConstraintInput,
                                      projectDbsFn: models.Project => InternalAndProjectDbs)(implicit inj: Injector)
    extends InternalProjectMutation[AddFieldConstraintMutationPayload] {

  val newConstraint: FieldConstraint = args.constraintType match {
    case FieldConstraintType.STRING =>
      val oneOfString = args.oneOfString.map(_.toList).getOrElse(List.empty)
      StringConstraint(
        id = Cuid.createCuid(),
        fieldId = args.fieldId,
        equalsString = args.equalsString,
        oneOfString = oneOfString,
        minLength = args.minLength,
        maxLength = args.maxLength,
        startsWith = args.startsWith,
        endsWith = args.endsWith,
        includes = args.includes,
        regex = args.regex
      )
    case FieldConstraintType.NUMBER =>
      val oneOfNumber = args.oneOfNumber.map(_.toList).getOrElse(List.empty)
      NumberConstraint(
        id = Cuid.createCuid(),
        fieldId = args.fieldId,
        equalsNumber = args.equalsNumber,
        oneOfNumber = oneOfNumber,
        min = args.min,
        max = args.max,
        exclusiveMin = args.exclusiveMin,
        exclusiveMax = args.exclusiveMax,
        multipleOf = args.multipleOf
      )
    case FieldConstraintType.BOOLEAN =>
      BooleanConstraint(id = Cuid.createCuid(), fieldId = args.fieldId, equalsBoolean = args.equalsBoolean)
    case FieldConstraintType.LIST =>
      ListConstraint(id = Cuid.createCuid(), fieldId = args.fieldId, uniqueItems = args.uniqueItems, minItems = args.minItems, maxItems = args.maxItems)
  }

  val field = project.getFieldById_!(args.fieldId)

  val updatedField = field.copy(constraints = field.constraints :+ newConstraint)
  val fieldType    = field.typeIdentifier

  override def prepareActions(): List[Mutaction] = {

    newConstraint.constraintType match {
      case _ if field.constraints.exists(_.constraintType == newConstraint.constraintType) =>
        actions = duplicateConstraint
      case FieldConstraintType.STRING if fieldType != TypeIdentifier.String   => actions = fieldConstraintTypeError
      case FieldConstraintType.BOOLEAN if fieldType != TypeIdentifier.Boolean => actions = fieldConstraintTypeError
      case FieldConstraintType.NUMBER if fieldType != TypeIdentifier.Float && fieldType != TypeIdentifier.Int =>
        actions = fieldConstraintTypeError
      case FieldConstraintType.LIST if !field.isList => actions = fieldConstraintListError
      case _ =>
        actions = List(
          CreateFieldConstraint(project = project, fieldId = args.fieldId, constraint = newConstraint),
          BumpProjectRevision(project = project),
          InvalidateSchema(project = project)
        )
    }
    actions
  }

  override def getReturnValue(): Option[AddFieldConstraintMutationPayload] = {
    Some(
      AddFieldConstraintMutationPayload(clientMutationId = args.clientMutationId,
                                        project = project,
                                        field = updatedField,
                                        constraints = updatedField.constraints))
  }

  def duplicateConstraint = {
    List(InvalidInput(SystemErrors.DuplicateFieldConstraint(constraintType = newConstraint.constraintType.toString, fieldId = field.id)))
  }

  def fieldConstraintTypeError = {
    List(
      InvalidInput(
        SystemErrors.FieldConstraintTypeNotCompatibleWithField(constraintType = newConstraint.constraintType.toString,
                                                               fieldId = field.id,
                                                               fieldType = field.typeIdentifier.toString)))
  }

  def fieldConstraintListError = List(InvalidInput(SystemErrors.ListFieldConstraintOnlyOnListFields(field.id)))
}

case class AddFieldConstraintMutationPayload(clientMutationId: Option[String], project: models.Project, field: models.Field, constraints: List[FieldConstraint])
    extends Mutation

case class AddFieldConstraintInput(clientMutationId: Option[String],
                                   fieldId: String,
                                   constraintType: FieldConstraintType,
                                   equalsString: Option[String] = None,
                                   oneOfString: Option[Seq[String]] = None,
                                   minLength: Option[Int] = None,
                                   maxLength: Option[Int] = None,
                                   startsWith: Option[String] = None,
                                   endsWith: Option[String] = None,
                                   includes: Option[String] = None,
                                   regex: Option[String] = None,
                                   equalsNumber: Option[Double] = None,
                                   oneOfNumber: Option[Seq[Double]] = None,
                                   min: Option[Double] = None,
                                   max: Option[Double] = None,
                                   exclusiveMin: Option[Double] = None,
                                   exclusiveMax: Option[Double] = None,
                                   multipleOf: Option[Double] = None,
                                   equalsBoolean: Option[Boolean] = None,
                                   uniqueItems: Option[Boolean] = None,
                                   minItems: Option[Int] = None,
                                   maxItems: Option[Int] = None)
