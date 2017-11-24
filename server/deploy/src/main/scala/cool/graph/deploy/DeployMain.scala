package cool.graph.deploy
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.deploy.schema.{SchemaBuilder, SchemaBuilderImpl}
import cool.graph.deploy.server.DeployServer
import slick.jdbc.MySQLProfile.api._

object DeployMain {
  implicit val system       = ActorSystem("deploy-main")
  implicit val materializer = ActorMaterializer()
  val internalDb            = Database.forConfig("internal")
  val schemaBuilder         = SchemaBuilder(internalDb)
  val server                = DeployServer(schemaBuilder = schemaBuilder)
}
