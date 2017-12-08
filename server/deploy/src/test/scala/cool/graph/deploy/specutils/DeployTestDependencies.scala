package cool.graph.deploy.specutils

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.deploy.DeployDependencies

case class DeployTestDependencies()(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends DeployDependencies {
  override implicit def self: DeployDependencies = this

  val internalTestDb = new InternalTestDatabase()
  val clientTestDb   = new ClientTestDatabase()
  val migrator       = TestMigrator()

  override val internalDb = internalTestDb.internalDatabase
  override val clientDb   = clientTestDb.clientDatabase
}
