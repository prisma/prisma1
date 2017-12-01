package cool.graph.deploy.database.tables

import cool.graph.shared.models.Region
import cool.graph.shared.models.Region.Region
import play.api.libs.json.JsValue
import slick.dbio.Effect.{Read, Write}
import slick.jdbc.MySQLProfile.api._
import slick.sql.{FixedSqlAction, FixedSqlStreamingAction, SqlAction}

case class Project(
    id: String,
    alias: Option[String],
    name: String,
    clientId: String
//    revision: Int,
//    model: JsValue, // schema
//    migrationSteps: JsValue,
//    hasBeenApplied: Boolean
)

class ProjectTable(tag: Tag) extends Table[Project](tag, "Project") {
//  implicit val RegionMapper     = ProjectTable.regionMapper
//  implicit val stringListMapper = MappedColumns.stringListMapper
//  implicit val jsonMapper = MappedColumns.jsonMapper

  def id       = column[String]("id", O.PrimaryKey)
  def alias    = column[Option[String]]("alias")
  def name     = column[String]("name")
  def clientId = column[String]("clientId")
//  def revision       = column[Int]("revision")
//  def model          = column[JsValue]("model")
//  def migrationSteps = column[JsValue]("migrationSteps")
//  def hasBeenApplied = column[Boolean]("hasBeenApplied")

  def client = foreignKey("project_clientid_foreign", clientId, Tables.Clients)(_.id)
  def *      = (id, alias, name, clientId) <> ((Project.apply _).tupled, Project.unapply)
}
//
object ProjectTable {
////  implicit val regionMapper = MappedColumnType.base[Region, String](
////    e => e.toString,
////    s => Region.withName(s)
////  )
//

  def byId(id: String): SqlAction[Option[Project], NoStream, Read] = {
    Tables.Projects.filter { _.id === id }.take(1).result.headOption
  }

  def byIdOrAlias(idOrAlias: String): SqlAction[Option[Project], NoStream, Read] = {
    Tables.Projects
      .filter { t =>
        t.id === idOrAlias || t.alias === idOrAlias
      }
      .take(1)
      .result
      .headOption
  }

  def byIdWithMigration(id: String): SqlAction[Option[(Project, Migration)], NoStream, Read] = {
    val baseQuery = for {
      project   <- Tables.Projects
      migration <- Tables.Migrations
      if migration.projectId === id && project.id === id && migration.hasBeenApplied
    } yield (project, migration)

    baseQuery.sortBy(_._2.revision.desc).take(1).result.headOption
  }

  def byIdOrAliasWithMigration(id: String): SqlAction[Option[(Project, Migration)], NoStream, Read] = {
    val baseQuery = for {
      project   <- Tables.Projects
      migration <- Tables.Migrations
      if project.id === id || project.alias === id
      if migration.projectId === project.id
      if migration.hasBeenApplied
    } yield (project, migration)

    baseQuery.sortBy(_._2.revision.desc).take(1).result.headOption
  }

//  def byIdWithsNextMigration(id: String): SqlAction[Option[(Project, Migration)], NoStream, Read] = {
//    val baseQuery = for {
//      project   <- Tables.Projects
//      migration <- Tables.Migrations
//      if migration.projectId === project.id && !migration.hasBeenApplied
//    } yield (project, migration)
//
//    baseQuery.sortBy(_._2.revision.asc).take(1).result.headOption
//  }
}

//  def currentProjectByIdOrAlias(idOrAlias: String): SqlAction[Option[Project], NoStream, Read] = {
//    val baseQuery = for {
//      project <- Tables.Projects
//      if project.id === idOrAlias || project.alias === idOrAlias
//      //if project.hasBeenApplied
//    } yield project
//    val query = baseQuery.sortBy(_.revision * -1).take(1)
//
//    query.result.headOption
//  }

//  def markAsApplied(id: String, revision: Int): FixedSqlAction[Int, NoStream, Write] = {
//    val baseQuery = for {
//      project <- Tables.Projects
//      if project.id === id
//      if project.revision === revision
//    } yield project
//
//    baseQuery.map(_.hasBeenApplied).update(true)
//  }
//
//  def unappliedMigrations(): FixedSqlStreamingAction[Seq[Project], Project, Read] = {
//    val baseQuery = for {
//      project <- Tables.Projects
//      if !project.hasBeenApplied
//    } yield project
//    val sorted = baseQuery.sortBy(_.revision * -1).take(1) // bug: use lowest unapplied
//    sorted.result
//  }
//}
