package cool.graph.shared.database

import slick.dbio.{DBIOAction, Effect, NoStream}

import scala.util.{Failure, Success, Try}

trait SqlDDLMutaction {
  def execute: Try[DBIOAction[Any, NoStream, Effect.All]]
  def rollback: Try[DBIOAction[Any, NoStream, Effect.All]] = Failure(sys.error("rollback not implemented"))
  def verify: Try[Unit]                                    = Success(())
}
