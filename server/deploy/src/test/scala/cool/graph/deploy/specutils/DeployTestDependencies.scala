package cool.graph.deploy.specutils

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.deploy.DeployDependencies
import cool.graph.deploy.server.ClusterAuthImpl
import cool.graph.graphql.GraphQlClient
import cool.graph.shared.models.{Project, ProjectId}

case class DeployTestDependencies()(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends DeployDependencies {
  override implicit def self: DeployDependencies = this

  val internalTestDb = new InternalTestDatabase()
  val clientTestDb   = new ClientTestDatabase()

  override lazy val internalDb = internalTestDb.internalDatabase
  override lazy val clientDb   = clientTestDb.clientDatabase

  val migrator             = TestMigrator(clientDb, internalDb, migrationPersistence)
  override val clusterAuth = new ClusterAuthImpl(publicKey = None)

  override def graphQlClient(project: Project) = {
    val port = sys.props.getOrElse("STUB_SERVER_PORT", {
      println("No running stub server detected! the GraphQlClient won't work!")
      12345
    })
    val headers = project.secrets.headOption match {
      case Some(secret) => Map("Authorization" -> s"Bearer $secret")
      case None         => Map.empty[String, String]
    }
    val ProjectId(name, stage) = project.projectId
    GraphQlClient(s"http://localhost:$port/$name/$stage/private", headers)
  }
}
