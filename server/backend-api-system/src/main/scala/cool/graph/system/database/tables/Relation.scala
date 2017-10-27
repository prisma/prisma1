package cool.graph.system.database.tables

import slick.jdbc.MySQLProfile.api._

case class Relation(
    id: String,
    projectId: String,
    name: String,
    description: Option[String],
    modelAId: String,
    modelBId: String
)

class RelationTable(tag: Tag) extends Table[Relation](tag, "Relation") {
  def id = column[String]("id", O.PrimaryKey)

  def projectId = column[String]("projectId")
  def project =
    foreignKey("relation_projectid_foreign", projectId, Tables.Projects)(_.id)

  def name = column[String]("name")

  def description = column[Option[String]]("description")

  def modelAId = column[String]("modelAId")
  def modelA =
    foreignKey("relation_modelaid_foreign", modelAId, Tables.Models)(_.id)

  def modelBId = column[String]("modelBId")
  def modelB =
    foreignKey("relation_modelbid_foreign", modelBId, Tables.Models)(_.id)

  def * =
    (id, projectId, name, description, modelAId, modelBId) <> ((Relation.apply _).tupled, Relation.unapply)
}
