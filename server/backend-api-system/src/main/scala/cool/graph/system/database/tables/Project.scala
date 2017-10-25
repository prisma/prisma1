package cool.graph.system.database.tables

import cool.graph.shared.models.Region
import cool.graph.shared.models.Region.Region
import slick.jdbc.MySQLProfile.api._

case class Project(
    id: String,
    alias: Option[String],
    name: String,
    revision: Int,
    webhookUrl: Option[String],
    clientId: String,
    allowQueries: Boolean,
    allowMutations: Boolean,
    typePositions: Seq[String],
    projectDatabaseId: String,
    isEjected: Boolean,
    hasGlobalStarPermission: Boolean
)

class ProjectTable(tag: Tag) extends Table[Project](tag, "Project") {
  implicit val RegionMapper     = ProjectTable.regionMapper
  implicit val stringListMapper = MappedColumns.stringListMapper

  def id                      = column[String]("id", O.PrimaryKey)
  def alias                   = column[Option[String]]("alias")
  def name                    = column[String]("name")
  def revision                = column[Int]("revision")
  def webhookUrl              = column[Option[String]]("webhookUrl")
  def allowQueries            = column[Boolean]("allowQueries")
  def allowMutations          = column[Boolean]("allowMutations")
  def typePositions           = column[Seq[String]]("typePositions")
  def isEjected               = column[Boolean]("isEjected")
  def hasGlobalStarPermission = column[Boolean]("hasGlobalStarPermission")

  def clientId = column[String]("clientId")
  def client   = foreignKey("project_clientid_foreign", clientId, Tables.Clients)(_.id)

  def projectDatabaseId = column[String]("projectDatabaseId")
  def projectDatabase   = foreignKey("project_databaseid_foreign", projectDatabaseId, Tables.ProjectDatabases)(_.id)

  def * =
    (id, alias, name, revision, webhookUrl, clientId, allowQueries, allowMutations, typePositions, projectDatabaseId, isEjected, hasGlobalStarPermission) <>
      ((Project.apply _).tupled, Project.unapply)
}

object ProjectTable {
  implicit val regionMapper = MappedColumnType.base[Region, String](
    e => e.toString,
    s => Region.withName(s)
  )
}
