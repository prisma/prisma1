package com.prisma.api.connector

case class Mutation(
    mutactions: Vector[DatabaseMutaction],
    childs: Vector[Mutation] = Vector.empty
)
object Mutation {
  def apply(mutaction: DatabaseMutaction): Mutation = Mutation(Vector(mutaction))
}

case class MutationResult(
    mutactionResults: Vector[DatabaseMutactionResult],
    childs: Vector[MutationResult]
)
