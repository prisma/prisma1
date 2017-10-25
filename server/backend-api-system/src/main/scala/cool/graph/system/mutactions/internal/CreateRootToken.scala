package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.shared.models.RootToken
import cool.graph.system.database.tables.{RootTokenTable, RelayIdTable}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future

case class CreateRootToken(projectId: String, rootToken: RootToken) extends SystemSqlMutaction {
  override def execute: Future[SystemSqlStatementResult[Any]] = {
    val rootTokens = TableQuery[RootTokenTable]
    val relayIds   = TableQuery[RelayIdTable]

    Future.successful(
      SystemSqlStatementResult(
        sqlAction = DBIO.seq(
          rootTokens += cool.graph.system.database.tables
            .RootToken(id = rootToken.id, projectId = projectId, name = rootToken.name, token = rootToken.token, created = rootToken.created),
          relayIds += cool.graph.system.database.tables
            .RelayId(rootToken.id, "PermanentAuthToken")
        )))
  }

  override def rollback = Some(DeleteRootToken(rootToken).execute)
}
