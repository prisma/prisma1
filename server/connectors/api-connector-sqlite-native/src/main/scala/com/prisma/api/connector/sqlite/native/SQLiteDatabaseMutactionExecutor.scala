package com.prisma.api.connector.sqlite.native
import com.google.protobuf.ByteString
import com.prisma.api.connector.jdbc.{NestedDatabaseMutactionInterpreter, TopLevelDatabaseMutactionInterpreter}
import com.prisma.api.connector.jdbc.impl._
import com.prisma.api.connector._
import com.prisma.api.connector.jdbc.database.JdbcActionsBuilder
import com.prisma.api.schema.APIErrors
import com.prisma.api.schema.APIErrors.{FieldCannotBeNull, NodeNotFoundForWhereError}
import com.prisma.connector.shared.jdbc.SlickDatabase
import com.prisma.gc_values.{IdGCValue, ListGCValue}
import com.prisma.rs.{NativeBinding, NodeNotFoundForWhere, RelationViolation, UniqueConstraintViolation}
import com.prisma.shared.models.Project
import play.api.libs.json.{JsValue, Json}
import prisma.protocol
import prisma.protocol.Error
import slick.dbio.DBIO
import slick.jdbc.TransactionIsolation

import scala.concurrent.{ExecutionContext, Future}

