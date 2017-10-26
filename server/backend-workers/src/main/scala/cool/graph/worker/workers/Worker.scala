package cool.graph.worker.workers

import scala.concurrent.Future

trait Worker {
  def start: Future[_] = Future.successful(())
  def stop: Future[_]  = Future.successful(())
}
