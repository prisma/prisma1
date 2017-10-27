package cool.graph.system.database.tables

import slick.jdbc.MySQLProfile.api._

case class FeatureToggle(
    id: String,
    projectId: String,
    name: String,
    isEnabled: Boolean
)

class FeatureToggleTable(tag: Tag) extends Table[FeatureToggle](tag, "FeatureToggle") {
  def id        = column[String]("id", O.PrimaryKey)
  def projectId = column[String]("projectId")
  def name      = column[String]("name")
  def isEnabled = column[Boolean]("isEnabled")

  def project = foreignKey("featuretoggle_enum_projectid_foreign", projectId, Tables.Projects)(_.id)

  def * = (id, projectId, name, isEnabled) <> ((FeatureToggle.apply _).tupled, FeatureToggle.unapply)
}
