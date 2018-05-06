package com.prisma.workers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.prisma.akkautil.http.{Routes, Server}
import com.prisma.workers.dependencies.WorkerDependencies

import scala.concurrent.Future
import scala.util.{Failure, Success}

case class WorkerServer(
    dependencies: WorkerDependencies,
    prefix: String = ""
)(
    implicit system: ActorSystem,
    materializer: ActorMaterializer
) extends Server {
  import system.dispatcher

  val workers = Vector[Worker](
    WebhookDelivererWorker(dependencies.httpClient, dependencies.webhooksConsumer)
  )

  val innerRoutes = Routes.emptyRoute

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

    stopFutures
  }
}
