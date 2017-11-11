package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.shared.models._
import cool.graph.system.database.tables.SeatTable
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future

case class JoinPendingSeats(client: Client) extends SystemSqlMutaction {

  implicit val mapper = SeatTable.SeatStatusMapper

  override def execute: Future[SystemSqlStatementResult[Any]] = {
    val seats = TableQuery[SeatTable]
    Future.successful(SystemSqlStatementResult(sqlAction = DBIO.seq({
      val q = for { s <- seats if s.email === client.email } yield (s.status, s.clientId)
      q.update(SeatStatus.JOINED, Some(client.id))
    })))
  }

  override def rollback = Some(SystemMutactionNoop().execute)

}
