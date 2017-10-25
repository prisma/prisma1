package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.shared.models.AlgoliaSyncQuery
import cool.graph.system.database.tables.AlgoliaSyncQueryTable
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future

case class UpdateAlgoliaSyncQuery(oldAlgoliaSyncQuery: AlgoliaSyncQuery, newAlgoliaSyncQuery: AlgoliaSyncQuery) extends SystemSqlMutaction {
  override def execute: Future[SystemSqlStatementResult[Any]] = {
    val algoliaSyncQueries = TableQuery[AlgoliaSyncQueryTable]

    Future.successful(SystemSqlStatementResult(sqlAction = DBIO.seq({
      val q = for { s <- algoliaSyncQueries if s.id === newAlgoliaSyncQuery.id } yield (s.indexName, s.query, s.isEnabled)
      q.update(newAlgoliaSyncQuery.indexName, newAlgoliaSyncQuery.fragment, newAlgoliaSyncQuery.isEnabled)
    })))
  }

  override def rollback = Some(UpdateAlgoliaSyncQuery(oldAlgoliaSyncQuery, oldAlgoliaSyncQuery).execute)
}
