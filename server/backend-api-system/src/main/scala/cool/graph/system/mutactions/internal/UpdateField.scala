package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.shared.NameConstraints
import cool.graph.shared.errors.{SystemErrors, UserInputErrors}
import cool.graph.shared.models.{Field, Model, TypeIdentifier}
import cool.graph.system.database.ModelToDbMapper
import cool.graph.system.database.client.ClientDbQueries
import cool.graph.system.database.tables.FieldTable
import cool.graph.system.mutactions.internal.validations.{EnumValueValidation, MigrationAndDefaultValueValidation}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class UpdateField(
    model: Model,
    oldField: Field,
    field: Field,
    migrationValue: Option[String],
    newModelId: Option[String] = None,
    clientDbQueries: ClientDbQueries
) extends SystemSqlMutaction {

  override def execute: Future[SystemSqlStatementResult[Any]] = {
    val fields = TableQuery[FieldTable]

    Future.successful(SystemSqlStatementResult(sqlAction = DBIO.seq({
      val q = for { f <- fields if f.id === field.id } yield f
      q.update(ModelToDbMapper.convertField(newModelId.getOrElse(model.id), field))
    })))
  }

  override def rollback: Some[Future[SystemSqlStatementResult[Any]]] = {
    Some(
      UpdateField(
        model = model,
        oldField = oldField,
        field = oldField,
        migrationValue = None,
        newModelId = Some(model.id),
        clientDbQueries = clientDbQueries
      ).execute
    )
  }

  def isUpdatingIllegalProperty(oldField: Field, newField: Field): Boolean = {
    oldField.typeIdentifier != newField.typeIdentifier ||
    oldField.name != newField.name || oldField.isList != newField.isList ||
    oldField.isUnique != newField.isUnique ||
    oldField.isRequired != newField.isRequired ||
    oldField.defaultValue != newField.defaultValue
  }

  override def verify(): Future[Try[MutactionVerificationSuccess]] = {
    //if a model gets renamed in a SchemaMigration the resolver uses the new table name although that transaction has not been performed yet.

    lazy val nodeExists                 = clientDbQueries.existsByModel(model)
    lazy val nodeWithNullFieldExists    = clientDbQueries.existsNullByModelAndScalarField(model, field)
    lazy val nodeWithNullRelationExists = clientDbQueries.existsNullByModelAndRelationField(model, field)
    lazy val nodeAndScalarExists        = Future.sequence(List(nodeExists, nodeWithNullFieldExists))
    lazy val nodeAndRelationExists      = Future.sequence(List(nodeExists, nodeWithNullRelationExists))

    def relationChecks(nodeExistsAndRelationExists: List[Boolean]): Try[MutactionVerificationSuccess] = {

      if (nodeExistsAndRelationExists.head) Failure(UserInputErrors.RelationChangedFromListToSingleAndNodesPresent(field.name))
      else if (nodeExistsAndRelationExists(1)) Failure(UserInputErrors.SettingRelationRequiredButNodesExist(field.name))
      else doVerify
    }

    def scalarChecks(nodeExistsAndScalarFieldExists: List[Boolean]): Try[MutactionVerificationSuccess] = {
      if (nodeExistsAndScalarFieldExists.head) Failure(UserInputErrors.ChangedIsListAndNoMigrationValue(field.name))
      else if (nodeExistsAndScalarFieldExists(1)) Failure(UserInputErrors.RequiredAndNoMigrationValue(modelName = model.name, fieldName = field.name))
      else doVerify
    }

    val listToSingle       = oldField.isList && !field.isList
    val optionalToRequired = !oldField.isRequired && field.isRequired
    val changedListStatus  = oldField.isList != field.isList

    if (field.relation.isDefined) {
      (listToSingle, optionalToRequired) match {
        case (true, false) =>
          nodeExists map {
            case false => doVerify
            case true  => Failure(UserInputErrors.RelationChangedFromListToSingleAndNodesPresent(field.name))
          }

        case (false, true) =>
          nodeWithNullRelationExists map {
            case false => doVerify
            case true  => Failure(UserInputErrors.SettingRelationRequiredButNodesExist(field.name))
          }

        case (false, false) =>
          Future(doVerify)

        case (true, true) =>
          nodeAndRelationExists map relationChecks
      }
    } else if (field.relation.isEmpty && migrationValue.isEmpty) {
      (changedListStatus, optionalToRequired, UpdateField.typeChangeRequiresMigration(oldField, field)) match {
        case (false, false, false) =>
          Future(doVerify)

        case (true, false, false) =>
          nodeExists map {
            case false => doVerify
            case true  => Failure(UserInputErrors.ChangedIsListAndNoMigrationValue(field.name))
          }

        case (false, true, false) =>
          nodeWithNullFieldExists map {
            case false => doVerify
            case true  => Failure(UserInputErrors.RequiredAndNoMigrationValue(modelName = model.name, fieldName = field.name))
          }

        case (true, true, false) =>
          nodeAndScalarExists map scalarChecks

        case (_, _, true) =>
          nodeExists map {
            case false => doVerify //if there are no nodes, there can also be no scalarNullFields,
            case true  => Failure(UserInputErrors.TypeChangeRequiresMigrationValue(field.name)) //if there are nodes we always require migValue
          }
      }
    } else Future(doVerify)
  }

  def doVerify: Try[MutactionVerificationSuccess] = {

    lazy val fieldValidations                      = UpdateField.fieldValidations(field, migrationValue)
    lazy val updateFieldFieldValidations           = UpdateField.fieldValidations(field, migrationValue)
    lazy val fieldWithSameNameAndDifferentIdExists = model.fields.exists(x => x.name.toLowerCase == field.name.toLowerCase && x.id != field.id)

    () match {
      case _ if model.getFieldById(field.id).isEmpty =>
        Failure(SystemErrors.FieldNotInModel(fieldName = field.name, modelName = model.name))

      case _ if field.isSystem && isUpdatingIllegalProperty(oldField = oldField, newField = field) =>
        Failure(SystemErrors.CannotUpdateSystemField(fieldName = field.name, modelName = model.name))

      case _ if fieldValidations.isFailure =>
        fieldValidations

      case _ if updateFieldFieldValidations.isFailure =>
        updateFieldFieldValidations

      case _ if fieldWithSameNameAndDifferentIdExists =>
        Failure(UserInputErrors.FieldAreadyExists(field.name))

      case _ =>
        Success(MutactionVerificationSuccess())
    }
  }
}

