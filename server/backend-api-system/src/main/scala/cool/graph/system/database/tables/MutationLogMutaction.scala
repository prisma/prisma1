package cool.graph.system.database.tables

import slick.jdbc.MySQLProfile.api._
import cool.graph.shared.models.{MutationLogStatus}
import org.joda.time.DateTime
import com.github.tototoshi.slick.MySQLJodaSupport._
import cool.graph.shared.models.MutationLogStatus.MutationLogStatus

case class MutationLogMutaction(
    id: String,
    name: String,
    index: Int,
    status: MutationLogStatus.Value,
    input: String,
    finishedAt: Option[DateTime],
    error: Option[String],
    rollbackError: Option[String],
    mutationLogId: String
)

class MutationLogMutactionTable(tag: Tag) extends Table[MutationLogMutaction](tag, "MutationLogMutaction") {
  implicit val mutationLogStatusMapper = MutationLog.mutationLogStatusMapper

  def id            = column[String]("id", O.PrimaryKey)
  def name          = column[String]("name")
  def index         = column[Int]("index")
  def status        = column[MutationLogStatus.Value]("status")
  def input         = column[String]("input")
  def finishedAt    = column[Option[DateTime]]("finishedAt")
  def error         = column[Option[String]]("error")
  def rollbackError = column[Option[String]]("rollbackError")

  def mutationLogId = column[String]("mutationLogId")
  def mutationLog =
    foreignKey("mutationlogmutaction_mutationlogid_foreign", mutationLogId, Tables.MutationLogs)(_.id)

  def * =
    (id, name, index, status, input, finishedAt, error, rollbackError, mutationLogId) <> ((MutationLogMutaction.apply _).tupled, MutationLogMutaction.unapply)
}
