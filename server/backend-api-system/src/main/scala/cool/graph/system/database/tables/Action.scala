package cool.graph.system.database.tables

import slick.jdbc.MySQLProfile.api._

import cool.graph.shared.models.ActionTriggerType
import cool.graph.shared.models.ActionHandlerType

case class Action(
    id: String,
    projectId: String,
    isActive: Boolean,
    triggerType: ActionTriggerType.Value,
    handlerType: ActionHandlerType.Value,
    description: Option[String]
)

class ActionTable(tag: Tag) extends Table[Action](tag, "Action") {

  implicit val actionTriggerTypeMapper =
    MappedColumnType.base[ActionTriggerType.Value, String](
      e => e.toString,
      s => ActionTriggerType.withName(s)
    )

  implicit val actionHandlerTypeMapper =
    MappedColumnType.base[ActionHandlerType.Value, String](
      e => e.toString,
      s => ActionHandlerType.withName(s)
    )

  def id       = column[String]("id", O.PrimaryKey)
  def isActive = column[Boolean]("isActive")

  def triggerType =
    column[ActionTriggerType.Value]("triggerType")

  def handlerType =
    column[ActionHandlerType.Value]("handlerType")

  def projectId = column[String]("projectId")
  def project =
    foreignKey("fk_Action_Project_projectId", projectId, Tables.Projects)(_.id)

  def description = column[Option[String]]("description")

  def * =
    (id, projectId, isActive, triggerType, handlerType, description) <> ((Action.apply _).tupled, Action.unapply)
}
