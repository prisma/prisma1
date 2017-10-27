package cool.graph.system.database.tables

import slick.jdbc.MySQLProfile.api._

case class PackageDefinition(
    id: String,
    name: String,
    projectId: String,
    definition: String,
    formatVersion: Int
)

class PackageDefinitionTable(tag: Tag) extends Table[PackageDefinition](tag, "PackageDefinition") {
  def id            = column[String]("id", O.PrimaryKey)
  def name          = column[String]("name")
  def definition    = column[String]("definition")
  def formatVersion = column[Int]("formatVersion")

  def projectId = column[String]("projectId")
  def project =
    foreignKey("packagedefinition_projectid_foreign", projectId, Tables.Projects)(_.id)

  def * =
    (id, name, projectId, definition, formatVersion) <> ((PackageDefinition.apply _).tupled, PackageDefinition.unapply)
}
