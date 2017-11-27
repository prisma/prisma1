package cool.graph.api
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.LazyLogging
import cool.graph.api.schema.SchemaBuilder
import cool.graph.api.server.ApiServer

import scala.concurrent.ExecutionContext.Implicits.global

object ApiMain extends App with LazyLogging {
  implicit val system          = ActorSystem("deploy-main")
  implicit val materializer    = ActorMaterializer()
  implicit val apiDependencies = new ApiDependenciesImpl
  val schemaBuilder            = SchemaBuilder()
  val server                   = ApiServer(schemaBuilder = schemaBuilder)

  Http().bindAndHandle(server.innerRoutes, "0.0.0.0", 9000).onSuccess {
    case _ => logger.info("Server running on: 9000")
  }
}
