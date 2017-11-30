package cool.graph.singleserver

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.deploy.DeployDependencies

trait SingleServerApiDependencies extends DeployDependencies {}

case class SingleServerDependencies(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends SingleServerApiDependencies {}
