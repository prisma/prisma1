package cool.graph.system.database.tables

import cool.graph.shared.models.UserType
import cool.graph.shared.models.UserType.UserType
import slick.jdbc.MySQLProfile.api._

case class Permission(
    id: String,
    description: Option[String],
    allowRead: Boolean,
    allowCreate: Boolean,
    allowUpdate: Boolean,
    allowDelete: Boolean,
    userType: UserType.Value,
    userPath: Option[String],
    fieldId: String
)

class PermissionTable(tag: Tag) extends Table[Permission](tag, "Permission") {

  implicit val userTypesMapper = MappedColumnType.base[UserType, String](
    e => e.toString,
    s => UserType.withName(s)
  )

  def id          = column[String]("id", O.PrimaryKey)
  def description = column[Option[String]]("comment") // TODO adjust db naming
  def allowRead   = column[Boolean]("allowRead")
  def allowCreate = column[Boolean]("allowCreate")
  def allowUpdate = column[Boolean]("allowUpdate")
  def allowDelete = column[Boolean]("allowDelete")
  def userType    = column[UserType]("userType")
  def userPath    = column[Option[String]]("userPath")

  def fieldId = column[String]("fieldId")
  def field =
    foreignKey("permission_fieldid_foreign", fieldId, Tables.Fields)(_.id)

  def * =
    (id, description, allowRead, allowCreate, allowUpdate, allowDelete, userType, userPath, fieldId) <> ((Permission.apply _).tupled, Permission.unapply)
}
