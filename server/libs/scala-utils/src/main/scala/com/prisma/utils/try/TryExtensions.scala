package cool.graph.utils.`try`

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object TryExtensions {
  implicit class TryExtensions[T](val theTry: Try[T]) extends AnyVal {
    def toFuture: Future[T] = theTry match {
      case Success(value)     => Future.successful(value)
      case Failure(exception) => Future.failed(exception)
    }
  }
}

object TryUtil {
  def sequence[T](trys: Vector[Try[T]]): Try[Vector[T]] = {
    val successes = trys.collect { case Success(x)     => x }
    val failures  = trys.collect { case f @ Failure(_) => f }
    if (successes.length == trys.length) {
      Success(successes)
    } else {
      failures.head.asInstanceOf[Try[Vector[T]]]
    }
  }
}
