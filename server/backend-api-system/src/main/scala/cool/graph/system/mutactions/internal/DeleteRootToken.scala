package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.shared.models.RootToken
import cool.graph.system.database.tables.{RootTokenTable, RelayIdTable}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future

case class DeleteRootToken(rootToken: RootToken) extends SystemSqlMutaction {
  override def execute: Future[SystemSqlStatementResult[Any]] = {
    val rootTokens = TableQuery[RootTokenTable]
    val relayIds   = TableQuery[RelayIdTable]

    Future.successful(
      SystemSqlStatementResult(
        sqlAction = DBIO.seq(
          rootTokens.filter(_.id === rootToken.id).delete,
          relayIds.filter(_.id === rootToken.id).delete
        )))
  }
}
