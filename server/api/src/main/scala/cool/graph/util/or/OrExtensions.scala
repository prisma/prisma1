package cool.graph.util.or

import org.scalactic.{Bad, Good, Or}

import scala.concurrent.Future

object OrExtensions {
  implicit class OrExtensions[G, B](or: Or[G, B]) {
    def toFuture: Future[G] = {
      or match {
        case Good(x)    => Future.successful(x)
        case Bad(error) => Future.failed(new Exception(s"The underlying Or was a Bad: ${error}"))
      }
    }
  }
}
