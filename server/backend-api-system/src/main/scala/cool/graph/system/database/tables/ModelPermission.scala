package cool.graph.system.database.tables

import cool.graph.shared.models.CustomRule.CustomRule
import cool.graph.shared.models.ModelOperation.ModelOperation
import cool.graph.shared.models.{CustomRule, ModelOperation, UserType}
import cool.graph.shared.models.UserType.UserType
import slick.jdbc.MySQLProfile.api._

case class ModelPermission(
    id: String,
    modelId: String,
    operation: ModelOperation.Value,
    userType: UserType.Value,
    rule: CustomRule.Value,
    ruleName: Option[String],
    ruleGraphQuery: Option[String],
    ruleGraphQueryFilePath: Option[String] = None,
    ruleWebhookUrl: Option[String],
    applyToWholeModel: Boolean,
    description: Option[String],
    isActive: Boolean
)

class ModelPermissionTable(tag: Tag) extends Table[ModelPermission](tag, "ModelPermission") {

  implicit val userTypesMapper = MappedColumnType.base[UserType, String](
    e => e.toString,
    s => UserType.withName(s)
  )

  implicit val operationTypesMapper = MappedColumnType.base[ModelOperation, String](
    e => e.toString,
    s => ModelOperation.withName(s)
  )

  implicit val customRuleTypesMapper =
    MappedColumnType.base[CustomRule, String](
      e => e.toString,
      s => CustomRule.withName(s)
    )

  def id                     = column[String]("id", O.PrimaryKey)
  def operation              = column[ModelOperation]("operation")
  def userType               = column[UserType]("userType")
  def rule                   = column[CustomRule]("rule")
  def ruleName               = column[Option[String]]("ruleName")
  def ruleGraphQuery         = column[Option[String]]("ruleGraphQuery")
  def ruleGraphQueryFilePath = column[Option[String]]("ruleGraphQueryFilePath")
  def ruleWebhookUrl         = column[Option[String]]("ruleWebhookUrl")
  def applyToWholeModel      = column[Boolean]("applyToWholeModel")
  def description            = column[Option[String]]("description")
  def isActive               = column[Boolean]("isActive")

  def modelId = column[String]("modelId")
  def model =
    foreignKey("modelpermission_modelid_foreign", modelId, Tables.Models)(_.id)

  def * =
    (id, modelId, operation, userType, rule, ruleName, ruleGraphQuery, ruleGraphQueryFilePath, ruleWebhookUrl, applyToWholeModel, description, isActive) <> ((ModelPermission.apply _).tupled, ModelPermission.unapply)
}
