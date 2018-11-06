package com.prisma.shared.models

object Manifestations {
  case class ModelManifestation(dbName: String)
  case class FieldManifestation(dbName: String)

  sealed trait RelationManifestation
  case class InlineRelationManifestation(inTableOfModelId: String, referencingColumn: String)      extends RelationManifestation
  case class RelationTableManifestation(table: String, modelAColumn: String, modelBColumn: String) extends RelationManifestation
}

sealed trait FieldBehaviour

object FieldBehaviour {
  case class IdBehaviour(strategy: IdStrategy)                 extends FieldBehaviour
  object CreatedAtBehaviour                                    extends FieldBehaviour
  object UpdatedAtBehaviour                                    extends FieldBehaviour
  case class ScalarListBehaviour(strategy: ScalarListStrategy) extends FieldBehaviour

  sealed trait IdStrategy
  object IdStrategy {
    object Auto extends IdStrategy
    object None extends IdStrategy
  }

  sealed trait ScalarListStrategy
  object ScalarListStrategy {
    object Embedded extends ScalarListStrategy
    object Relation extends ScalarListStrategy
  }
}

sealed trait RelationStrategy
object RelationStrategy {
  object Auto          extends RelationStrategy
  object Embed         extends RelationStrategy
  object RelationTable extends RelationStrategy
}
