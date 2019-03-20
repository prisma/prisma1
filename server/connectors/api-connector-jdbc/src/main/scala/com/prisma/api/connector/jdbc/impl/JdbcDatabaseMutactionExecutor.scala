package com.prisma.api.connector.jdbc.impl

import com.prisma.api.connector._
import com.prisma.api.connector.jdbc.database.JdbcActionsBuilder
import com.prisma.api.connector.jdbc.{NestedDatabaseMutactionInterpreter, TopLevelDatabaseMutactionInterpreter}
import com.prisma.connector.shared.jdbc.SlickDatabase
import com.prisma.gc_values.IdGCValue
import com.prisma.shared.models.Project
import play.api.libs.json.JsValue
import slick.dbio.DBIO._
import slick.jdbc.TransactionIsolation

import scala.concurrent.{ExecutionContext, Future}

case class JdbcDatabaseMutactionExecutor(
    slickDatabase: SlickDatabase,
    manageRelayIds: Boolean
)(implicit ec: ExecutionContext)
    extends DatabaseMutactionExecutor {
  import slickDatabase.profile.api._

  override def executeRaw(project: Project, query: String): Future[JsValue] = {
    val action = JdbcActionsBuilder(project, slickDatabase).executeRaw(query)
    runAttached(project, action)
  }

  override def executeTransactionally(mutaction: TopLevelDatabaseMutaction) = execute(mutaction, transactionally = true)

  override def executeNonTransactionally(mutaction: TopLevelDatabaseMutaction) = execute(mutaction, transactionally = false)

  private def execute(mutaction: TopLevelDatabaseMutaction, transactionally: Boolean): Future[MutactionResults] = {

    val actionsBuilder = JdbcActionsBuilder(mutaction.project, slickDatabase)
    val singleAction = transactionally match {
      case true  => executeTopLevelMutaction(mutaction, actionsBuilder).transactionally
      case false => executeTopLevelMutaction(mutaction, actionsBuilder)
    }

    val finalAction = if (slickDatabase.isMySql) singleAction.withTransactionIsolation(TransactionIsolation.ReadCommitted) else singleAction

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
          childResults <- executeTopLevelMutaction(result.asInstanceOf[UpsertNodeResult].result.asInstanceOf[TopLevelDatabaseMutaction], mutationBuilder)
                           .map(Vector(_))
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
          result <- interpreterFor(m).dbioActionWithErrorMapped(mutationBuilder, parentId)
          childResults <- executeNestedMutaction(result.asInstanceOf[UpsertNodeResult].result.asInstanceOf[NestedDatabaseMutaction], parentId, mutationBuilder)
                           .map(Vector(_))
        } yield MutactionResults(result +: childResults.flatMap(_.results))

      case m: FurtherNestedMutaction =>
        for {
          result <- interpreterFor(m).dbioActionWithErrorMapped(mutationBuilder, parentId)
          childResults <- result match {
                           case result: FurtherNestedMutactionResult =>
                             DBIO.sequence(m.allNestedMutactions.map(executeNestedMutaction(_, result.id, mutationBuilder)))
                           case _ => DBIO.successful(Vector.empty)
                         }
        } yield MutactionResults(result +: childResults.flatMap(_.results))

      case m: FinalMutaction =>
        for {
          result <- interpreterFor(m).dbioActionWithErrorMapped(mutationBuilder, parentId)
        } yield MutactionResults(Vector(result))
    }
  }

  def interpreterFor(mutaction: TopLevelDatabaseMutaction): TopLevelDatabaseMutactionInterpreter = mutaction match {
    case m: TopLevelCreateNode  => CreateNodeInterpreter(mutaction = m, includeRelayRow = manageRelayIds)
    case m: TopLevelUpdateNode  => UpdateNodeInterpreter(m)
    case m: TopLevelUpsertNode  => UpsertNodeInterpreter(m)
    case m: TopLevelDeleteNode  => DeleteNodeInterpreter(m, shouldDeleteRelayIds = manageRelayIds)
    case m: TopLevelUpdateNodes => UpdateNodesInterpreter(m)
    case m: TopLevelDeleteNodes => DeleteNodesInterpreter(m, shouldDeleteRelayIds = manageRelayIds)
    case m: ResetData           => ResetDataInterpreter(m)
    case m: ImportNodes         => ImportNodesInterpreter(m, shouldCreateRelayIds = manageRelayIds)
    case m: ImportRelations     => ImportRelationsInterpreter(m)
    case m: ImportScalarLists   => ImportScalarListsInterpreter(m)
  }

  def interpreterFor(mutaction: NestedDatabaseMutaction): NestedDatabaseMutactionInterpreter = mutaction match {
    case m: NestedCreateNode  => NestedCreateNodeInterpreter(m, includeRelayRow = manageRelayIds)
    case m: NestedUpdateNode  => NestedUpdateNodeInterpreter(m)
    case m: NestedUpsertNode  => NestedUpsertNodeInterpreter(m)
    case m: NestedDeleteNode  => NestedDeleteNodeInterpreter(m, shouldDeleteRelayIds = manageRelayIds)
    case m: NestedConnect     => NestedConnectInterpreter(m)
    case m: NestedSet         => NestedSetInterpreter(m)
    case m: NestedDisconnect  => NestedDisconnectInterpreter(m)
    case m: NestedUpdateNodes => NestedUpdateNodesInterpreter(m)
    case m: NestedDeleteNodes => NestedDeleteNodesInterpreter(m, shouldDeleteRelayIds = manageRelayIds)
  }

  private def runAttached[T](project: Project, query: DBIO[T]) = {
    if (slickDatabase.isSQLite) {
      import slickDatabase.profile.api._

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

      slickDatabase.database.run(attachIfNecessary.withPinnedSession)
    } else {
      slickDatabase.database.run(query)
    }
  }
}
