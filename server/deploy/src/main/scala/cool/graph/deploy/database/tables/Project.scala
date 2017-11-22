package cool.graph.deploy.database.tables

import cool.graph.shared.models.Region
import cool.graph.shared.models.Region.Region
import play.api.libs.json.JsValue
import slick.dbio.Effect.Read
import slick.jdbc.MySQLProfile.api._
import slick.sql.SqlAction

case class Project(
    id: String,
    alias: Option[String],
    name: String,
    revision: Int,
    clientId: String,
    model: JsValue,
    migrationSteps: JsValue
)

class ProjectTable(tag: Tag) extends Table[Project](tag, "Project") {
  implicit val RegionMapper     = ProjectTable.regionMapper
  implicit val stringListMapper = MappedColumns.stringListMapper
  implicit val jsonMapper       = MappedColumns.jsonMapper

  def id             = column[String]("id", O.PrimaryKey)
  def alias          = column[Option[String]]("alias")
  def name           = column[String]("name")
  def revision       = column[Int]("revision")
  def model          = column[JsValue]("model")
  def migrationSteps = column[JsValue]("migrationSteps")

  def clientId = column[String]("clientId")
  def client   = foreignKey("project_clientid_foreign", clientId, Tables.Clients)(_.id)

  def * =
    (id, alias, name, revision, clientId, model, migrationSteps) <>
      ((Project.apply _).tupled, Project.unapply)
}

object ProjectTable {
  implicit val regionMapper = MappedColumnType.base[Region, String](
    e => e.toString,
    s => Region.withName(s)
  )

  def currentProjectById(id: String): SqlAction[Option[Project], NoStream, Read] = {
    val baseQuery = for {
      project <- Tables.Projects
      if project.id === id
    } yield project
    val query = baseQuery.sortBy(_.revision * -1).take(1)

    query.result.headOption
  }
}
