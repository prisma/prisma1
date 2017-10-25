package cool.graph.system.database.tables

import slick.jdbc.MySQLProfile.api._

case class SearchProviderAlgolia(
    id: String,
    integrationId: String,
    applicationId: String,
    apiKey: String
)

class SearchProviderAlgoliaTable(tag: Tag) extends Table[SearchProviderAlgolia](tag, "SearchProviderAlgolia") {

  def id            = column[String]("id", O.PrimaryKey)
  def applicationId = column[String]("applicationId")
  def apiKey        = column[String]("apiKey")

  def integrationId = column[String]("integrationId")
  def integration =
    foreignKey("searchprovideralgolia_integrationid_foreign", integrationId, Tables.Integrations)(_.id)

  def * =
    (id, integrationId, applicationId, apiKey) <> ((SearchProviderAlgolia.apply _).tupled, SearchProviderAlgolia.unapply)
}
