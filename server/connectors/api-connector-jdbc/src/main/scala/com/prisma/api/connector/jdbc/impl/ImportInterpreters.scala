package com.prisma.api.connector.jdbc.impl

import com.prisma.api.connector.{DatabaseMutactionResult, ImportNodes, ImportRelations, ImportScalarLists}
import com.prisma.api.connector.jdbc.DatabaseMutactionInterpreter
import com.prisma.api.connector.jdbc.database.JdbcActionsBuilder
import com.prisma.gc_values.IdGCValue
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext

case class ImportNodesInterpreter(mutaction: ImportNodes) extends DatabaseMutactionInterpreter {
  override protected def dbioAction(mutationBuilder: JdbcActionsBuilder, parentId: IdGCValue): DBIO[DatabaseMutactionResult] = {
    mutationBuilder.createDataItemsImport(mutaction).andThen(unitResult)
  }
}

case class ImportRelationsInterpreter(mutaction: ImportRelations) extends DatabaseMutactionInterpreter {
  override protected def dbioAction(mutationBuilder: JdbcActionsBuilder, parentId: IdGCValue): DBIO[DatabaseMutactionResult] = {
    mutationBuilder.createRelationRowsImport(mutaction).andThen(unitResult)
  }
}

case class ImportScalarListsInterpreter(mutaction: ImportScalarLists)(implicit ec: ExecutionContext) extends DatabaseMutactionInterpreter {
  override protected def dbioAction(mutationBuilder: JdbcActionsBuilder, parentId: IdGCValue): DBIO[DatabaseMutactionResult] = {
    mutationBuilder.pushScalarListsImport(mutaction).andThen(unitResult)
  }
}
