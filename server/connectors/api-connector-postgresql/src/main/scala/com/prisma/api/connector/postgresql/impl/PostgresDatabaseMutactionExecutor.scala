package com.prisma.api.connector.postgresql.impl

import java.util.concurrent.Executors

import com.prisma.api.connector._
import com.prisma.api.connector.postgresql.DatabaseMutactionInterpreter
import com.prisma.api.connector.postgresql.database.PostgresApiDatabaseMutationBuilder
import com.prisma.api.schema.UserFacingError
import com.prisma.gc_values.{CuidGCValue, IdGCValue}
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}

case class PostgresDatabaseMutactionExecutor(clientDb: Database, createRelayIds: Boolean)(implicit ec: ExecutionContext) extends DatabaseMutactionExecutor {

  override def executeTransactionally(mutaction: TopLevelDatabaseMutaction) = execute(mutaction, transactionally = true)

  override def executeNonTransactionally(mutaction: TopLevelDatabaseMutaction) = execute(mutaction, transactionally = false)

  private def execute(mutaction: TopLevelDatabaseMutaction, transactionally: Boolean): Future[DatabaseMutactionResult] = {
    val mutationBuilder = PostgresApiDatabaseMutationBuilder(schemaName = mutaction.project.id)
    // fixme: handing in those non existent values should not happen
    val singleAction = transactionally match {
      case true  => recurse(mutaction, CuidGCValue("does-not-exist"), mutationBuilder).transactionally
      case false => recurse(mutaction, CuidGCValue("does-not-exist"), mutationBuilder)
    }

    clientDb.run(singleAction)
  }

  // FIXME: the recursion part may deadlock when no dedicated ec is used. This can be observed with test cases in DeadlockSpec that use nested mutations.
  private val recurseEc = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10))

  def recurse(
      mutaction: DatabaseMutaction,
      parentId: IdGCValue,
      mutationBuilder: PostgresApiDatabaseMutationBuilder
  ): DBIO[DatabaseMutactionResult] = {
    mutaction match {
      case m: FurtherNestedMutaction =>
        for {
          result <- interpreterFor(m).newActionWithErrorMapped(mutationBuilder, parentId)(recurseEc)
          childResults <- result.id match {
                           case Some(id) => DBIO.sequence(m.allMutactions.map(recurse(_, id, mutationBuilder)))
                           case None     => DBIO.successful(())
                         }
        } yield result
      case m: FinalMutaction =>
        for {
          result <- interpreterFor(m).newActionWithErrorMapped(mutationBuilder, parentId)(recurseEc)
        } yield result
    }
  }

  def interpreterFor(mutaction: DatabaseMutaction): DatabaseMutactionInterpreter = mutaction match {
    case m: AddDataItemToManyRelationByPath   => AddDataItemToManyRelationByPathInterpreter(m)
    case m: CascadingDeleteRelationMutactions => CascadingDeleteRelationMutactionsInterpreter(m)
    case m: CreateDataItem                    => CreateDataItemInterpreter(mutaction = m, includeRelayRow = createRelayIds)
    case m: DeleteDataItem                    => DeleteDataItemInterpreter(m)
    case m: NestedDeleteDataItem              => DeleteDataItemNestedInterpreter(m)
    case m: DeleteDataItems                   => DeleteDataItemsInterpreter(m)
    case m: DeleteManyRelationChecks          => DeleteManyRelationChecksInterpreter(m)
    case m: DeleteRelationCheck               => DeleteRelationCheckInterpreter(m)
    case m: NestedConnectRelation             => NestedConnectRelationInterpreter(m)
    case m: NestedCreateDataItem              => NestedCreateDataItemInterpreter(m)
    case m: NestedDisconnectRelation          => NestedDisconnectRelationInterpreter(m)
    case m: ResetDataMutaction                => ResetDataInterpreter(m)
    case m: UpdateDataItem                    => UpdateDataItemInterpreter(m)
    case m: NestedUpdateDataItem              => NestedUpdateDataItemInterpreter(m)
    case m: UpdateDataItems                   => UpdateDataItemsInterpreter(m)
    case m: UpsertDataItem                    => UpsertDataItemInterpreter(m, this)
    case m: NestedUpsertDataItem              => NestedUpsertDataItemInterpreter(m, this)
    case m: VerifyConnection                  => VerifyConnectionInterpreter(m)
    case m: VerifyWhere                       => VerifyWhereInterpreter(m)
    case m: CreateDataItemsImport             => CreateDataItemsImportInterpreter(m)
    case m: CreateRelationRowsImport          => CreateRelationRowsImportInterpreter(m)
    case m: PushScalarListsImport             => PushScalarListsImportInterpreter(m)
  }
}
