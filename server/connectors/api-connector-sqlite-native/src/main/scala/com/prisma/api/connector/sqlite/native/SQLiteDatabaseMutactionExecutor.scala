package com.prisma.api.connector.sqlite.native
import com.prisma.api.connector.jdbc.{NestedDatabaseMutactionInterpreter, TopLevelDatabaseMutactionInterpreter}
import com.prisma.api.connector.jdbc.impl.JdbcDatabaseMutactionExecutor
import com.prisma.api.connector.{DatabaseMutactionExecutor, MutactionResults, NestedDatabaseMutaction, TopLevelDatabaseMutaction}
import com.prisma.connector.shared.jdbc.SlickDatabase
import com.prisma.rs.NativeBinding
import com.prisma.shared.models.Project
import play.api.libs.json.JsValue
import prisma.protocol

import scala.concurrent.{ExecutionContext, Future}

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

case class SQLiteDatabaseMutactionExecutor2(
    override val slickDatabase: SlickDatabase,
    override val manageRelayIds: Boolean
)(implicit ec: ExecutionContext)
    extends JdbcDatabaseMutactionExecutor(slickDatabase, manageRelayIds) {

  override def executeTransactionally(mutaction: TopLevelDatabaseMutaction): Future[MutactionResults] = {
    executeNonTransactionally(mutaction)
  }

  override def interpreterFor(mutaction: TopLevelDatabaseMutaction): TopLevelDatabaseMutactionInterpreter = {
    super.interpreterFor(mutaction)
  }

  override def interpreterFor(mutaction: NestedDatabaseMutaction): NestedDatabaseMutactionInterpreter = {
    super.interpreterFor(mutaction)
  }
}
