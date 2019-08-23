package util

import enumeratum.{EnumEntry, Enum => Enumeratum}

sealed trait ConnectorCapability extends EnumEntry

object ConnectorCapability extends Enumeratum[ConnectorCapability] {
  val values = findValues

  sealed trait ScalarListsCapability     extends ConnectorCapability
  object ScalarListsCapability           extends ScalarListsCapability
  object EmbeddedScalarListsCapability   extends ScalarListsCapability
  object NonEmbeddedScalarListCapability extends ScalarListsCapability

  object NodeQueryCapability extends ConnectorCapability

  object EmbeddedTypesCapability          extends ConnectorCapability
  object JoinRelationsFilterCapability    extends ConnectorCapability
  object TransactionalExecutionCapability extends ConnectorCapability

  object SupportsExistingDatabasesCapability extends ConnectorCapability
  object MigrationsCapability                extends ConnectorCapability
  object RawAccessCapability                 extends ConnectorCapability
  object IntrospectionCapability             extends ConnectorCapability
  object JoinRelationLinksCapability         extends ConnectorCapability // the ability to join using relation links
  object MongoJoinRelationLinksCapability    extends ConnectorCapability // does not allow NOT/OR and only _some on manyrelations
  object RelationLinkListCapability          extends ConnectorCapability // relation links can be stored inline in a node in a list
  object RelationLinkTableCapability         extends ConnectorCapability // relation links are stored in a table

  sealed trait IdCapability   extends ConnectorCapability
  object IntIdCapability      extends IdCapability
  object UuidIdCapability     extends IdCapability
  object IdSequenceCapability extends IdCapability

  object Prisma2Capability extends ConnectorCapability
}

case class ConnectorCapabilities(capabilities: Set[ConnectorCapability]) {
  import ConnectorCapability._

  def has(capability: ConnectorCapability): Boolean = capability match {
    case ScalarListsCapability => capabilities.contains(EmbeddedScalarListsCapability) || capabilities.contains(NonEmbeddedScalarListCapability)
    case x                     => capabilities.contains(x)
  }
  def hasNot(capability: ConnectorCapability): Boolean = !has(capability)

  def supportsScalarLists = capabilities.exists(_.isInstanceOf[ScalarListsCapability])

  def isMongo: Boolean = has(EmbeddedTypesCapability)
}

object ConnectorCapabilities {
  import ConnectorCapability._

  val empty: ConnectorCapabilities                                     = ConnectorCapabilities(Set.empty[ConnectorCapability])
  def apply(capabilities: ConnectorCapability*): ConnectorCapabilities = ConnectorCapabilities(Set(capabilities: _*))

  lazy val sqlite: ConnectorCapabilities = {
    ConnectorCapabilities(sqlShared)
  }

  lazy val postgres: ConnectorCapabilities = {
    val capas = sqlShared ++ Set(UuidIdCapability)
    ConnectorCapabilities(capas)
  }

  lazy val mysql: ConnectorCapabilities = {
    ConnectorCapabilities(sqlShared)
  }

  private lazy val sqlShared: Set[ConnectorCapability] = {
    Set(
      TransactionalExecutionCapability,
      JoinRelationsFilterCapability,
      JoinRelationLinksCapability,
      RelationLinkTableCapability,
      MigrationsCapability,
      NonEmbeddedScalarListCapability,
//      NodeQueryCapability,
      IntrospectionCapability,
      SupportsExistingDatabasesCapability,
      IntIdCapability,
      NonEmbeddedScalarListCapability,
      RawAccessCapability,
      IdSequenceCapability,
      Prisma2Capability
    )
  }

  val mongo: ConnectorCapabilities = {
    val common = Set(
      NodeQueryCapability,
      EmbeddedScalarListsCapability,
      EmbeddedTypesCapability,
      JoinRelationLinksCapability,
      MongoJoinRelationLinksCapability,
      RelationLinkListCapability,
      EmbeddedTypesCapability,
      SupportsExistingDatabasesCapability
    )

    ConnectorCapabilities(common)
  }
}
