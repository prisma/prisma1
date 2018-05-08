package com.prisma.shared.models

object Manifestations {
  case class ModelManifestation(dbName: String)
  case class FieldManifestation(dbName: String)

  sealed trait RelationManifestation
  case class InlineRelationManifestation(inTableOfModelId: String, referencingColumn: String)      extends RelationManifestation
  case class RelationTableManifestation(table: String, modelAColumn: String, modelBColumn: String) extends RelationManifestation
}
