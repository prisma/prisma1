package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.shared.models._
import cool.graph.system.database.ModelToDbMapper
import cool.graph.system.database.tables.{FieldConstraintTable, RelayIdTable}
import scaldi.Injector
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future

case class CreateFieldConstraint(project: Project, constraint: FieldConstraint, fieldId: String)(implicit inj: Injector) extends SystemSqlMutaction {
  override def execute: Future[SystemSqlStatementResult[Any]] = {

    val constraints = TableQuery[FieldConstraintTable]
    val relayIds    = TableQuery[RelayIdTable]

    Future.successful {
      SystemSqlStatementResult {
        DBIO.seq(
          constraints += ModelToDbMapper.convertFieldConstraint(constraint),
          relayIds += cool.graph.system.database.tables.RelayId(constraint.id, constraints.baseTableRow.tableName)
        )
      }
    }
  }

  override def rollback = Some(DeleteFieldConstraint(project, constraint).execute)

}
