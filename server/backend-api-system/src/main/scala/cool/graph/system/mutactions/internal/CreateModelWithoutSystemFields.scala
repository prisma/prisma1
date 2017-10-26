package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.client.database.DataResolver
import cool.graph.shared.NameConstraints
import cool.graph.shared.errors.UserInputErrors
import cool.graph.system.database.tables.{FieldTable, ModelTable, PermissionTable, RelayIdTable}
import cool.graph.shared.models._
import cool.graph.shared.schema.CustomScalarTypes
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class CreateModelWithoutSystemFields(project: Project, model: Model) extends SystemSqlMutaction {
  override def execute: Future[SystemSqlStatementResult[Any]] = {
    val models      = TableQuery[ModelTable]
    val fields      = TableQuery[FieldTable]
    val permissions = TableQuery[PermissionTable]
    val relayIds    = TableQuery[RelayIdTable]

    Future.successful(
      SystemSqlStatementResult(
        sqlAction = DBIO.seq(
          models += cool.graph.system.database.tables
            .Model(model.id, model.name, model.description, model.isSystem, project.id, fieldPositions = Seq.empty),
          relayIds +=
            cool.graph.system.database.tables.RelayId(model.id, "Model")
        )))
  }

  override def rollback = Some(DeleteModel(project, model).execute)

  override def verify(): Future[Try[MutactionVerificationSuccess]] = {
    Future.successful(
      () match {
        case _ if !NameConstraints.isValidModelName(model.name)               => Failure(UserInputErrors.InvalidName(name = model.name))
        case _ if CustomScalarTypes.isScalar(model.name)                      => Failure(UserInputErrors.InvalidName(name = model.name))
        case _ if project.getModelByName(model.name).exists(_.id != model.id) => Failure(UserInputErrors.ModelWithNameAlreadyExists(name = model.name))
        case _                                                                => Success(MutactionVerificationSuccess())

      }
    )
  }
}
