package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.shared.models.{Client, Project, Seat}
import cool.graph.system.database.tables.{RelayIdTable, SeatTable}
import scaldi.Injector
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.MySQLProfile.backend.DatabaseDef
import slick.lifted.TableQuery

import scala.concurrent.Future

case class DeleteSeat(client: Client, project: Project, seat: Seat, internalDatabase: DatabaseDef)(implicit inj: Injector) extends SystemSqlMutaction {
  override def execute: Future[SystemSqlStatementResult[Any]] = {
    val seats    = TableQuery[SeatTable]
    val relayIds = TableQuery[RelayIdTable]

    Future.successful(SystemSqlStatementResult(sqlAction = DBIO.seq(seats.filter(_.id === seat.id).delete, relayIds.filter(_.id === seat.id).delete)))
  }

  override def rollback = Some(CreateSeat(client, project, seat, internalDatabase).execute)

}
