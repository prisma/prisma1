package cool.graph.system.database.tables

import cool.graph.shared.models.CustomerSource.CustomerSource
import slick.jdbc.MySQLProfile.api._
import cool.graph.shared.models.CustomerSource
import org.joda.time.DateTime
import com.github.tototoshi.slick.MySQLJodaSupport._

case class Client(
    id: String,
    auth0Id: Option[String],
    isAuth0IdentityProviderEmail: Boolean,
    name: String,
    email: String,
    password: String,
    resetPasswordToken: Option[String],
    source: CustomerSource.Value,
    createdAt: DateTime,
    updatedAt: DateTime
)

class ClientTable(tag: Tag) extends Table[Client](tag, "Client") {
  implicit val sourceMapper = ClientTable.sourceMapper

  def id                           = column[String]("id", O.PrimaryKey)
  def auth0Id                      = column[Option[String]]("auth0Id")
  def isAuth0IdentityProviderEmail = column[Boolean]("isAuth0IdentityProviderEmail")
  def name                         = column[String]("name")
  def email                        = column[String]("email")
  def password                     = column[String]("password")
  def resetPasswordToken           = column[Option[String]]("resetPasswordSecret")
  def source                       = column[CustomerSource]("source")
  def createdAt                    = column[DateTime]("createdAt")
  def updatedAt                    = column[DateTime]("updatedAt")

  def * =
    (id, auth0Id, isAuth0IdentityProviderEmail, name, email, password, resetPasswordToken, source, createdAt, updatedAt) <> ((Client.apply _).tupled, Client.unapply)
}

object ClientTable {
  implicit val sourceMapper = MappedColumnType.base[CustomerSource, String](
    e => e.toString,
    s => CustomerSource.withName(s)
  )
}
