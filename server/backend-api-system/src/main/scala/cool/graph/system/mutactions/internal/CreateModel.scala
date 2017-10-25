package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.shared.NameConstraints
import cool.graph.shared.errors.UserInputErrors
import cool.graph.shared.models._
import cool.graph.shared.schema.CustomScalarTypes
import cool.graph.system.database.ModelToDbMapper
import cool.graph.system.database.tables.{FieldTable, ModelTable, RelayIdTable}
import cool.graph.system.mutactions.internal.validations.TypeNameValidation
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future
import scala.util.{Failure, Try}

case class CreateModel(project: Project, model: Model) extends SystemSqlMutaction {

  override def execute: Future[SystemSqlStatementResult[Any]] = {
    val models   = TableQuery[ModelTable]
    val fields   = TableQuery[FieldTable]
    val relayIds = TableQuery[RelayIdTable]

    Future.successful(
      SystemSqlStatementResult(
        sqlAction = DBIO
          .seq(
            models += ModelToDbMapper.convertModel(project, model),
            relayIds += cool.graph.system.database.tables.RelayId(model.id, "Model"),
            fields ++= model.fields.map(f => ModelToDbMapper.convertField(model.id, f)),
            relayIds ++= model.fields.map(f => cool.graph.system.database.tables.RelayId(f.id, "Field"))
          )))
  }

  override def rollback = Some(DeleteModel(project, model).execute)

  override def verify(): Future[Try[MutactionVerificationSuccess]] = {
    if (!NameConstraints.isValidModelName(model.name)) {
      return Future.successful(Failure(UserInputErrors.InvalidName(name = model.name)))
    }

    if (CustomScalarTypes.isScalar(model.name)) {
      return Future.successful(Failure(UserInputErrors.InvalidName(name = model.name)))
    }

    if (project.getModelByName(model.name).exists(_.id != model.id)) {
      return Future.successful(Failure(UserInputErrors.ModelWithNameAlreadyExists(name = model.name)))
    }

    Future.successful {
      for {
        _ <- TypeNameValidation.validateModelName(project, model.name)
      } yield {
        MutactionVerificationSuccess()
      }
    }
  }
}
