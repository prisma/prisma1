package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.shared.models._
import cool.graph.system.database.ModelToDbMapper
import cool.graph.system.database.tables.FieldConstraintTable
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future

case class UpdateFieldConstraint(field: Field, oldConstraint: FieldConstraint, constraint: FieldConstraint) extends SystemSqlMutaction {
  override def execute: Future[SystemSqlStatementResult[Any]] = {

    val constraints = TableQuery[FieldConstraintTable]

    val query = constraints.filter(_.id === constraint.id)

    Future.successful(SystemSqlStatementResult(sqlAction = DBIO.seq(query.update(ModelToDbMapper.convertFieldConstraint(constraint)))))

  }

  override def rollback = Some(UpdateFieldConstraint(field = field, oldConstraint = constraint, constraint = oldConstraint).execute)

}
