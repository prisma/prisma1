package com.prisma.api.connector.sqlite.native
import com.prisma.api.connector.{DatabaseMutactionExecutor, MutactionResults, TopLevelDatabaseMutaction}
import com.prisma.rs.NativeBinding
import com.prisma.shared.models.Project
import play.api.libs.json.JsValue
import prisma.protocol

import scala.concurrent.Future

case class SQLiteDatabaseMutactionExecutor(delegate: DatabaseMutactionExecutor) extends DatabaseMutactionExecutor {
  override def executeTransactionally(mutaction: TopLevelDatabaseMutaction): Future[MutactionResults] = {
    delegate.executeTransactionally(mutaction)
  }
  override def executeNonTransactionally(mutaction: TopLevelDatabaseMutaction): Future[MutactionResults] = {
    // this is only called by the export which we won't support for now
    ???
  }
  override def executeRaw(project: Project, query: String): Future[JsValue] = {
//    val input = prisma.protocol.ExecuteRawInput(
//      header = protocol.Header("ExecuteRawInput"),
//      query = query
//    )
//    val json = NativeBinding.execute_raw(input);
//    Future.successful(json)
    delegate.executeRaw(project, query)
  }
}
