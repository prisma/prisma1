package com.prisma.deploy.schema.mutations

import scala.concurrent.Future

trait Mutation[+T <: sangria.relay.Mutation] {
  def execute: Future[MutationResult[T]]
}

sealed trait MutationResult[+T]
case class MutationSuccess[T](result: T) extends MutationResult[T]
object MutationError                     extends MutationResult[Nothing]
