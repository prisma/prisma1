package cool.graph.system.database.tables

import cool.graph.Types.Id
import slick.jdbc.MySQLProfile.api._

case class Model(
    id: String,
    name: String,
    description: Option[String],
    isSystem: Boolean,
    projectId: String,
    fieldPositions: Seq[Id]
)

class ModelTable(tag: Tag) extends Table[Model](tag, "Model") {
  implicit val stringListMapper = MappedColumns.stringListMapper

  def id             = column[String]("id", O.PrimaryKey)
  def name           = column[String]("modelName")
  def description    = column[Option[String]]("description")
  def isSystem       = column[Boolean]("isSystem")
  def fieldPositions = column[Seq[String]]("fieldPositions")

  def projectId = column[String]("projectId")
  def project   = foreignKey("model_projectid_modelname_uniq", projectId, Tables.Projects)(_.id)

  def * = (id, name, description, isSystem, projectId, fieldPositions) <> ((Model.apply _).tupled, Model.unapply)
}
