package cool.graph.api
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.LazyLogging
import cool.graph.akkautil.http.ServerExecutor
import cool.graph.api.schema.SchemaBuilder
import cool.graph.api.server.ApiServer

object ApiMain extends App with LazyLogging {
  implicit val system          = ActorSystem("api-main")
  implicit val materializer    = ActorMaterializer()
  implicit val apiDependencies = new ApiDependenciesImpl

  val schemaBuilder = SchemaBuilder()
  val server        = ApiServer(schemaBuilder = schemaBuilder, "api")

  ServerExecutor(9000, server).startBlocking()
}
