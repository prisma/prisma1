package cool.graph.system

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.akkautil.http.ServerExecutor

import scala.language.postfixOps

object SystemMain extends App {
  implicit val system: ActorSystem             = ActorSystem("sangria-server")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val inj: SystemInjector             = new SystemInjectorImpl()

  ServerExecutor(8081, SystemServer(inj.schemaBuilder, "system")).startBlocking()
}
