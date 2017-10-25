package cool.graph.system.database.tables

import slick.jdbc.MySQLProfile.api._

case class ActionHandlerWebhook(
    id: String,
    actionId: String,
    url: String,
    isAsync: Boolean
)

class ActionHandlerWebhookTable(tag: Tag) extends Table[ActionHandlerWebhook](tag, "ActionHandlerWebhook") {

  def id = column[String]("id", O.PrimaryKey)

  def actionId = column[String]("actionId")
  def action =
    foreignKey("fk_ActionHandlerWebhook_Action_actionId", actionId, Tables.Actions)(_.id)

  def url     = column[String]("url")
  def isAsync = column[Boolean]("isAsync")

  def * =
    (id, actionId, url, isAsync) <> ((ActionHandlerWebhook.apply _).tupled, ActionHandlerWebhook.unapply)
}
