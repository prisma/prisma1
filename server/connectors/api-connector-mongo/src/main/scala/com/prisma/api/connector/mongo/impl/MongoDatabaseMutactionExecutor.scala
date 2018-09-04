package com.prisma.api.connector.mongo.impl

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.database._
import com.prisma.api.connector.mongo.extensions.SlickReplacement._
import com.prisma.api.connector.mongo.{NestedDatabaseMutactionInterpreter, TopLevelDatabaseMutactionInterpreter}
import com.prisma.gc_values.IdGCValue
import org.mongodb.scala.{MongoClient, MongoDatabase}
import play.api.libs.json.JsValue

import scala.concurrent.{ExecutionContext, Future}

class MongoDatabaseMutactionExecutor(client: MongoClient)(implicit ec: ExecutionContext) extends DatabaseMutactionExecutor {

  override def executeTransactionally(mutaction: TopLevelDatabaseMutaction): Future[MutactionResults] = execute(mutaction, transactionally = true)

  override def executeNonTransactionally(mutaction: TopLevelDatabaseMutaction): Future[MutactionResults] = execute(mutaction, transactionally = false)

  private def execute(mutaction: TopLevelDatabaseMutaction, transactionally: Boolean): Future[MutactionResults] = {
    val actionsBuilder = MongoActionsBuilder(mutaction.project.id, client)
    val action         = generateTopLevelMutaction(actionsBuilder.database, mutaction, actionsBuilder)

    run(actionsBuilder.database, action)
  }

  def generateNestedMutaction(
      database: MongoDatabase,
      mutaction: NestedDatabaseMutaction,
      parentId: IdGCValue,
      mutationBuilder: MongoActionsBuilder
  ): MongoAction[MutactionResults] = {
    generateMutaction[NestedDatabaseMutaction](database, mutaction, mutationBuilder, mut => interpreterFor(mut).mongoAction(mutationBuilder, parentId))
  }

  def generateTopLevelMutaction(
      database: MongoDatabase,
      mutaction: TopLevelDatabaseMutaction,
      mutationBuilder: MongoActionsBuilder
  ): MongoAction[MutactionResults] = {
    generateMutaction[TopLevelDatabaseMutaction](database, mutaction, mutationBuilder, mut => interpreterFor(mut).mongoAction(mutationBuilder))
  }

  def generateMutaction[T <: DatabaseMutaction](
      database: MongoDatabase,
      mutaction: T,
      mutationBuilder: MongoActionsBuilder,
      fn: T => MongoAction[MutactionResults]
  ): MongoAction[MutactionResults] = {
    mutaction match {
      case m: FurtherNestedMutaction =>
        for {
          result <- fn(mutaction)
          childResults <- result match {
                           case results: MutactionResults =>
                             val stillToExecute = m.allNestedMutactions diff results.results.map(_.mutaction)
                             val resultOfM      = results.results.find(_.mutaction == m).get.asInstanceOf[FurtherNestedMutactionResult]

                             val nestedMutactionsStillToRun = stillToExecute.map(x => generateNestedMutaction(database, x, resultOfM.id, mutationBuilder))
                             MongoAction.seq(nestedMutactionsStillToRun)
                           case _ => MongoAction.successful(Vector.empty)
                         }
        } yield MutactionResults(result.results ++ childResults.flatMap(_.results))

      case m: FinalMutaction =>
        for {
          result <- fn(mutaction)
        } yield result

      case _ => sys.error("not implemented yet")
    }
  }

  def interpreterFor(mutaction: TopLevelDatabaseMutaction): TopLevelDatabaseMutactionInterpreter = mutaction match {
    case m: TopLevelCreateNode => CreateNodeInterpreter(mutaction = m, includeRelayRow = false)
    case m: TopLevelUpdateNode => UpdateNodeInterpreter(mutaction = m)
    case m: TopLevelUpsertNode => ??? //delayed
    case m: TopLevelDeleteNode => DeleteNodeInterpreter(mutaction = m, shouldDeleteRelayIds = false)
    case m: UpdateNodes        => UpdateNodesInterpreter(mutaction = m)
    case m: DeleteNodes        => DeleteNodesInterpreter(mutaction = m, shouldDeleteRelayIds = false)
    case m: ResetData          => ResetDataInterpreter(mutaction = m)
    case m: ImportNodes        => ??? //delayed
    case m: ImportRelations    => ??? //delayed
    case m: ImportScalarLists  => ??? //delayed
  }

  //for embedded types none of these should actually fire since they should be embedded in their toplevel actions
  def interpreterFor(mutaction: NestedDatabaseMutaction): NestedDatabaseMutactionInterpreter = mutaction match {
    case m: NestedCreateNode => NestedCreateNodeInterpreter(mutaction = m, includeRelayRow = false)
    case m: NestedUpdateNode => NestedUpdateNodeInterpreter(mutaction = m)
    case m: NestedUpsertNode => ??? //delayed
    case m: NestedDeleteNode => NestedDeleteNodeInterpreter(mutaction = m, shouldDeleteRelayIds = false)
    case m: NestedConnect    => ??? //delayed
    case m: NestedDisconnect => ??? //delayed
  }

  override def executeRaw(query: String): Future[JsValue] = ???
}
