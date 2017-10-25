package cool.graph.utils.future

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

object FutureUtils {

  /**
    * Executes callbacks for either failure or success after a future completes, and _after those complete_ will return
    * the original future, which either contains the successful value or the error it failed with in the first place.
    * If the futures returned by the callbacks fail, those are returned instead.
    */
  implicit class FutureChainer[A](val f: Future[A]) extends AnyVal {
    def andThenFuture(handleSuccess: A => Future[_], handleFailure: Throwable => Future[_])(implicit executor: ExecutionContext): Future[A] = {
      for {
        _ <- f.toFutureTry.flatMap {
              case Success(x) => handleSuccess(x)
              case Failure(e) => handleFailure(e)
            }
        r <- f
      } yield r
    }
  }

  /**
    * Ensures that a list of () => Future[T] ("deferred future") is run / called and completed sequentially.
    * Returns a future containing a list of the result values of all futures.
    */
  implicit class DeferredFutureCollectionExtensions[T](val futures: List[() => Future[T]]) extends AnyVal {

    def runSequentially(implicit executor: ExecutionContext): Future[List[T]] = {
      val accumulator = Future.successful(List.empty[T])

      futures.foldLeft(accumulator)((prevFutures, nextFuture) => {
        for {
          list <- prevFutures
          next <- nextFuture()
        } yield list :+ next
      })
    }

    def runInChunksOf(maxParallelism: Int)(implicit executor: ExecutionContext): Future[List[T]] = {
      require(maxParallelism >= 1, "parallelism must be >= 1")
      futures match {
        case Nil =>
          Future.successful(List.empty)

        case _ =>
          val (firstFutures, nextFutures) = futures.splitAt(maxParallelism)
          val firstFuturesTriggered       = Future.sequence(firstFutures.map(fn => fn()))

          for {
            firstResults <- firstFuturesTriggered
            nextResults  <- nextFutures.runInChunksOf(maxParallelism)
          } yield {
            firstResults ++ nextResults
          }
      }
    }
  }

  /**
    * Maps a completed future to a successful future containing a try which can then be handled in a subsequent calls.
    * For example for handling expected errors:
    *
    * ftr.toFutureTry.flatMap {
    *   case Success(x) => Future.successful(x)
    *   case Failure(e) => Future.successful(someFallbackValue)
    * }
    */
  implicit class FutureExtensions[T](val future: Future[T]) extends AnyVal {
    def toFutureTry(implicit ec: ExecutionContext): Future[Try[T]] = {
      val promise = Promise[Try[T]]

      future.onComplete(promise.success)
      promise.future
    }
  }
}
