package com.prisma.api.connector.mongo

import com.prisma.api.connector.{DatabaseMutactionExecutor, MutactionResults, TopLevelDatabaseMutaction}

import scala.concurrent.Future

class MongoDatabaseMutactionExecutor extends DatabaseMutactionExecutor {
  override def executeTransactionally(mutaction: TopLevelDatabaseMutaction): Future[MutactionResults] = ???

  override def executeNonTransactionally(mutaction: TopLevelDatabaseMutaction): Future[MutactionResults] = ???
}
