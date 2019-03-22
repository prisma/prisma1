package com.prisma.api.connector.sqlite.native
import com.google.protobuf.ByteString
import com.prisma.api.connector.jdbc.{NestedDatabaseMutactionInterpreter, TopLevelDatabaseMutactionInterpreter}
import com.prisma.api.connector.jdbc.impl.JdbcDatabaseMutactionExecutor
import com.prisma.api.connector._
import com.prisma.api.connector.jdbc.database.JdbcActionsBuilder
import com.prisma.connector.shared.jdbc.SlickDatabase
import com.prisma.gc_values.ListGCValue
import com.prisma.rs.NativeBinding
import com.prisma.shared.models.Project
import play.api.libs.json.{JsValue, Json}
import prisma.protocol
import slick.dbio

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

class SQLiteDatabaseMutactionExecutor2(
    slickDatabaseArg: SlickDatabase,
    manageRelayIds: Boolean
)(implicit ec: ExecutionContext)
    extends JdbcDatabaseMutactionExecutor(slickDatabaseArg, manageRelayIds) {

  import this.slickDatabaseArg.profile.api._
  import NativeUtils._

  override def executeTransactionally(mutaction: TopLevelDatabaseMutaction): Future[MutactionResults] = {
    executeNonTransactionally(mutaction)
  }

  override def interpreterFor(mutaction: TopLevelDatabaseMutaction): TopLevelDatabaseMutactionInterpreter = {
    import com.prisma.shared.models.ProjectJsonFormatter._
    val projectJson = ByteString.copyFromUtf8(Json.toJson(mutaction.project).toString())
    val headerName  = mutaction.getClass.getSimpleName

    mutaction match {
      case m: TopLevelCreateNode =>
        val protoMutaction = prisma.protocol.DatabaseMutaction.Type.Create(
          prisma.protocol.CreateNode(
            header = prisma.protocol.Header(headerName),
            modelName = m.model.name,
            nonListArgs = prismaArgsToProtoclArgs(m.nonListArgs),
            listArgs = listArgsToProtocolArgs(m.listArgs)
          ))
        val envelope = prisma.protocol.DatabaseMutaction(projectJson, protoMutaction)
        top_level_mutaction_interpreter(envelope, m)

//      case m: TopLevelUpdateNode =>
//        val protoMutaction = prisma.protocol.DatabaseMutaction.Type.Create(
//          prisma.protocol.CreateNode(
//            header = prisma.protocol.Header(headerName),
//            modelName = m.model.name,
//            nonListArgs = prismaArgsToProtoclArgs(m.nonListArgs),
//            listArgs = listArgsToProtocolArgs(m.listArgs)
//          ))
//        val envelope = prisma.protocol.DatabaseMutaction(projectJson, protoMutaction)
//        top_level_mutaction_interpreter(envelope, m)

      case _ =>
        super.interpreterFor(mutaction)
    }
  }

  override def interpreterFor(mutaction: NestedDatabaseMutaction): NestedDatabaseMutactionInterpreter = {
    super.interpreterFor(mutaction)
  }

  private def listArgsToProtocolArgs(listArgs: Vector[(String, ListGCValue)]): protocol.PrismaArgs = {
    prisma.protocol.PrismaArgs(
      listArgs.map { case (k, v) => prisma.protocol.KeyValueContainer(k, toValueContainer(v)) }
    )
  }

  private def prismaArgsToProtoclArgs(prismaArgs: PrismaArgs): protocol.PrismaArgs = {
    prisma.protocol.PrismaArgs(
      prismaArgs.rootGCMap.map { case (k, v) => prisma.protocol.KeyValueContainer(k, toValueContainer(v)) }.toVector
    )
  }

  private def top_level_mutaction_interpreter(
      protoMutaction: prisma.protocol.DatabaseMutaction,
      mutaction: DatabaseMutaction
  ): TopLevelDatabaseMutactionInterpreter = {

    new TopLevelDatabaseMutactionInterpreter {
      override protected def dbioAction(mutationBuilder: JdbcActionsBuilder): dbio.DBIO[DatabaseMutactionResult] = {
        SimpleDBIO { x =>
          val executionResult = NativeBinding.execute_mutaction(protoMutaction)
          executionResult.`type` match {
            case prisma.protocol.DatabaseMutactionResult.Type.Create(result) =>
              CreateNodeResult(toIdGcValue(result.id), mutaction.asInstanceOf[CreateNode])

            case prisma.protocol.DatabaseMutactionResult.Type.Unit(_) =>
              UnitDatabaseMutactionResult

            case prisma.protocol.DatabaseMutactionResult.Type.Empty =>
              UnitDatabaseMutactionResult
          }
        }
      }
    }
  }
}
