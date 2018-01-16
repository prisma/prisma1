package cool.graph.utils.or

import org.scalactic.{Bad, Good, Or}

import scala.concurrent.Future

object OrExtensions {
  implicit class OrExtensions[G, B](or: Or[G, B]) {
    def toFuture: Future[G] = {
      or match {
        case Good(x)    => Future.successful(x)
        case Bad(error) => Future.failed(new Exception(s"The underlying Or was a Bad: $error"))
      }
    }
  }

  def sequence[A, B](seq: Vector[Or[A, B]]): Or[Vector[A], B] = {
    def recurse(seq: Vector[Or[A, B]])(acc: Vector[A]): Or[Vector[A], B] = {
      if (seq.isEmpty) {
        Good(acc)
      } else {
        seq.head match {
          case Good(x)    => recurse(seq.tail)(acc :+ x)
          case Bad(error) => Bad(error)
        }
      }
    }
    recurse(seq)(Vector.empty)
  }

  def sequence[A, B](opt: Option[Or[A, B]]): Or[Option[A], B] = {
    opt match {
      case Some(x) => x.map(Some(_))
      case None    => Good(None)
    }
  }
}
