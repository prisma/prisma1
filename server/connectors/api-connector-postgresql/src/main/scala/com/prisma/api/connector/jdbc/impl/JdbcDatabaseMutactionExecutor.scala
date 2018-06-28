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
      case m: FurtherNestedMutaction =>
        for {
          result <- interpreterFor(m).newActionWithErrorMapped(mutationBuilder, parentId)
          childResults <- result match {
                           case result: UpsertDataItemResult         => recurse(result.mutaction, parentId, mutationBuilder).map(Vector(_))
                           case result: FurtherNestedMutactionResult => DBIO.sequence(m.allMutactions.map(recurse(_, result.id, mutationBuilder)))
                           case _                                    => DBIO.successful(Vector.empty)
                         }
        } yield {
          MutactionResults(
            databaseResult = result,
            nestedResults = childResults
          )
        }
      case m: FinalMutaction =>
        for {
          result <- interpreterFor(m).newActionWithErrorMapped(mutationBuilder, parentId)
        } yield {
          MutactionResults(
            databaseResult = result,
            nestedResults = Vector.empty
          )
        }
    }
  }

  def interpreterFor(mutaction: DatabaseMutaction): DatabaseMutactionInterpreter = mutaction match {
    case m: CreateDataItem           => CreateDataItemInterpreter(mutaction = m, includeRelayRow = createRelayIds)
    case m: DeleteDataItem           => DeleteDataItemInterpreter(m)
    case m: NestedDeleteDataItem     => DeleteDataItemNestedInterpreter(m)
    case m: DeleteDataItems          => DeleteDataItemsInterpreter(m)
    case m: NestedConnectRelation    => NestedConnectRelationInterpreter(m)
    case m: NestedCreateDataItem     => NestedCreateDataItemInterpreter(m, includeRelayRow = createRelayIds)
    case m: NestedDisconnectRelation => NestedDisconnectRelationInterpreter(m)
    case m: ResetDataMutaction       => ResetDataInterpreter(m)
    case m: UpdateDataItem           => UpdateDataItemInterpreter(m)
    case m: NestedUpdateDataItem     => NestedUpdateDataItemInterpreter(m)
    case m: UpdateDataItems          => UpdateDataItemsInterpreter(m)
    case m: UpsertDataItem           => UpsertDataItemInterpreter(m)
    case m: NestedUpsertDataItem     => NestedUpsertDataItemInterpreter(m)
    case m: CreateDataItemsImport    => CreateDataItemsImportInterpreter(m)
    case m: CreateRelationRowsImport => CreateRelationRowsImportInterpreter(m)
    case m: PushScalarListsImport    => PushScalarListsImportInterpreter(m)
  }
}