object UpdateField {
  def typeChangeRequiresMigration(oldField: Field, updatedField: Field): Boolean = {
    (oldField.typeIdentifier, updatedField.typeIdentifier) match {
      case (_, TypeIdentifier.String)                       => false
      case (oldType, updatedType) if oldType == updatedType => false
      case _                                                => true
    }
  }

  def fieldValidations(field: Field, migrationValue: Option[String]): Try[MutactionVerificationSuccess] = {
    lazy val isInvalidFieldName                 = !NameConstraints.isValidFieldName(field.name)
    lazy val defaultAndMigrationValueValidation = MigrationAndDefaultValueValidation.validateMigrationAndDefaultValue(migrationValue, field)
    lazy val enumValueValidation                = EnumValueValidation.validateEnumField(migrationValue, field)
    lazy val isRequiredManyRelation             = field.relation.isDefined && field.isList && field.isRequired

    () match {
      case _ if isInvalidFieldName                           => Failure(UserInputErrors.InvalidName(name = field.name))
      case _ if enumValueValidation.isFailure                => enumValueValidation
      case _ if defaultAndMigrationValueValidation.isFailure => defaultAndMigrationValueValidation
      case _ if isRequiredManyRelation                       => Failure(UserInputErrors.ListRelationsCannotBeRequired(field.name))
      case _                                                 => Success(MutactionVerificationSuccess())
    }
  }

}
