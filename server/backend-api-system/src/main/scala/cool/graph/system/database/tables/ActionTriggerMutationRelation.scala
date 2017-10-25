package cool.graph.system.database.tables

import slick.jdbc.MySQLProfile.api._

import cool.graph.shared.models.ActionTriggerMutationRelationMutationType

case class ActionTriggerMutationRelation(
    id: String,
    actionId: String,
    relationId: String,
    mutationType: ActionTriggerMutationRelationMutationType.Value,
    fragment: String
)

class ActionTriggerMutationRelationTable(tag: Tag) extends Table[ActionTriggerMutationRelation](tag, "ActionTriggerMutationRelation") {

  implicit val actionTriggerMutationRelationMutationTypeMapper =
    MappedColumnType
      .base[ActionTriggerMutationRelationMutationType.Value, String](
        e => e.toString,
        s => ActionTriggerMutationRelationMutationType.withName(s)
      )

  def id = column[String]("id", O.PrimaryKey)

  def mutationType =
    column[ActionTriggerMutationRelationMutationType.Value]("mutationType")

  def actionId = column[String]("actionId")
  def action =
    foreignKey("fk_ActionTriggerMutationRelationMutationType_Action_actionId", actionId, Tables.Actions)(_.id)

  def relationId = column[String]("relationId")
  def relation =
    foreignKey("fk_ActionTriggerMutationRelationMutationType_Relation_relationId", relationId, Tables.Relations)(_.id)

  def fragment = column[String]("fragment")

  def * =
    (id, actionId, relationId, mutationType, fragment) <> ((ActionTriggerMutationRelation.apply _).tupled, ActionTriggerMutationRelation.unapply)
}
