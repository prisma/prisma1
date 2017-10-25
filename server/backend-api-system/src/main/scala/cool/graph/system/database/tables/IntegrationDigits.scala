package cool.graph.system.database.tables

import slick.jdbc.MySQLProfile.api._

case class IntegrationDigits(
    id: String,
    integrationId: String,
    consumerKey: String,
    consumerSecret: String
)

class IntegrationDigitsTable(tag: Tag) extends Table[IntegrationDigits](tag, "AuthProviderDigits") {

  def id             = column[String]("id", O.PrimaryKey)
  def consumerKey    = column[String]("consumerKey")
  def consumerSecret = column[String]("consumerSecret")

  def integrationId = column[String]("integrationId")
  def integration =
    foreignKey("authproviderdigits_integrationid_foreign", integrationId, Tables.Integrations)(_.id)

  def * =
    (id, integrationId, consumerKey, consumerSecret) <> ((IntegrationDigits.apply _).tupled, IntegrationDigits.unapply)
}