case class SQLiteDatabaseMutactionExecutor(
    slickDatabaseArg: SlickDatabase,
    manageRelayIds: Boolean
)(implicit ec: ExecutionContext)
    extends DatabaseMutactionExecutor {

  import slickDatabaseArg.profile.api.{DBIO => _, _}
  import NativeUtils._

  override def executeRaw(project: Project, query: String): Future[JsValue] = {
    val action = JdbcActionsBuilder(project, slickDatabaseArg).executeRaw(query)
    runAttached(project, action)
  }

  override def executeNonTransactionally(mutaction: TopLevelDatabaseMutaction) = execute(mutaction, transactionally = false)
  override def executeTransactionally(mutaction: TopLevelDatabaseMutaction)    = execute(mutaction, transactionally = false)

  private def execute(mutaction: TopLevelDatabaseMutaction, transactionally: Boolean): Future[MutactionResults] = {

    val actionsBuilder = JdbcActionsBuilder(mutaction.project, slickDatabaseArg)
    val singleAction = transactionally match {
      case true  => executeTopLevelMutaction(mutaction, actionsBuilder).transactionally
      case false => executeTopLevelMutaction(mutaction, actionsBuilder)
    }

    val finalAction = if (slickDatabaseArg.isMySql) singleAction.withTransactionIsolation(TransactionIsolation.ReadCommitted) else singleAction

    runAttached(mutaction.project, finalAction)
  }

  def executeTopLevelMutaction(
      mutaction: TopLevelDatabaseMutaction,
      mutationBuilder: JdbcActionsBuilder
  ): DBIO[MutactionResults] = {
    mutaction match {
      case m: TopLevelUpsertNode =>
        for {
          result <- interpreterFor(m).dbioActionWithErrorMapped(mutationBuilder)
          childResults <- result match {
                           case result: CreateNodeResult =>
                             DBIO.sequence(m.create.allNestedMutactions.map(executeNestedMutaction(_, result.id, mutationBuilder)))
                           case result: UpdateNodeResult =>
                             DBIO.sequence(m.update.allNestedMutactions.map(executeNestedMutaction(_, result.id, mutationBuilder)))
                           case _ => DBIO.successful(Vector.empty)
                         }
        } yield MutactionResults(result +: childResults.flatMap(_.results))
      case m: FurtherNestedMutaction =>
        for {
          result <- interpreterFor(m).dbioActionWithErrorMapped(mutationBuilder)
          childResults <- result match {
                           case result: FurtherNestedMutactionResult =>
                             DBIO.sequence(m.allNestedMutactions.map(executeNestedMutaction(_, result.id, mutationBuilder)))
                           case _ => DBIO.successful(Vector.empty)
                         }
        } yield MutactionResults(result +: childResults.flatMap(_.results))

      case m: FinalMutaction =>
        for {
          result <- interpreterFor(m).dbioActionWithErrorMapped(mutationBuilder)
        } yield MutactionResults(Vector(result))
    }
  }

  def executeNestedMutaction(
      mutaction: NestedDatabaseMutaction,
      parentId: IdGCValue,
      mutationBuilder: JdbcActionsBuilder
  ): DBIO[MutactionResults] = {
    mutaction match {
      case m: UpsertNode =>
        for {
          result <- interpreterFor(m, parentId).dbioActionWithErrorMapped(mutationBuilder, parentId)
          childResults <- executeNestedMutaction(result.asInstanceOf[UpsertNodeResult].result.asInstanceOf[NestedDatabaseMutaction], parentId, mutationBuilder)
                           .map(Vector(_))
        } yield MutactionResults(result +: childResults.flatMap(_.results))

      case m: FurtherNestedMutaction =>
        for {
          result <- interpreterFor(m, parentId).dbioActionWithErrorMapped(mutationBuilder, parentId)
          childResults <- result match {
                           case result: FurtherNestedMutactionResult =>
                             DBIO.sequence(m.allNestedMutactions.map(executeNestedMutaction(_, result.id, mutationBuilder)))
                           case _ => DBIO.successful(Vector.empty)
                         }
        } yield MutactionResults(result +: childResults.flatMap(_.results))

      case m: FinalMutaction =>
        for {
          result <- interpreterFor(m, parentId).dbioActionWithErrorMapped(mutationBuilder, parentId)
        } yield MutactionResults(Vector(result))
    }
  }

  def interpreterFor(mutaction: TopLevelDatabaseMutaction): TopLevelDatabaseMutactionInterpreter = {
    import com.prisma.shared.models.ProjectJsonFormatter._
    val projectJson = ByteString.copyFromUtf8(Json.toJson(mutaction.project).toString())
    val headerName  = mutaction.getClass.getSimpleName

    val DO_NOT_FORWARD_THIS_ONE = false

    mutaction match {
      case m: TopLevelCreateNode =>
        val protoMutaction = prisma.protocol.DatabaseMutaction.Type.Create(
          createNodeToProtocol(m)
        )
        val envelope = prisma.protocol.DatabaseMutaction(projectJson, None, protoMutaction)
        top_level_mutaction_interpreter(envelope, m)

      case m: TopLevelUpdateNode =>
        val protoMutaction = prisma.protocol.DatabaseMutaction.Type.Update(
          updateNodeToProtocol(m)
        )
        val envelope = prisma.protocol.DatabaseMutaction(projectJson, None, protoMutaction)
        val errorHandler: PartialFunction[prisma.protocol.Error.Value, Throwable] = {
          case Error.Value.NodeNotFoundForWhere(_)  => throw NodeNotFoundForWhereError(m.where)
          case Error.Value.FieldCannotBeNull(field) => throw FieldCannotBeNull(field)
        }
        top_level_mutaction_interpreter(envelope, m, errorHandler)

      case m: TopLevelUpsertNode =>
        val protoMutaction = prisma.protocol.DatabaseMutaction.Type.Upsert(
          prisma.protocol.UpsertNode(
            header = prisma.protocol.Header(headerName),
            where = toNodeSelector(m.where),
            create = createNodeToProtocol(m.create),
            update = updateNodeToProtocol(m.update),
          ))
        val envelope = prisma.protocol.DatabaseMutaction(projectJson, None, protoMutaction)
        top_level_mutaction_interpreter(envelope, m)

      case m: TopLevelUpdateNodes =>
        val protoMutaction = prisma.protocol.DatabaseMutaction.Type.UpdateNodes(
          prisma.protocol.UpdateNodes(
            header = prisma.protocol.Header(headerName),
            modelName = m.model.name,
            filter = toProtocolFilter(m.whereFilter.getOrElse(AndFilter(Vector.empty))),
            nonListArgs = prismaArgsToProtoclArgs(m.nonListArgs),
            listArgs = listArgsToProtocolArgs(m.listArgs),
          ))
        val envelope = prisma.protocol.DatabaseMutaction(projectJson, None, protoMutaction)
        top_level_mutaction_interpreter(envelope, m)

      case m: TopLevelDeleteNode if DO_NOT_FORWARD_THIS_ONE =>
        val protoMutaction = prisma.protocol.DatabaseMutaction.Type.Delete(
          prisma.protocol.DeleteNode(
            header = prisma.protocol.Header(headerName),
            where = toNodeSelector(m.where),
          ))
        val envelope = prisma.protocol.DatabaseMutaction(projectJson, None, protoMutaction)
        top_level_mutaction_interpreter(envelope, m)

      case m: TopLevelDeleteNodes if DO_NOT_FORWARD_THIS_ONE =>
        val protoMutaction = prisma.protocol.DatabaseMutaction.Type.DeleteNodes(
          prisma.protocol.DeleteNodes(
            header = prisma.protocol.Header(headerName),
            modelName = m.model.name,
            filter = toProtocolFilter(m.whereFilter.getOrElse(AndFilter(Vector.empty))),
          ))
        val envelope = prisma.protocol.DatabaseMutaction(projectJson, None, protoMutaction)
        top_level_mutaction_interpreter(envelope, m)

      case m: ResetData if DO_NOT_FORWARD_THIS_ONE =>
        val protoMutaction = prisma.protocol.DatabaseMutaction.Type.Reset(prisma.protocol.ResetData())
        val envelope       = prisma.protocol.DatabaseMutaction(projectJson, None, protoMutaction)
        top_level_mutaction_interpreter(envelope, m)

      case m: TopLevelDeleteNode  => DeleteNodeInterpreter(m, shouldDeleteRelayIds = manageRelayIds)
      case m: TopLevelDeleteNodes => DeleteNodesInterpreter(m, shouldDeleteRelayIds = manageRelayIds)
      case m: ResetData           => ResetDataInterpreter(m)
      case m: ImportNodes         => ImportNodesInterpreter(m, shouldCreateRelayIds = manageRelayIds)
      case m: ImportRelations     => ImportRelationsInterpreter(m)
      case m: ImportScalarLists   => ImportScalarListsInterpreter(m)
    }
  }

  def interpreterFor(mutaction: NestedDatabaseMutaction, parentId: IdGCValue): NestedDatabaseMutactionInterpreter = {
    import com.prisma.shared.models.ProjectJsonFormatter._
    val projectJson = ByteString.copyFromUtf8(Json.toJson(mutaction.project).toString())
    val headerName  = mutaction.getClass.getSimpleName
    val prismaId = toPrismaId(parentId)

    val DO_NOT_FORWARD_THIS_ONE = false

    mutaction match {
      case m: NestedCreateNode =>
        val protoMutaction = prisma.protocol.DatabaseMutaction.Type.NestedCreate(
          nestedCreateNodeToProtocol(m)
        )
        val envelope = prisma.protocol.DatabaseMutaction(projectJson, Some(prismaId), protoMutaction)

        val errorHandler: PartialFunction[prisma.protocol.Error.Value, Throwable] = {
          case Error.Value.RelationViolation(rv)    => throw RelationViolation(rv.relationName, rv.modelAName, rv.modelBName)
        }

        nested_mutaction_interpreter(envelope, m, errorHandler)

      case m: NestedUpdateNode if DO_NOT_FORWARD_THIS_ONE =>
        val protoMutaction = prisma.protocol.DatabaseMutaction.Type.NestedUpdate(
          nestedUpdateNodeToProtocol(m)
        )
        val envelope = prisma.protocol.DatabaseMutaction(projectJson, Some(prismaId), protoMutaction)
        nested_mutaction_interpreter(envelope, m)

      case m: NestedUpsertNode if DO_NOT_FORWARD_THIS_ONE =>
        val protoMutaction = prisma.protocol.DatabaseMutaction.Type.NestedUpsert(
          nestedUpsertToProtocol(m)
        )
        val envelope = prisma.protocol.DatabaseMutaction(projectJson, Some(prismaId), protoMutaction)
        nested_mutaction_interpreter(envelope, m)

      case m: NestedDeleteNode if DO_NOT_FORWARD_THIS_ONE =>
        val protoMutaction = prisma.protocol.DatabaseMutaction.Type.NestedDelete(
          nestedDeleteToProtocol(m)
        )
        val envelope = prisma.protocol.DatabaseMutaction(projectJson, Some(prismaId), protoMutaction)
        nested_mutaction_interpreter(envelope, m)

      case m: NestedConnect if DO_NOT_FORWARD_THIS_ONE =>
        val protoMutaction = prisma.protocol.DatabaseMutaction.Type.NestedConnect(
          nestedConnectToProtocol(m)
        )
        val envelope = prisma.protocol.DatabaseMutaction(projectJson, Some(prismaId), protoMutaction)
        nested_mutaction_interpreter(envelope, m)

      case m: NestedDisconnect if DO_NOT_FORWARD_THIS_ONE =>
        val protoMutaction = prisma.protocol.DatabaseMutaction.Type.NestedDisconnect(
          nestedDisconnectToProtocol(m)
        )
        val envelope = prisma.protocol.DatabaseMutaction(projectJson, Some(prismaId), protoMutaction)
        nested_mutaction_interpreter(envelope, m)

      case m: NestedSet if DO_NOT_FORWARD_THIS_ONE =>
        val protoMutaction = prisma.protocol.DatabaseMutaction.Type.NestedSet(
          nestedSetToProtocol(m)
        )
        val envelope = prisma.protocol.DatabaseMutaction(projectJson, Some(prismaId), protoMutaction)
        nested_mutaction_interpreter(envelope, m)

      case m: NestedUpdateNodes if DO_NOT_FORWARD_THIS_ONE =>
        val protoMutaction = prisma.protocol.DatabaseMutaction.Type.NestedUpdateNodes(
          nestedUpdateManyToProtocol(m)
        )
        val envelope = prisma.protocol.DatabaseMutaction(projectJson, Some(prismaId), protoMutaction)
        nested_mutaction_interpreter(envelope, m)

      case m: NestedDeleteNodes if DO_NOT_FORWARD_THIS_ONE =>
        val protoMutaction = prisma.protocol.DatabaseMutaction.Type.NestedDeleteNodes(
          nestedDeleteManyToProtocol(m)
        )
        val envelope = prisma.protocol.DatabaseMutaction(projectJson, Some(prismaId), protoMutaction)
        nested_mutaction_interpreter(envelope, m)

      case m: NestedUpdateNode  => NestedUpdateNodeInterpreter(m)
      case m: NestedUpsertNode  => NestedUpsertNodeInterpreter(m)
      case m: NestedDeleteNode  => NestedDeleteNodeInterpreter(m, shouldDeleteRelayIds = manageRelayIds)
      case m: NestedConnect     => NestedConnectInterpreter(m)
      case m: NestedSet         => NestedSetInterpreter(m)
      case m: NestedDisconnect  => NestedDisconnectInterpreter(m)
      case m: NestedUpdateNodes => NestedUpdateNodesInterpreter(m)
      case m: NestedDeleteNodes => NestedDeleteNodesInterpreter(m, shouldDeleteRelayIds = manageRelayIds)
    }
  }

  private def createNodeToProtocol(m: TopLevelCreateNode) = {
    prisma.protocol.CreateNode(
      header = prisma.protocol.Header(m.getClass.getSimpleName),
      modelName = m.model.name,
      nonListArgs = prismaArgsToProtoclArgs(m.nonListArgs),
      listArgs = listArgsToProtocolArgs(m.listArgs),
      nested = nestedMutactionsToProtocol(m),
    )
  }

  private def nestedCreateNodeToProtocol(m: NestedCreateNode) = {
    prisma.protocol.NestedCreateNode(
      header = prisma.protocol.Header(m.getClass.getSimpleName),
      modelName = m.relationField.model.name,
      fieldName = m.relationField.name,
      nonListArgs = prismaArgsToProtoclArgs(m.nonListArgs),
      listArgs = listArgsToProtocolArgs(m.listArgs),
      topIsCreate = m.topIsCreate,
      nested = nestedMutactionsToProtocol(m),
    )
  }

  private def nestedUpsertToProtocol(m: NestedUpsertNode) = {
    prisma.protocol.NestedUpsertNode(
      header = prisma.protocol.Header(m.getClass.getSimpleName),
      modelName = m.relationField.model.name,
      fieldName = m.relationField.name,
      where = m.where.map(toNodeSelector),
      create = nestedCreateNodeToProtocol(m.create),
      update = nestedUpdateNodeToProtocol(m.update)
    )
  }

  private def nestedDeleteToProtocol(m: NestedDeleteNode) = {
    prisma.protocol.NestedDeleteNode(
      header = prisma.protocol.Header(m.getClass.getSimpleName),
      modelName = m.relationField.model.name,
      fieldName = m.relationField.name,
      where = m.where.map(toNodeSelector)
    )
  }

  private def nestedConnectToProtocol(m: NestedConnect) = {
    prisma.protocol.NestedConnect(
      header = prisma.protocol.Header(m.getClass.getSimpleName),
      modelName = m.relationField.model.name,
      fieldName = m.relationField.name,
      where = toNodeSelector(m.where),
      topIsCreate = m.topIsCreate
    )
  }

  private def nestedDisconnectToProtocol(m: NestedDisconnect) = {
    prisma.protocol.NestedDisconnect(
      header = prisma.protocol.Header(m.getClass.getSimpleName),
      modelName = m.relationField.model.name,
      fieldName = m.relationField.name,
      where = m.where.map(toNodeSelector)
    )
  }

  private def nestedSetToProtocol(m: NestedSet) = {
    prisma.protocol.NestedSet(
      header = prisma.protocol.Header(m.getClass.getSimpleName),
      modelName = m.relationField.model.name,
      fieldName = m.relationField.name,
      wheres = m.wheres.map(toNodeSelector)
    )
  }

  private def nestedUpdateManyToProtocol(m: NestedUpdateNodes) = {
    prisma.protocol.NestedUpdateNodes(
      header = prisma.protocol.Header(m.getClass.getSimpleName),
      modelName = m.relationField.model.name,
      fieldName = m.relationField.name,
      filter = m.whereFilter.map(toProtocolFilter),
      nonListArgs = prismaArgsToProtoclArgs(m.nonListArgs),
      listArgs = listArgsToProtocolArgs(m.listArgs),
    )
  }
  private def nestedDeleteManyToProtocol(m: NestedDeleteNodes) = {
    prisma.protocol.NestedDeleteNodes(
      header = prisma.protocol.Header(m.getClass.getSimpleName),
      modelName = m.relationField.model.name,
      fieldName = m.relationField.name,
      filter = m.whereFilter.map(toProtocolFilter),
    )
  }

  private def updateNodeToProtocol(m: TopLevelUpdateNode) = {
    prisma.protocol.UpdateNode(
      header = prisma.protocol.Header(m.getClass.getSimpleName),
      where = toNodeSelector(m.where),
      nonListArgs = prismaArgsToProtoclArgs(m.nonListArgs),
      listArgs = listArgsToProtocolArgs(m.listArgs),
      nested = nestedMutactionsToProtocol(m),
    )
  }

  private def nestedUpdateNodeToProtocol(m: NestedUpdateNode) = {
    prisma.protocol.NestedUpdateNode(
      header = prisma.protocol.Header(m.getClass.getSimpleName),
      modelName = m.relationField.model.name,
      fieldName = m.relationField.name,
      where = m.where.map(toNodeSelector),
      nonListArgs = prismaArgsToProtoclArgs(m.nonListArgs),
      listArgs = listArgsToProtocolArgs(m.listArgs),
      nested = nestedMutactionsToProtocol(m),
    )
  }

  private def nestedMutactionsToProtocol(m: FurtherNestedMutaction): prisma.protocol.NestedMutactions = {
    prisma.protocol.NestedMutactions(
      creates = m.nestedCreates.map(nestedCreateNodeToProtocol),
      updates = m.nestedUpdates.map(nestedUpdateNodeToProtocol),
      upserts = m.nestedUpserts.map(nestedUpsertToProtocol),
      deletes = m.nestedDeletes.map(nestedDeleteToProtocol),
      connects = m.nestedConnects.map(nestedConnectToProtocol),
      disconnects = m.nestedDisconnects.map(nestedDisconnectToProtocol),
      sets = m.nestedSets.map(nestedSetToProtocol),
      updateManys = m.nestedUpdateManys.map(nestedUpdateManyToProtocol),
      deleteManys = m.nestedDeleteManys.map(nestedDeleteManyToProtocol)
    )
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
      override protected def dbioAction(mutationBuilder: JdbcActionsBuilder): DBIO[DatabaseMutactionResult] = {
        forwarding_dbio(protoMutaction, mutaction, errorHandler)
      }

      override val errorMapper: PartialFunction[Throwable, APIErrors.ClientApiError] = sharedErrorMapper
    }
  }

  private def nested_mutaction_interpreter(
      protoMutaction: prisma.protocol.DatabaseMutaction,
      mutaction: DatabaseMutaction,
      errorHandler: PartialFunction[prisma.protocol.Error.Value, Throwable] = PartialFunction.empty
  ): NestedDatabaseMutactionInterpreter = {

    new NestedDatabaseMutactionInterpreter {
      override protected def dbioAction(mutationBuilder: JdbcActionsBuilder, parentId: IdGCValue): DBIO[DatabaseMutactionResult] = {
        forwarding_dbio(protoMutaction, mutaction, errorHandler)
      }

      override val errorMapper: PartialFunction[Throwable, APIErrors.ClientApiError] = sharedErrorMapper
    }
  }

  private def forwarding_dbio(
      protoMutaction: prisma.protocol.DatabaseMutaction,
      mutaction: DatabaseMutaction,
      errorHandler: PartialFunction[prisma.protocol.Error.Value, Throwable] = PartialFunction.empty
  ): DBIO[DatabaseMutactionResult] = {
    SimpleDBIO { _ =>
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

  private val sharedErrorMapper: PartialFunction[Throwable, APIErrors.ClientApiError] = {
    case UniqueConstraintViolation(fieldName) => {
      val splitted = fieldName.split('.')
      APIErrors.UniqueConstraintViolation(splitted(0), "Field name = " + splitted(1))
    }
    case NodeNotFoundForWhere(modelName, fieldName, value) => {
      APIErrors.NodeNotFoundForWhereErrorNative(modelName, value, fieldName)
    }
    case RelationViolation(relationName, modelAName, modelBName) => {
      APIErrors.RequiredRelationWouldBeViolatedNative(relationName, modelAName, modelBName)
    }
    case FieldCannotBeNull(fieldName) => {
      APIErrors.FieldCannotBeNull(fieldName)
    }
  }

  private def runAttached[T](project: Project, query: DBIO[T]) = {
    if (slickDatabaseArg.isSQLite) {
      import slickDatabaseArg.profile.api._

      val list               = sql"""PRAGMA database_list;""".as[(String, String, String)]
      val path               = s"""'db/${project.dbName}.db'"""
      val attach             = sqlu"ATTACH DATABASE #$path AS #${project.dbName};"
      val activateForeignKey = sqlu"""PRAGMA foreign_keys = ON;"""

      val attachIfNecessary = for {
        attachedDbs <- list
        _ <- attachedDbs.map(_._2).contains(project.dbName) match {
              case true  => slick.dbio.DBIO.successful(())
              case false => attach
            }
        _      <- activateForeignKey
        result <- query
      } yield result

      slickDatabaseArg.database.run(attachIfNecessary.withPinnedSession)
    } else {
      slickDatabaseArg.database.run(query)
    }
  }
}
