package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.shared.models._
import cool.graph.system.database.tables.{AlgoliaSyncQueryTable, RelayIdTable}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future

case class CreateAlgoliaSyncQuery(searchProviderAlgolia: SearchProviderAlgolia, algoliaSyncQuery: AlgoliaSyncQuery) extends SystemSqlMutaction {
  override def execute: Future[SystemSqlStatementResult[Any]] = {
    Future.successful({
      val algoliaSyncQueries = TableQuery[AlgoliaSyncQueryTable]
      val relayIds           = TableQuery[RelayIdTable]
      SystemSqlStatementResult(
        sqlAction = DBIO.seq(
          algoliaSyncQueries +=
            cool.graph.system.database.tables.AlgoliaSyncQuery(
              algoliaSyncQuery.id,
              algoliaSyncQuery.indexName,
              algoliaSyncQuery.fragment,
              algoliaSyncQuery.model.id,
              searchProviderAlgolia.subTableId,
              algoliaSyncQuery.isEnabled
            ),
          relayIds +=
            cool.graph.system.database.tables.RelayId(algoliaSyncQuery.id, "AlgoliaSyncQuery")
        ))
    })
  }

  override def rollback: Some[Future[SystemSqlStatementResult[Any]]] = Some(DeleteAlgoliaSyncQuery(searchProviderAlgolia, algoliaSyncQuery).execute)
}
