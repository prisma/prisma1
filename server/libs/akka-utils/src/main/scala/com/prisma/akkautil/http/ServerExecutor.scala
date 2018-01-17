package com.prisma.akkautil.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import ch.megard.akka.http.cors.scaladsl.CorsDirectives
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._

import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{Await, Future}

/**
  * Class that knows how to start and stop servers. Takes one or more servers.
  * In case that more than one server is given, the ServerExecutor combines all given servers into one server
  * by collecting all their routes. Evaluation order is strictly linear.
  */
case class ServerExecutor(port: Int, servers: Server*)(implicit system: ActorSystem, materializer: ActorMaterializer) {
  import system.dispatcher

  val routes: Route = {
    handleRejections(CorsDirectives.corsRejectionHandler) {
      cors() {
        val routes = servers.map(_.routes) :+ statusRoute
        routes.reduceLeft(_ ~ _)
      }
    }
  }

  def statusRoute: Route = (get & path("status")) {
    val checks = Future.sequence(servers.map(_.healthCheck))

    onSuccess(checks) { _ =>
      complete("OK")
    }
  }

  lazy val serverBinding: Future[ServerBinding] = {
    val binding = Http().bindAndHandle(Route.handlerFlow(routes), "0.0.0.0", port)
    binding.foreach(b => println(s"Server running on :${b.localAddress.getPort}"))
    binding
  }

  def start: Future[_] = Future.sequence[Any, Seq](servers.map(_.onStart) :+ serverBinding)
  def stop: Future[_]  = Future.sequence[Any, Seq](servers.map(_.onStop) :+ serverBinding.map(_.unbind))

  // Starts the server and blocks the calling thread until the underlying actor system terminates.
  def startBlocking(duration: Duration = 15.seconds): Unit = {
    start
    Await.result(system.whenTerminated, Duration.Inf)
  }

  def stopBlocking(duration: Duration = 15.seconds): Unit = Await.result(stop, duration)
}
