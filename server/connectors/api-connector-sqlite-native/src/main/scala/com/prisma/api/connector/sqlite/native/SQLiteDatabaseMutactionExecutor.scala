package com.prisma.api.connector.sqlite.native
import com.prisma.api.connector.{DatabaseMutactionExecutor, MutactionResults, TopLevelDatabaseMutaction}
import com.prisma.shared.models.Project
import play.api.libs.json.JsValue

import scala.concurrent.Future

case class SQLiteDatabaseMutactionExecutor(delegate: DatabaseMutactionExecutor) extends DatabaseMutactionExecutor {
  override def executeTransactionally(mutaction: TopLevelDatabaseMutaction): Future[MutactionResults] = {
    delegate.executeTransactionally(mutaction)
  }
  override def executeNonTransactionally(mutaction: TopLevelDatabaseMutaction): Future[MutactionResults] = {
    delegate.executeNonTransactionally(mutaction)
  }
  override def executeRaw(project: Project, query: String): Future[JsValue] = {
    delegate.executeRaw(project, query)
  }
}
