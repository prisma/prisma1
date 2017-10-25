package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.shared.errors.{UserAPIErrors, UserInputErrors}
import cool.graph.shared.models.{Field, Model, Project}
import cool.graph.system.database.ModelToDbMapper
import cool.graph.system.database.client.ClientDbQueries
import cool.graph.system.database.tables.{FieldTable, RelayIdTable}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class CreateField(
    project: Project,
    model: Model,
    field: Field,
    migrationValue: Option[String],
    clientDbQueries: ClientDbQueries
) extends SystemSqlMutaction {
  override def execute: Future[SystemSqlStatementResult[Any]] = {
    val fields   = TableQuery[FieldTable]
    val relayIds = TableQuery[RelayIdTable]

    Future.successful(
      SystemSqlStatementResult(
        sqlAction = DBIO.seq(
          fields += ModelToDbMapper.convertField(model.id, field),
          relayIds += cool.graph.system.database.tables.RelayId(field.id, "Field")
        )))
  }

  override def rollback: Some[Future[SystemSqlStatementResult[Any]]] =
    Some(DeleteField(project, model, field, allowDeleteSystemField = true).execute)

  override def verify(): Future[Try[MutactionVerificationSuccess]] = {
    lazy val itemCount = clientDbQueries.itemCountForModel(model)

    if (field.isScalar && field.isRequired && migrationValue.isEmpty) {
      itemCount map {
        case 0 => doVerify
        case _ => Failure(UserInputErrors.RequiredAndNoMigrationValue(modelName = model.name, fieldName = field.name))
      }
    } else if (field.isUnique && migrationValue.nonEmpty) {
      itemCount map {
        case 0 =>
          doVerify

        case 1 =>
          doVerify

        case _ =>
          Failure(
            UserAPIErrors.UniqueConstraintViolation(
              model.name,
              s"${field.name} has more than one entry and can't be added as a unique field with a non-unique value."
            ))
      }
    } else {
      Future(doVerify)
    }
  }

  def doVerify: Try[MutactionVerificationSuccess] = {
    lazy val fieldValidations    = UpdateField.fieldValidations(field, migrationValue)
    lazy val relationValidations = relationValidation

    () match {
      case _ if fieldValidations.isFailure                                        => fieldValidations
      case _ if model.fields.exists(_.name.toLowerCase == field.name.toLowerCase) => Failure(UserInputErrors.FieldAreadyExists(field.name))
      case _ if field.relation.isDefined && relationValidations.isFailure         => relationValidations
      case _                                                                      => Success(MutactionVerificationSuccess())
    }
  }

  private def relationValidation: Try[MutactionVerificationSuccess] = {

    val relation              = field.relation.get
    val otherFieldsInRelation = project.getFieldsByRelationId(relation.id)

    // todo: Asserts are preconditions in the code.
    // Triggering one should make us reproduce the bug first thing in the morning.
    // let's find a good way to handle this.
    assert(otherFieldsInRelation.length <= 2)

    otherFieldsInRelation.length match {
      case 2 =>
        Failure(UserAPIErrors.RelationAlreadyFull(relationId = relation.id, field1 = otherFieldsInRelation.head.name, field2 = otherFieldsInRelation(1).name))
      case _ => Success(MutactionVerificationSuccess())
    }
  }
}
