package cool.graph.system.database.tables

import slick.jdbc.MySQLProfile.api._

case class AlgoliaSyncQuery(
    id: String,
    indexName: String,
    query: String,
    modelId: String,
    searchProviderAlgoliaId: String,
    isEnabled: Boolean
)

class AlgoliaSyncQueryTable(tag: Tag) extends Table[AlgoliaSyncQuery](tag, "AlgoliaSyncQuery") {

  def id                      = column[String]("id", O.PrimaryKey)
  def modelId                 = column[String]("modelId")
  def searchProviderAlgoliaId = column[String]("searchProviderAlgoliaId")
  def indexName               = column[String]("indexName")
  def query                   = column[String]("query")
  def isEnabled               = column[Boolean]("isEnabled")

  def model =
    foreignKey("algoliasyncquery_modelid_foreign", modelId, Tables.Models)(_.id)

  def searchProviderAlgolia =
    foreignKey("algoliasyncquery_searchprovideralgoliaid_foreign", searchProviderAlgoliaId, Tables.SearchProviderAlgolias)(_.id)

  def * =
    (id, indexName, query, modelId, searchProviderAlgoliaId, isEnabled) <> ((AlgoliaSyncQuery.apply _).tupled, AlgoliaSyncQuery.unapply)
}
