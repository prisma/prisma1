package cool.graph.deploy.specutils

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.deploy.DeployDependencies
import cool.graph.deploy.server.ClusterAuthImpl

case class DeployTestDependencies()(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends DeployDependencies {
  override implicit def self: DeployDependencies = this

  val internalTestDb = new InternalTestDatabase()
  val clientTestDb   = new ClientTestDatabase()

  override lazy val internalDb = internalTestDb.internalDatabase
  override lazy val clientDb   = clientTestDb.clientDatabase

  val migrator             = TestMigrator(clientDb, internalDb, migrationPersistence)
  override val clusterAuth = new ClusterAuthImpl(publicKey = None)
}
