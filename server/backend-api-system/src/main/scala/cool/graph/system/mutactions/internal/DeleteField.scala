package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.shared.errors.SystemErrors
import cool.graph.shared.models.{Field, Model, Project}
import cool.graph.system.database.client.EmptyClientDbQueries
import cool.graph.system.database.tables.{FieldTable, RelayIdTable}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class DeleteField(
    project: Project,
    model: Model,
    field: Field,
    allowDeleteSystemField: Boolean = false,
    allowDeleteRelationField: Boolean = false
) extends SystemSqlMutaction {

  override def execute: Future[SystemSqlStatementResult[Any]] = {
    val fields   = TableQuery[FieldTable]
    val relayIds = TableQuery[RelayIdTable]

    Future.successful(SystemSqlStatementResult(sqlAction = DBIO.seq(fields.filter(_.id === field.id).delete, relayIds.filter(_.id === field.id).delete)))
  }

  override def rollback = Some(CreateField(project, model, field, None, EmptyClientDbQueries).execute)

  override def verify(): Future[Try[MutactionVerificationSuccess]] =
    Future.successful(() match {
      case _ if model.getFieldById(field.id).isEmpty =>
        Failure(SystemErrors.FieldNotInModel(fieldName = field.name, modelName = model.name))

      case _ if field.isSystem && !allowDeleteSystemField =>
        Failure(SystemErrors.SystemFieldCannotBeRemoved(fieldName = field.name))

      case _ if field.relation.isDefined && !allowDeleteRelationField =>
        Failure(SystemErrors.CantDeleteRelationField(fieldName = field.name))

      case _ =>
        Success(MutactionVerificationSuccess())
    })
}
