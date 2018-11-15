package com.prisma.shared.models

import enumeratum.{EnumEntry, Enum => Enumeratum}

sealed trait ConnectorCapability extends EnumEntry

object ApiConnectorCapability extends Enumeratum[ConnectorCapability] {
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
  object JoinRelationsCapability             extends ConnectorCapability // the ability to join using relation links
  object RelationLinkListCapability          extends ConnectorCapability // relation links can be stored inline in a node in a list
  object RelationLinkTableCapability         extends ConnectorCapability // relation links are stored in a table
  // IntrospectionCapability
  // RawAccessCapability
}
