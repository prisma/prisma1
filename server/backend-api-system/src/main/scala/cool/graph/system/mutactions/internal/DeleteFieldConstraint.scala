package cool.graph.system.mutactions.internal

import cool.graph.shared.models.{FieldConstraint, Project}
import cool.graph.system.database.tables.{FieldConstraintTable, RelayIdTable}
import cool.graph.{SystemSqlMutaction, SystemSqlStatementResult}
import scaldi.Injector
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future

case class DeleteFieldConstraint(project: Project, constraint: FieldConstraint)(implicit inj: Injector) extends SystemSqlMutaction {
  override def execute: Future[SystemSqlStatementResult[Any]] = {
    val constraints = TableQuery[FieldConstraintTable]
    val relayIds    = TableQuery[RelayIdTable]

    Future.successful {
      SystemSqlStatementResult {
        DBIO.seq(
          constraints.filter(_.id === constraint.id).delete,
          relayIds.filter(_.id === constraint.id).delete
        )
      }
    }
  }

  override def rollback: Option[Future[SystemSqlStatementResult[Any]]] = Some(CreateFieldConstraint(project, constraint, constraint.fieldId).execute)

}
