package cool.graph.system.database.tables

import org.joda.time.DateTime
import slick.jdbc.MySQLProfile.api._
import com.github.tototoshi.slick.MySQLJodaSupport._

case class RootToken(
    id: String,
    projectId: String,
    token: String,
    name: String,
    created: DateTime
)

class RootTokenTable(tag: Tag) extends Table[RootToken](tag, "PermanentAuthToken") {

  def id        = column[String]("id", O.PrimaryKey)
  def token     = column[String]("token")
  def name      = column[String]("name")
  def created   = column[DateTime]("created")
  def projectId = column[String]("projectId")
  def project =
    foreignKey("systemtoken_projectid_foreign", projectId, Tables.Projects)(_.id)

  def * =
    (id, projectId, token, name, created) <> ((RootToken.apply _).tupled, RootToken.unapply)
}
