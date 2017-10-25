package cool.graph.system.database.tables

import slick.jdbc.MySQLProfile.api._

case class IntegrationAuth0(
    id: String,
    integrationId: String,
    clientId: String,
    clientSecret: String,
    domain: String
)

class IntegrationAuth0Table(tag: Tag) extends Table[IntegrationAuth0](tag, "AuthProviderAuth0") {

  def id           = column[String]("id", O.PrimaryKey)
  def clientId     = column[String]("clientId")
  def clientSecret = column[String]("clientSecret")
  def domain       = column[String]("domain")

  def integrationId = column[String]("integrationId")
  def integration =
    foreignKey("authproviderauth0_integrationid_foreign", integrationId, Tables.Integrations)(_.id)

  def * =
    (id, integrationId, clientId, clientSecret, domain) <> ((IntegrationAuth0.apply _).tupled, IntegrationAuth0.unapply)
}
