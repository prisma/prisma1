package cool.graph.system.database.tables

import slick.jdbc.MySQLProfile.api._

case class ModelPermissionField(
    id: String,
    modelPermissionId: String,
    fieldId: String
)

class ModelPermissionFieldTable(tag: Tag) extends Table[ModelPermissionField](tag, "ModelPermissionField") {

  def id = column[String]("id", O.PrimaryKey)

  def modelPermissionId = column[String]("modelPermissionId")
  def modelPermission =
    foreignKey("modelpermissionfield_modelpermissionid_foreign", modelPermissionId, Tables.ModelPermissions)(_.id)

  def fieldId = column[String]("fieldId")
  def field =
    foreignKey("modelpermissionfield_fieldid_foreign", fieldId, Tables.Fields)(_.id)

  def * =
    (id, modelPermissionId, fieldId) <> ((ModelPermissionField.apply _).tupled, ModelPermissionField.unapply)
}
