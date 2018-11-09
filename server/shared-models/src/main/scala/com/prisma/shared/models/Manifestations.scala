package com.prisma.shared.models

import enumeratum.{EnumEntry, Enum => Enumeratum}

object Manifestations {
  case class ModelManifestation(dbName: String)
  case class FieldManifestation(dbName: String)

  sealed trait RelationLinkManifestation
  case class EmbeddedRelationLink(inTableOfModelName: String, referencingColumn: String) extends RelationLinkManifestation
  case class RelationTable(table: String, modelAColumn: String, modelBColumn: String)    extends RelationLinkManifestation
}

sealed trait FieldBehaviour

object FieldBehaviour {
  case class IdBehaviour(strategy: IdStrategy)                 extends FieldBehaviour
  object CreatedAtBehaviour                                    extends FieldBehaviour
  object UpdatedAtBehaviour                                    extends FieldBehaviour
  case class ScalarListBehaviour(strategy: ScalarListStrategy) extends FieldBehaviour

  sealed abstract class IdStrategy(override val entryName: String) extends EnumEntry
  object IdStrategy extends Enumeratum[IdStrategy] {
    override def values = findValues

    object Auto extends IdStrategy("Auto")
    object None extends IdStrategy("None")
  }

  sealed abstract class ScalarListStrategy(override val entryName: String) extends EnumEntry
  object ScalarListStrategy extends Enumeratum[ScalarListStrategy] {
    override def values = findValues

    object Embedded extends ScalarListStrategy("Embedded")
    object Relation extends ScalarListStrategy("Relation")
  }
}

sealed abstract class RelationStrategy(override val entryName: String) extends EnumEntry
object RelationStrategy extends Enumeratum[RelationStrategy] {
  override def values = findValues

  object Auto          extends RelationStrategy("Auto")
  object Embed         extends RelationStrategy("Embed")
  object RelationTable extends RelationStrategy("RelationTable")
}
