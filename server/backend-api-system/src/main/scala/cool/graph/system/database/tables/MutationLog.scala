package cool.graph.system.database.tables

import com.github.tototoshi.slick.MySQLJodaSupport._
import cool.graph.shared.models.MutationLogStatus
import cool.graph.shared.models.MutationLogStatus.MutationLogStatus
import org.joda.time.DateTime
import slick.jdbc.MySQLProfile.api._

case class MutationLog(
    id: String,
    name: String,
    status: MutationLogStatus.Value,
    failedMutaction: Option[String],
    input: String,
    startedAt: DateTime,
    finishedAt: Option[DateTime],
    projectId: Option[String],
    clientId: Option[String]
)

class MutationLogTable(tag: Tag) extends Table[MutationLog](tag, "MutationLog") {
  implicit val mutationLogStatusMapper = MutationLog.mutationLogStatusMapper

  def id              = column[String]("id", O.PrimaryKey)
  def name            = column[String]("name")
  def status          = column[MutationLogStatus]("status")
  def failedMutaction = column[Option[String]]("failedMutaction")
  def input           = column[String]("input")
  def startedAt       = column[DateTime]("startedAt")
  def finishedAt      = column[Option[DateTime]]("finishedAt")

  def projectId = column[Option[String]]("projectId")
  def project =
    foreignKey("mutationlog_projectid_foreign", projectId, Tables.Projects)(_.id.?)

  def clientId = column[Option[String]]("clientId")
  def client =
    foreignKey("mutationlog_clientid_foreign", clientId, Tables.Clients)(_.id.?)

  def * =
    (id, name, status, failedMutaction, input, startedAt, finishedAt, projectId, clientId) <> ((MutationLog.apply _).tupled, MutationLog.unapply)
}

object MutationLog {
  implicit val mutationLogStatusMapper = MappedColumnType.base[MutationLogStatus, String](
    e => e.toString,
    s => MutationLogStatus.withName(s)
  )
}
