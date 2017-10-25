package cool.graph.system.database.tables

import cool.graph.shared.models.SeatStatus
import cool.graph.shared.models.SeatStatus.SeatStatus
import slick.jdbc.MySQLProfile.api._

case class Seat(
    id: String,
    status: SeatStatus,
    email: String,
    projectId: String,
    clientId: Option[String]
)

class SeatTable(tag: Tag) extends Table[Seat](tag, "Seat") {

  implicit val mapper = SeatTable.SeatStatusMapper

  def id     = column[String]("id", O.PrimaryKey)
  def status = column[SeatStatus]("status")

  def email = column[String]("email")

  def projectId = column[String]("projectId")
  def project =
    foreignKey("seat_projectid_foreign", projectId, Tables.Projects)(_.id)

  def clientId = column[Option[String]]("clientId")
  def client =
    foreignKey("seat_clientid_foreign", clientId, Tables.Clients)(_.id.?)

  def * =
    (id, status, email, projectId, clientId) <> ((Seat.apply _).tupled, Seat.unapply)
}

object SeatTable {
  implicit val SeatStatusMapper =
    MappedColumnType.base[SeatStatus.Value, String](
      e => e.toString,
      s => SeatStatus.withName(s)
    )
}
