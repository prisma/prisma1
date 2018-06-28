package com.prisma.api.connector.jdbc.impl

import com.prisma.api.connector._
import com.prisma.api.connector.jdbc.DatabaseMutactionInterpreter
import com.prisma.api.connector.jdbc.database.{PostgresApiDatabaseMutationBuilder, SlickDatabase}
import com.prisma.gc_values.{CuidGCValue, IdGCValue}

import scala.concurrent.{ExecutionContext, Future}

case class JdbcDatabaseMutactionExecutor(
    slickDatabase: SlickDatabase,
    createRelayIds: Boolean
)(implicit ec: ExecutionContext)
    extends DatabaseMutactionExecutor {
  import slickDatabase.profile.api._

  override def executeTransactionally(mutaction: TopLevelDatabaseMutaction) = execute(mutaction, transactionally = true)

  override def executeNonTransactionally(mutaction: TopLevelDatabaseMutaction) = execute(mutaction, transactionally = false)

  private def execute(mutaction: TopLevelDatabaseMutaction, transactionally: Boolean): Future[MutactionResults] = {
    val mutationBuilder = PostgresApiDatabaseMutationBuilder(schemaName = mutaction.project.id, slickDatabase)
    // fixme: handing in those non existent values should not happen
    val singleAction = transactionally match {
      case true  => recurse(mutaction, CuidGCValue("does-not-exist"), mutationBuilder).transactionally
      case false => recurse(mutaction, CuidGCValue("does-not-exist"), mutationBuilder)
    }

    slickDatabase.database.run(singleAction)
  }

  def recurse(
      mutaction: DatabaseMutaction,
      parentId: IdGCValue,
      mutationBuilder: PostgresApiDatabaseMutationBuilder
  ): DBIO[MutactionResults] = {
    mutaction match {
      case m: UpsertNode =>
        for {
          result       <- interpreterFor(m).dbioActionWithErrorMapped(mutationBuilder, parentId)
          childResults <- recurse(result.asInstanceOf[UpsertNodeResult].result, parentId, mutationBuilder).map(Vector(_))
        } yield MutactionResults(result, childResults)

      case m: FurtherNestedMutaction =>
        for {
          result <- interpreterFor(m).dbioActionWithErrorMapped(mutationBuilder, parentId)
          childResults <- result match {
                           case result: FurtherNestedMutactionResult => DBIO.sequence(m.allNestedMutactions.map(recurse(_, result.id, mutationBuilder)))
                           case _                                    => DBIO.successful(Vector.empty)
                         }
        } yield MutactionResults(result, childResults)

      case m: FinalMutaction =>
        for {
          result <- interpreterFor(m).dbioActionWithErrorMapped(mutationBuilder, parentId)
        } yield MutactionResults(result, Vector.empty)
    }
  }

  def interpreterFor(mutaction: DatabaseMutaction): DatabaseMutactionInterpreter = mutaction match {
    case m: TopLevelCreateNode => CreateNodeInterpreter(mutaction = m, includeRelayRow = createRelayIds)
    case m: NestedCreateNode   => NestedCreateNodeInterpreter(m, includeRelayRow = createRelayIds)
    case m: TopLevelUpdateNode => UpdateNodeInterpreter(m)
    case m: NestedUpdateNode   => NestedUpdateNodeInterpreter(m)
    case m: TopLevelUpsertNode => UpsertDataItemInterpreter(m)
    case m: NestedUpsertNode   => NestedUpsertDataItemInterpreter(m)
    case m: TopLevelDeleteNode => DeleteNodeInterpreter(m)
    case m: NestedDeleteNode   => NestedDeleteNodeInterpreter(m)
    case m: NestedConnect      => NestedConnectInterpreter(m)
    case m: NestedDisconnect   => NestedDisconnectInterpreter(m)
    case m: DeleteNodes        => DeleteDataItemsInterpreter(m)
    case m: ResetData          => ResetDataInterpreter(m)
    case m: UpdateNodes        => UpdateDataItemsInterpreter(m)
    case m: ImportNodes        => ImportNodesInterpreter(m)
    case m: ImportRelations    => ImportRelationsInterpreter(m)
    case m: ImportScalarLists  => ImportScalarListsInterpreter(m)
  }
}
