package cool.graph.worker

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.akkautil.http.{Routes, Server}
import cool.graph.bugsnag.BugSnagger
import cool.graph.worker.services.WorkerServices
import cool.graph.worker.workers.{FunctionLogsWorker, WebhookDelivererWorker, Worker}

import scala.concurrent.Future
import scala.util.{Failure, Success}

case class WorkerServer(services: WorkerServices, prefix: String = "")(implicit system: ActorSystem, materializer: ActorMaterializer, bugsnag: BugSnagger)
    extends Server {
  import system.dispatcher

  val workers = Vector[Worker](
    FunctionLogsWorker(services.logsDb, services.logsQueue),
    WebhookDelivererWorker(services.httpClient, services.webhooksConsumer, services.logsQueue)
  )

  val innerRoutes = Routes.emptyRoute

  def healthCheck: Future[_] = Future.successful(())

  override def onStart: Future[_] = {
    println("Initializing workers...")
    val initFutures = Future.sequence(workers.map(_.start))

    initFutures.onComplete {
      case Success(_)   => println(s"Successfully started ${workers.length} workers.")
      case Failure(err) => println(s"Failed to initialize workers: $err")
    }

    initFutures
  }

  override def onStop: Future[_] = {
    println("Stopping workers...")
    val stopFutures = Future.sequence(workers.map(_.stop))

    stopFutures.onComplete(_ => services.shutdown)
    stopFutures
  }
}
