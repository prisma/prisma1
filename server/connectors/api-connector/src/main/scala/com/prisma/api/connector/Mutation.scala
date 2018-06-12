package com.prisma.api.connector

case class Mutation(
    mutactions: Vector[DatabaseMutaction],
    childs: Vector[Mutation]
)
case class MutationResult(
    mutactionResults: Vector[DatabaseMutactionResult],
    childs: Vector[MutationResult]
)
