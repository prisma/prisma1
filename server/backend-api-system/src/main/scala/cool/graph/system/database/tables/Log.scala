package cool.graph.system.database.tables

import com.github.tototoshi.slick.MySQLJodaSupport._
import cool.graph.shared.models.LogStatus
import org.joda.time.DateTime
import slick.jdbc.MySQLProfile.api._

case class Log(
    id: String,
    projectId: String,
    functionId: String,
    requestId: Option[String],
    status: LogStatus.Value,
    duration: Int,
    timestamp: DateTime,
    message: String
)

class LogTable(tag: Tag) extends Table[Log](tag, "Log") {

  implicit val statusMapper =
    MappedColumnType.base[LogStatus.Value, String](
      e => e.toString,
      s => LogStatus.withName(s)
    )

  def id         = column[String]("id", O.PrimaryKey)
  def projectId  = column[String]("projectId")
  def functionId = column[String]("functionId")
  def requestId  = column[Option[String]]("requestId")
  def status     = column[LogStatus.Value]("status")
  def duration   = column[Int]("duration")
  def timestamp  = column[DateTime]("timestamp")
  def message    = column[String]("message")

  def * =
    (id, projectId, functionId, requestId, status, duration, timestamp, message) <> ((Log.apply _).tupled, Log.unapply)
}
