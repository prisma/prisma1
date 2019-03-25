package com.prisma.api.connector.sqlite.native
import com.google.protobuf.ByteString
import com.prisma.api.connector.jdbc.{NestedDatabaseMutactionInterpreter, TopLevelDatabaseMutactionInterpreter}
import com.prisma.api.connector.jdbc.impl.{GetFieldFromSQLUniqueException, JdbcDatabaseMutactionExecutor}
import com.prisma.api.connector._
import com.prisma.api.connector.jdbc.database.JdbcActionsBuilder
import com.prisma.api.schema.APIErrors
import com.prisma.api.schema.APIErrors.{FieldCannotBeNull, NodeNotFoundForWhereError}
import com.prisma.connector.shared.jdbc.SlickDatabase
import com.prisma.gc_values.ListGCValue
import com.prisma.rs.{NativeBinding, UniqueConstraintViolation}
import com.prisma.shared.models.Project
import play.api.libs.json.{JsValue, Json}
import prisma.protocol
import prisma.protocol.Error
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

    val DO_NOT_FORWARD_THIS_ONE = false

    mutaction match {
      case m: TopLevelCreateNode =>
        val protoMutaction = prisma.protocol.DatabaseMutaction.Type.Create(
          createNodeToProtocol(m)
        )
        val envelope = prisma.protocol.DatabaseMutaction(projectJson, protoMutaction)
        top_level_mutaction_interpreter(envelope, m)

      case m: TopLevelUpdateNode =>
        val protoMutaction = prisma.protocol.DatabaseMutaction.Type.Update(
          updateNodeToProtocol(m)
        )
        val envelope = prisma.protocol.DatabaseMutaction(projectJson, protoMutaction)
        val errorHandler: PartialFunction[prisma.protocol.Error.Value, Throwable] = {
          case Error.Value.NodeNotFoundForWhere(_)  => throw NodeNotFoundForWhereError(m.where)
          case Error.Value.FieldCannotBeNull(field) => throw FieldCannotBeNull(field)
        }
        top_level_mutaction_interpreter(envelope, m, errorHandler)

      case m: TopLevelUpsertNode if DO_NOT_FORWARD_THIS_ONE =>
        val protoMutaction = prisma.protocol.DatabaseMutaction.Type.Upsert(
          prisma.protocol.UpsertNode(
            header = prisma.protocol.Header(headerName),
            where = toNodeSelector(m.where),
            create = createNodeToProtocol(m.create),
            update = updateNodeToProtocol(m.update),
          ))
        val envelope = prisma.protocol.DatabaseMutaction(projectJson, protoMutaction)
        top_level_mutaction_interpreter(envelope, m)

      case m: TopLevelDeleteNode if DO_NOT_FORWARD_THIS_ONE =>
        val protoMutaction = prisma.protocol.DatabaseMutaction.Type.Delete(
          prisma.protocol.DeleteNode(
            header = prisma.protocol.Header(headerName),
            where = toNodeSelector(m.where),
          ))
        val envelope = prisma.protocol.DatabaseMutaction(projectJson, protoMutaction)
        top_level_mutaction_interpreter(envelope, m)

      case m: TopLevelUpdateNodes if DO_NOT_FORWARD_THIS_ONE =>
        val protoMutaction = prisma.protocol.DatabaseMutaction.Type.UpdateNodes(
          prisma.protocol.UpdateNodes(
            header = prisma.protocol.Header(headerName),
            modelName = m.model.name,
            filter = toPrismaFilter(m.whereFilter.getOrElse(AndFilter(Vector.empty))),
            nonListArgs = prismaArgsToProtoclArgs(m.nonListArgs),
            listArgs = listArgsToProtocolArgs(m.listArgs),
          ))
        val envelope = prisma.protocol.DatabaseMutaction(projectJson, protoMutaction)
        top_level_mutaction_interpreter(envelope, m)

      case m: TopLevelDeleteNodes if DO_NOT_FORWARD_THIS_ONE =>
        val protoMutaction = prisma.protocol.DatabaseMutaction.Type.DeleteNodes(
          prisma.protocol.DeleteNodes(
            header = prisma.protocol.Header(headerName),
            modelName = m.model.name,
            filter = toPrismaFilter(m.whereFilter.getOrElse(AndFilter(Vector.empty))),
          ))
        val envelope = prisma.protocol.DatabaseMutaction(projectJson, protoMutaction)
        top_level_mutaction_interpreter(envelope, m)

      case m: ResetData if DO_NOT_FORWARD_THIS_ONE =>
        val protoMutaction = prisma.protocol.DatabaseMutaction.Type.Reset(prisma.protocol.ResetData())
        val envelope       = prisma.protocol.DatabaseMutaction(projectJson, protoMutaction)
        top_level_mutaction_interpreter(envelope, m)

      case _ =>
        super.interpreterFor(mutaction)
    }
  }

  private def createNodeToProtocol(m: CreateNode) = {
    prisma.protocol.CreateNode(
      header = prisma.protocol.Header(m.getClass.getSimpleName),
      modelName = m.model.name,
      nonListArgs = prismaArgsToProtoclArgs(m.nonListArgs),
      listArgs = listArgsToProtocolArgs(m.listArgs)
    )
  }

  private def updateNodeToProtocol(m: TopLevelUpdateNode) = {
    prisma.protocol.UpdateNode(
      header = prisma.protocol.Header(m.getClass.getSimpleName),
      where = toNodeSelector(m.where),
      nonListArgs = prismaArgsToProtoclArgs(m.nonListArgs),
      listArgs = listArgsToProtocolArgs(m.listArgs)
    )
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
      mutaction: DatabaseMutaction,
      errorHandler: PartialFunction[prisma.protocol.Error.Value, Throwable] = PartialFunction.empty
  ): TopLevelDatabaseMutactionInterpreter = {

    new TopLevelDatabaseMutactionInterpreter {
      override protected def dbioAction(mutationBuilder: JdbcActionsBuilder): dbio.DBIO[DatabaseMutactionResult] = {
        SimpleDBIO { x =>
          val executionResult = NativeBinding.execute_mutaction(protoMutaction, errorHandler)
          executionResult.`type` match {
            case prisma.protocol.DatabaseMutactionResult.Type.Create(result) =>
              val m = mutaction match {
                case m: CreateNode => m
                case m: UpsertNode => m.create
                case m             => sys.error(s"mutaction of type [$m] is disallowed here")
              }
              CreateNodeResult(toIdGcValue(result.id), m)

            case prisma.protocol.DatabaseMutactionResult.Type.Update(result) =>
              val m = mutaction match {
                case m: UpdateNode => m
                case m: UpsertNode => m.update
                case m             => sys.error(s"mutaction of type [$m] is disallowed here")
              }
              UpdateNodeResult(toIdGcValue(result.id), PrismaNode.dummy, m)

            case prisma.protocol.DatabaseMutactionResult.Type.Delete(_) =>
              DeleteNodeResult(PrismaNode.dummy, mutaction.asInstanceOf[DeleteNode])

            case prisma.protocol.DatabaseMutactionResult.Type.Many(result) =>
              ManyNodesResult(mutaction.asInstanceOf[FinalMutaction], result.count)

            case prisma.protocol.DatabaseMutactionResult.Type.Unit(_) =>
              UnitDatabaseMutactionResult

            case prisma.protocol.DatabaseMutactionResult.Type.Empty =>
              UnitDatabaseMutactionResult
          }
        }
      }

      override val errorMapper: PartialFunction[Throwable, APIErrors.UniqueConstraintViolation] = {
        case UniqueConstraintViolation(fieldName) => {
          val splitted = fieldName.split('.')
          APIErrors.UniqueConstraintViolation(splitted(0), "Field name = " + splitted(1))
        }
      }
    }
  }
}
