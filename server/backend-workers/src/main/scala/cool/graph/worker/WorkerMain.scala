package cool.graph.worker

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.akkautil.http.ServerExecutor
import cool.graph.bugsnag.BugSnaggerImpl
import cool.graph.worker.services.WorkerCloudServices
import cool.graph.worker.utils.Env

object WorkerMain extends App {
  implicit val bugsnagger   = BugSnaggerImpl(Env.bugsangApiKey)
  implicit val system       = ActorSystem("backend-workers")
  implicit val materializer = ActorMaterializer()

  val services       = WorkerCloudServices()
  val serverExecutor = ServerExecutor(8090, WorkerServer(services))

  serverExecutor.startBlocking()
}
