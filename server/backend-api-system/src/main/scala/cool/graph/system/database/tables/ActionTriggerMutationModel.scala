package cool.graph.system.database.tables

import slick.jdbc.MySQLProfile.api._

import cool.graph.shared.models.ActionTriggerMutationModelMutationType

case class ActionTriggerMutationModel(
    id: String,
    actionId: String,
    modelId: String,
    mutationType: ActionTriggerMutationModelMutationType.Value,
    fragment: String
)

class ActionTriggerMutationModelTable(tag: Tag) extends Table[ActionTriggerMutationModel](tag, "ActionTriggerMutationModel") {

  implicit val actionTriggerMutationModelMutationTypeMapper = MappedColumnType
    .base[ActionTriggerMutationModelMutationType.Value, String](
      e => e.toString,
      s => ActionTriggerMutationModelMutationType.withName(s)
    )

  def id = column[String]("id", O.PrimaryKey)

  def mutationType =
    column[ActionTriggerMutationModelMutationType.Value]("mutationType")

  def actionId = column[String]("actionId")
  def action =
    foreignKey("fk_ActionTriggerMutationModelMutationType_Action_actionId", actionId, Tables.Actions)(_.id)

  def modelId = column[String]("modelId")
  def model =
    foreignKey("fk_ActionTriggerMutationModelMutationType_Model_modelId", modelId, Tables.Models)(_.id)

  def fragment = column[String]("fragment")

  def * =
    (id, actionId, modelId, mutationType, fragment) <> ((ActionTriggerMutationModel.apply _).tupled, ActionTriggerMutationModel.unapply)
}
