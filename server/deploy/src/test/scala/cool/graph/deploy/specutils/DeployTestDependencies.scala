package cool.graph.deploy.specutils

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.deploy.DeployDependencies
import cool.graph.deploy.server.DummyClusterAuth
import cool.graph.graphql.GraphQlClient
import cool.graph.messagebus.pubsub.inmemory.InMemoryAkkaPubSub
import cool.graph.shared.models.{Project, ProjectId}

case class DeployTestDependencies()(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends DeployDependencies {
  override implicit def self: DeployDependencies = this

  val internalTestDb = new InternalTestDatabase()
  val clientTestDb   = new ClientTestDatabase()

  override lazy val internalDb = internalTestDb.internalDatabase
  override lazy val clientDb   = clientTestDb.clientDatabase

  override lazy val migrator              = TestMigrator(clientDb, internalDb, migrationPersistence)
  override lazy val clusterAuth           = DummyClusterAuth()
  override lazy val invalidationPublisher = InMemoryAkkaPubSub[String]()

  override def graphQlClient(project: Project) = {
    val port = sys.props.getOrElse("STUB_SERVER_PORT", sys.error("No running stub server detected! Can't instantiate GraphQlClient."))
    val headers = project.secrets.headOption match {
      case Some(secret) => Map("Authorization" -> s"Bearer $secret")
      case None         => Map.empty[String, String]
    }
    val ProjectId(name, stage) = project.projectId
    GraphQlClient(s"http://localhost:$port/$name/$stage/private", headers)
  }
}
