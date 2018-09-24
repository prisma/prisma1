package com.prisma.api.connector.mongo

import com.prisma.api.connector.mongo.database.{MongoAction, MongoActionsBuilder, SimpleMongoAction}
import com.prisma.api.connector.{MutactionResults, UnitDatabaseMutactionResult}
import com.prisma.gc_values.IdGCValue

trait DatabaseMutactionInterpreter {
  protected val unitResult = UnitDatabaseMutactionResult
}

trait TopLevelDatabaseMutactionInterpreter extends DatabaseMutactionInterpreter {
  def mongoAction(mutationBuilder: MongoActionsBuilder): SimpleMongoAction[MutactionResults]
}

trait NestedDatabaseMutactionInterpreter extends DatabaseMutactionInterpreter {
  def mongoAction(mutationBuilder: MongoActionsBuilder, parentId: IdGCValue): MongoAction[MutactionResults]
}
