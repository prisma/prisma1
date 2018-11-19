package com.prisma.shared.models

import com.prisma.shared.models.ConnectorCapability._
import enumeratum.{EnumEntry, Enum => Enumeratum}

sealed trait ConnectorCapability extends EnumEntry

object ConnectorCapability extends Enumeratum[ConnectorCapability] {
  val values = findValues

  sealed trait ScalarListsCapability     extends ConnectorCapability
  object ScalarListsCapability           extends ScalarListsCapability
  object EmbeddedScalarListsCapability   extends ScalarListsCapability
  object NonEmbeddedScalarListCapability extends ScalarListsCapability

  object NodeQueryCapability extends ConnectorCapability

  object EmbeddedTypesCapability       extends ConnectorCapability
  object JoinRelationsFilterCapability extends ConnectorCapability

  object ImportExportCapability extends ConnectorCapability

  object TransactionalExecutionCapability extends ConnectorCapability

  object SupportsExistingDatabasesCapability extends ConnectorCapability
  object MigrationsCapability                extends ConnectorCapability
  object LegacyDataModelCapability           extends ConnectorCapability
  object IntrospectionCapability             extends ConnectorCapability
  object JoinRelationLinksCapability         extends ConnectorCapability // the ability to join using relation links
  object RelationLinkListCapability          extends ConnectorCapability // relation links can be stored inline in a node in a list
  object RelationLinkTableCapability         extends ConnectorCapability // relation links are stored in a table
  // IntrospectionCapability
  // RawAccessCapability

  sealed trait IdCapability extends ConnectorCapability
  object IntIdCapability    extends IdCapability
  object UuidIdCapability   extends IdCapability
}

object ConnectorCapabilities {
  val mysql = Set.empty
  def mongo = Set.empty

  def postgres(isActive: Boolean) = {
    val common: Set[ConnectorCapability] = Set(
      LegacyDataModelCapability,
      TransactionalExecutionCapability,
      JoinRelationsFilterCapability,
      JoinRelationLinksCapability,
      IntrospectionCapability,
      RelationLinkTableCapability,
      IntIdCapability,
      UuidIdCapability
    )
    if (isActive) {
      common ++ Set(MigrationsCapability, NonEmbeddedScalarListCapability, NodeQueryCapability, ImportExportCapability)
    } else {
      common ++ Set(SupportsExistingDatabasesCapability)
    }
  }
}
