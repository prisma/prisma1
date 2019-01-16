package com.prisma.deploy.persistence
import com.prisma.deploy.connector.persistence.InternalMigration
import com.prisma.deploy.specutils.DeploySpecBase
import com.prisma.shared.models.ConnectorCapability
import org.scalatest.{FlatSpec, Matchers, WordSpecLike}

class InternalMigrationPersistenceSpec extends WordSpecLike with Matchers with DeploySpecBase {
  override def doNotRunForCapabilities = Set(ConnectorCapability.MongoJoinRelationLinksCapability)
  val persistence                      = testDependencies.deployConnector.internalMigrationPersistence

  "everything should work" in {
    val migration1 = InternalMigration(id = "my-fancy-test-migration-1")
    val migration2 = InternalMigration(id = "my-fancy-test-migration-2")

    persistence.create(migration1).await
    persistence.loadAll().await should be(Vector(migration1))

    persistence.create(migration2).await
    persistence.loadAll().await should be(Vector(migration1, migration2))
  }

}
