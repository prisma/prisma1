package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.shared.models.Client
import cool.graph.system.database.tables.{ClientTable, RelayIdTable}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future

case class DeleteClient(client: Client) extends SystemSqlMutaction {
  override def execute: Future[SystemSqlStatementResult[Any]] = {
    val clients  = TableQuery[ClientTable]
    val relayIds = TableQuery[RelayIdTable]

    Future.successful(SystemSqlStatementResult(sqlAction = DBIO.seq(clients.filter(_.id === client.id).delete, relayIds.filter(_.id === client.id).delete)))
  }

  override def rollback = Some(CreateClient(client).execute)

}
