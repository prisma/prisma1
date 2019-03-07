package com.prisma.api.connector.mongo.impl

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.database._
import com.prisma.api.connector.mongo.extensions.SlickReplacement._
import com.prisma.api.connector.mongo.{NestedDatabaseMutactionInterpreter, TopLevelDatabaseMutactionInterpreter}
import com.prisma.shared.models.Project
import org.mongodb.scala.{MongoClient, MongoDatabase}
import play.api.libs.json.{JsValue, Json}

import scala.concurrent.{ExecutionContext, Future}

class MongoDatabaseMutactionExecutor(client: MongoClient)(implicit ec: ExecutionContext) extends DatabaseMutactionExecutor {

  override def executeTransactionally(mutaction: TopLevelDatabaseMutaction): Future[MutactionResults] = execute(mutaction, transactionally = true)

  override def executeNonTransactionally(mutaction: TopLevelDatabaseMutaction): Future[MutactionResults] = execute(mutaction, transactionally = false)

  private def execute(mutaction: TopLevelDatabaseMutaction, transactionally: Boolean): Future[MutactionResults] = {
    val actionsBuilder = MongoActionsBuilder(mutaction.project.dbName, client)
    val action         = generateTopLevelMutaction(actionsBuilder.database, mutaction, actionsBuilder)

    run(actionsBuilder.database, action)
  }

  def generateTopLevelMutaction(
      database: MongoDatabase,
      mutaction: TopLevelDatabaseMutaction,
      mutationBuilder: MongoActionsBuilder
  ): MongoAction[MutactionResults] = {
    mutaction match {
      case m: TopLevelUpsertNode =>
        for {
          result <- interpreterFor(m).mongoActionWithErrorMapped(mutationBuilder)
          childResult <- generateTopLevelMutaction(database,
                                                   result.results.head.asInstanceOf[UpsertNodeResult].result.asInstanceOf[TopLevelDatabaseMutaction],
                                                   mutationBuilder)
        } yield result.merge(childResult)

      case m: FurtherNestedMutaction =>
        for {
          result <- interpreterFor(m).mongoActionWithErrorMapped(mutationBuilder)
          childResults <- result match {
                           case results: MutactionResults =>
                             val nestedMutactions =
                               m.allNestedMutactions.map(x => generateNestedMutaction(database, x, results, results.nodeAddress(m), mutationBuilder))
                             MongoAction.seq(nestedMutactions)
                           case _ => MongoAction.successful(Vector.empty)
                         }
        } yield result.merge(childResults)

      case m: FinalMutaction =>
        for {
          result <- interpreterFor(m).mongoActionWithErrorMapped(mutationBuilder)
        } yield result

      case _ => sys.error("not implemented yet")
    }
  }

  def generateNestedMutaction(
      database: MongoDatabase,
      mutaction: NestedDatabaseMutaction,
      previousResults: MutactionResults,
      parent: NodeAddress,
      mutationBuilder: MongoActionsBuilder
  ): MongoAction[MutactionResults] = {
    mutaction match {
      case m: NestedUpsertNode =>
        if (previousResults.contains(m)) {
          val result = getResultForMutactionFromPreviousResults(previousResults, m)
          for {
            childResult <- generateNestedMutaction(
                            database,
                            result.asInstanceOf[UpsertNodeResult].result.asInstanceOf[NestedDatabaseMutaction],
                            previousResults,
                            parent,
                            mutationBuilder
                          )
          } yield result.merge(childResult)

        } else {
          for {
            result <- interpreterFor(m).mongoActionWithErrorMapped(mutationBuilder, parent)
            childResult <- generateNestedMutaction(
                            database,
                            result.results.head.asInstanceOf[UpsertNodeResult].result.asInstanceOf[NestedDatabaseMutaction],
                            previousResults.merge(result),
                            parent,
                            mutationBuilder
                          )
          } yield result.merge(childResult)
        }
      case m: FurtherNestedMutaction =>
        if (previousResults.contains(m)) {
          val result: DatabaseMutactionResult = getResultForMutactionFromPreviousResults(previousResults, m)

          val nestedMutactions =
            m.allNestedMutactions.map(x => generateNestedMutaction(database, x, previousResults, previousResults.nodeAddress(m), mutationBuilder))

          for {
            childResults <- MongoAction.seq(nestedMutactions)
          } yield result.merge(childResults)

        } else {
          for {
            result <- interpreterFor(m).mongoActionWithErrorMapped(mutationBuilder, parent)
            childResults <- result match {
                             case results: MutactionResults =>
                               val nestedMutactions =
                                 m.allNestedMutactions.map(x =>
                                   generateNestedMutaction(database, x, previousResults.merge(result), results.nodeAddress(m), mutationBuilder))
                               MongoAction.seq(nestedMutactions)
                             case _ => MongoAction.successful(Vector.empty)
                           }
          } yield result.merge(childResults)
        }
      case m: FinalMutaction =>
        if (previousResults.contains(m)) {
          val result = getResultForMutactionFromPreviousResults(previousResults, m)
          MongoAction.successful(MutactionResults(Vector(result)))
        } else {
          for {
            result <- interpreterFor(m).mongoActionWithErrorMapped(mutationBuilder, parent)
          } yield result
        }
      case _ => sys.error("not implemented yet")
    }
  }

  private def getResultForMutactionFromPreviousResults(previousResults: MutactionResults, m: DatabaseMutaction) = {
    previousResults.results.find(res => res.mutaction.id == m.id).get
  }

  def interpreterFor(mutaction: TopLevelDatabaseMutaction): TopLevelDatabaseMutactionInterpreter = mutaction match {
    case m: TopLevelCreateNode  => CreateNodeInterpreter(mutaction = m)
    case m: TopLevelUpdateNode  => UpdateNodeInterpreter(mutaction = m)
    case m: TopLevelUpsertNode  => UpsertNodeInterpreter(mutaction = m)
    case m: TopLevelDeleteNode  => DeleteNodeInterpreter(mutaction = m)
    case m: TopLevelUpdateNodes => UpdateNodesInterpreter(mutaction = m)
    case m: TopLevelDeleteNodes => DeleteNodesInterpreter(mutaction = m)
    case m: ResetData           => ResetDataInterpreter(mutaction = m)
    case m: ImportNodes         => ??? //delayed
    case m: ImportRelations     => ??? //delayed
    case m: ImportScalarLists   => ??? //delayed
  }

  //for embedded types none of these should actually fire since they should be embedded in their toplevel actions
  def interpreterFor(mutaction: NestedDatabaseMutaction): NestedDatabaseMutactionInterpreter = mutaction match {
    case m: NestedCreateNode  => NestedCreateNodeInterpreter(mutaction = m)
    case m: NestedUpdateNode  => NestedUpdateNodeInterpreter(mutaction = m)
    case m: NestedUpsertNode  => NestedUpsertNodeInterpreter(mutaction = m)
    case m: NestedDeleteNode  => NestedDeleteNodeInterpreter(mutaction = m)
    case m: NestedConnect     => NestedConnectInterpreter(mutaction = m)
    case m: NestedSet         => NestedSetInterpreter(mutaction = m)
    case m: NestedDisconnect  => NestedDisconnectInterpreter(mutaction = m)
    case m: NestedUpdateNodes => NestedUpdateNodesInterpreter(mutaction = m)
    case m: NestedDeleteNodes => NestedDeleteNodesInterpreter(mutaction = m)
  }

  override def executeRaw(project: Project, query: String): Future[JsValue] = Future.successful(Json.obj("notImplemented" -> true))
}
