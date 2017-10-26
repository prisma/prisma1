package cool.graph.system.database.tables

import cool.graph.shared.models.CustomRule.CustomRule
import cool.graph.shared.models.UserType.UserType
import cool.graph.shared.models.{CustomRule, UserType}
import slick.jdbc.MySQLProfile.api._

case class RelationPermission(
    id: String,
    relationId: String,
    connect: Boolean,
    disconnect: Boolean,
    userType: UserType.Value,
    rule: CustomRule.Value,
    ruleName: Option[String],
    ruleGraphQuery: Option[String],
    ruleGraphQueryFilePath: Option[String],
    ruleWebhookUrl: Option[String],
    description: Option[String],
    isActive: Boolean
)

class RelationPermissionTable(tag: Tag) extends Table[RelationPermission](tag, "RelationPermission") {

  implicit val userTypesMapper = MappedColumnType.base[UserType, String](
    e => e.toString,
    s => UserType.withName(s)
  )

  implicit val customRuleTypesMapper =
    MappedColumnType.base[CustomRule, String](
      e => e.toString,
      s => CustomRule.withName(s)
    )

  def id                     = column[String]("id", O.PrimaryKey)
  def connect                = column[Boolean]("connect")
  def disconnect             = column[Boolean]("disconnect")
  def userType               = column[UserType]("userType")
  def rule                   = column[CustomRule]("rule")
  def ruleName               = column[Option[String]]("ruleName")
  def ruleGraphQuery         = column[Option[String]]("ruleGraphQuery")
  def ruleGraphQueryFilePath = column[Option[String]]("ruleGraphQueryFilePath")
  def ruleWebhookUrl         = column[Option[String]]("ruleWebhookUrl")
  def description            = column[Option[String]]("description")
  def isActive               = column[Boolean]("isActive")

  def relationId = column[String]("relationId")
  def relation =
    foreignKey("relationpermission_relationid_foreign", relationId, Tables.Relations)(_.id)

  def * =
    (id, relationId, connect, disconnect, userType, rule, ruleName, ruleGraphQuery, ruleGraphQueryFilePath, ruleWebhookUrl, description, isActive) <> ((RelationPermission.apply _).tupled, RelationPermission.unapply)
}
