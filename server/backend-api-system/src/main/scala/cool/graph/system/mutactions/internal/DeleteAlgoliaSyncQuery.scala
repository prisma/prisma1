package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.shared.models.{AlgoliaSyncQuery, SearchProviderAlgolia}
import cool.graph.system.database.tables.{AlgoliaSyncQueryTable, RelayIdTable}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future

case class DeleteAlgoliaSyncQuery(searchProviderAlgolia: SearchProviderAlgolia, algoliaSyncQuery: AlgoliaSyncQuery) extends SystemSqlMutaction {
  override def execute: Future[SystemSqlStatementResult[Any]] = {
    val algoliaSyncQueries = TableQuery[AlgoliaSyncQueryTable]
    val relayIds           = TableQuery[RelayIdTable]

    Future.successful(
      SystemSqlStatementResult(
        sqlAction = DBIO.seq(algoliaSyncQueries.filter(_.id === algoliaSyncQuery.id).delete, relayIds.filter(_.id === algoliaSyncQuery.id).delete)))
  }

  override def rollback = Some(CreateAlgoliaSyncQuery(searchProviderAlgolia, algoliaSyncQuery).execute)

}
