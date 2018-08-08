package com.prisma.api.connector.jdbc.impl

import com.prisma.api.connector._
import com.prisma.api.connector.jdbc.database.{JdbcActionsBuilder, SlickDatabase}
import com.prisma.api.connector.jdbc.{NestedDatabaseMutactionInterpreter, TopLevelDatabaseMutactionInterpreter}
import com.prisma.gc_values.IdGCValue
import slick.jdbc.TransactionIsolation

import scala.concurrent.{ExecutionContext, Future}

case class JdbcDatabaseMutactionExecutor(
    slickDatabase: SlickDatabase,
    isActive: Boolean
)(implicit ec: ExecutionContext)
    extends DatabaseMutactionExecutor {
  import slickDatabase.profile.api._

  override def executeTransactionally(mutaction: TopLevelDatabaseMutaction) = execute(mutaction, transactionally = true)

  override def executeNonTransactionally(mutaction: TopLevelDatabaseMutaction) = execute(mutaction, transactionally = false)

  private def execute(mutaction: TopLevelDatabaseMutaction, transactionally: Boolean): Future[MutactionResults] = {
    val actionsBuilder = JdbcActionsBuilder(schemaName = mutaction.project.id, slickDatabase)
    val singleAction = transactionally match {
      case true  => executeTopLevelMutaction(mutaction, actionsBuilder).transactionally
      case false => executeTopLevelMutaction(mutaction, actionsBuilder)
    }

    if (slickDatabase.isMySql) {
      slickDatabase.database.run(singleAction.withTransactionIsolation(TransactionIsolation.ReadCommitted))
    } else if (slickDatabase.isPostgres) {
      slickDatabase.database.run(singleAction)
    } else {
      sys.error("No valid database profile given.")
    }
  }

  def executeTopLevelMutaction(
      mutaction: TopLevelDatabaseMutaction,
      mutationBuilder: JdbcActionsBuilder
  ): DBIO[MutactionResults] = {
    mutaction match {
      case m: TopLevelUpsertNode =>
        for {
          result <- interpreterFor(m).dbioActionWithErrorMapped(mutationBuilder)
          childResults <- executeTopLevelMutaction(result.asInstanceOf[UpsertNodeResult].result.asInstanceOf[TopLevelDatabaseMutaction], mutationBuilder)
                           .map(Vector(_))
        } yield MutactionResults(result, childResults)

      case m: FurtherNestedMutaction =>
        for {
          result <- interpreterFor(m).dbioActionWithErrorMapped(mutationBuilder)
          childResults <- result match {
                           case result: FurtherNestedMutactionResult =>
                             DBIO.sequence(m.allNestedMutactions.map(executeNestedMutaction(_, result.id, mutationBuilder)))
                           case _ => DBIO.successful(Vector.empty)
                         }
        } yield MutactionResults(result, childResults)

      case m: FinalMutaction =>
        for {
          result <- interpreterFor(m).dbioActionWithErrorMapped(mutationBuilder)
        } yield MutactionResults(result, Vector.empty)
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
          result <- interpreterFor(m).dbioActionWithErrorMapped(mutationBuilder, parentId)
          childResults <- executeNestedMutaction(result.asInstanceOf[UpsertNodeResult].result.asInstanceOf[NestedDatabaseMutaction], parentId, mutationBuilder)
                           .map(Vector(_))
        } yield MutactionResults(result, childResults)

      case m: FurtherNestedMutaction =>
        for {
          result <- interpreterFor(m).dbioActionWithErrorMapped(mutationBuilder, parentId)
          childResults <- result match {
                           case result: FurtherNestedMutactionResult =>
                             DBIO.sequence(m.allNestedMutactions.map(executeNestedMutaction(_, result.id, mutationBuilder)))
                           case _ => DBIO.successful(Vector.empty)
                         }
        } yield MutactionResults(result, childResults)

      case m: FinalMutaction =>
        for {
          result <- interpreterFor(m).dbioActionWithErrorMapped(mutationBuilder, parentId)
        } yield MutactionResults(result, Vector.empty)
    }
  }

  def interpreterFor(mutaction: TopLevelDatabaseMutaction): TopLevelDatabaseMutactionInterpreter = mutaction match {
    case m: TopLevelCreateNode => CreateNodeInterpreter(mutaction = m, includeRelayRow = isActive)
    case m: TopLevelUpdateNode => UpdateNodeInterpreter(m)
    case m: TopLevelUpsertNode => UpsertDataItemInterpreter(m)
    case m: TopLevelDeleteNode => DeleteNodeInterpreter(m, shouldDeleteRelayIds = isActive)
    case m: UpdateNodes        => UpdateDataItemsInterpreter(m)
    case m: DeleteNodes        => DeleteDataItemsInterpreter(m, shouldDeleteRelayIds = isActive)
    case m: ResetData          => ResetDataInterpreter(m)
    case m: ImportNodes        => ImportNodesInterpreter(m)
    case m: ImportRelations    => ImportRelationsInterpreter(m)
    case m: ImportScalarLists  => ImportScalarListsInterpreter(m)
  }

  def interpreterFor(mutaction: NestedDatabaseMutaction): NestedDatabaseMutactionInterpreter = mutaction match {
    case m: NestedCreateNode => NestedCreateNodeInterpreter(m, includeRelayRow = isActive)
    case m: NestedUpdateNode => NestedUpdateNodeInterpreter(m)
    case m: NestedUpsertNode => NestedUpsertDataItemInterpreter(m)
    case m: NestedDeleteNode => NestedDeleteNodeInterpreter(m, shouldDeleteRelayIds = isActive)
    case m: NestedConnect    => NestedConnectInterpreter(m)
    case m: NestedDisconnect => NestedDisconnectInterpreter(m)
  }
}
