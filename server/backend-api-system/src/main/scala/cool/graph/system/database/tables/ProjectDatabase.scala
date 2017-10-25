package cool.graph.system.database.tables

import cool.graph.shared.models.Region.Region
import slick.jdbc.MySQLProfile.api._

case class ProjectDatabase(id: String, region: Region, name: String, isDefaultForRegion: Boolean)

class ProjectDatabaseTable(tag: Tag) extends Table[ProjectDatabase](tag, "ProjectDatabase") {
  implicit val RegionMapper = ProjectTable.regionMapper

  def id                 = column[String]("id", O.PrimaryKey)
  def region             = column[Region]("region")
  def name               = column[String]("name")
  def isDefaultForRegion = column[Boolean]("isDefaultForRegion")

  def * = (id, region, name, isDefaultForRegion) <> ((ProjectDatabase.apply _).tupled, ProjectDatabase.unapply)
}
