package cool.graph.private_api.mutations

import cool.graph.Mutaction
import cool.graph.shared.errors.GeneralError

import scala.concurrent.{ExecutionContext, Future}

trait PrivateMutation[T] {
  def execute()(implicit ec: ExecutionContext): Future[T] = {
    for {
      mutactions <- prepare
      results    <- Future.sequence(mutactions.map(_.execute))
      errors     = results.collect { case e: GeneralError => e }
    } yield {
      if (errors.nonEmpty) {
        throw errors.head
      } else {
        result
      }
    }
  }

  def prepare: Future[List[Mutaction]]

  def result: T
}
