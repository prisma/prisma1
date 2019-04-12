package com.prisma.api.connector.jdbc.impl

import com.prisma.api.connector.jdbc.TopLevelDatabaseMutactionInterpreter
import com.prisma.api.connector.jdbc.database.JdbcActionsBuilder
import com.prisma.api.connector.{DatabaseMutactionResult, ImportNodes, ImportRelations, ImportScalarLists}
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext

case class ImportNodesInterpreter(mutaction: ImportNodes) extends TopLevelDatabaseMutactionInterpreter {
  override protected def dbioAction(mutationBuilder: JdbcActionsBuilder): DBIO[DatabaseMutactionResult] = {
    mutationBuilder.importNodes(mutaction).andThen(unitResult)
  }
}

case class ImportRelationsInterpreter(mutaction: ImportRelations) extends TopLevelDatabaseMutactionInterpreter {
  override protected def dbioAction(mutationBuilder: JdbcActionsBuilder): DBIO[DatabaseMutactionResult] = {
    mutationBuilder.importRelations(mutaction).andThen(unitResult)
  }
}

case class ImportScalarListsInterpreter(mutaction: ImportScalarLists)(implicit ec: ExecutionContext) extends TopLevelDatabaseMutactionInterpreter {
  override protected def dbioAction(mutationBuilder: JdbcActionsBuilder): DBIO[DatabaseMutactionResult] = {
    mutationBuilder.importScalarLists(mutaction).andThen(unitResult)
  }
}
