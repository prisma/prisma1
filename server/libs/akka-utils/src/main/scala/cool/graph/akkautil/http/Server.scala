package cool.graph.akkautil.http

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives.pathPrefix
import scala.concurrent.Future

trait Server {
  val prefix: String

  protected val innerRoutes: Route

  def routes: Route = prefix match {
    case prfx if prfx.nonEmpty => pathPrefix(prfx) { innerRoutes }
    case _                     => innerRoutes
  }

  def onStart: Future[_] = Future.successful(())
  def onStop: Future[_]  = Future.successful(())

  def healthCheck: Future[_]
}
