package com.prisma.api.connector.mongo

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.database._
import com.prisma.api.connector.mongo.impl.{CreateNodeInterpreter, DeleteNodeInterpreter, ResetDataInterpreter, UpdateNodeInterpreter}
import com.prisma.gc_values.IdGCValue
import org.mongodb.scala.{MongoClient, MongoDatabase}

import scala.concurrent.{ExecutionContext, Future}

class MongoDatabaseMutactionExecutor(client: MongoClient)(implicit ec: ExecutionContext) extends DatabaseMutactionExecutor {

  override def executeTransactionally(mutaction: TopLevelDatabaseMutaction): Future[MutactionResults] = execute(mutaction, transactionally = true)

  override def executeNonTransactionally(mutaction: TopLevelDatabaseMutaction): Future[MutactionResults] = execute(mutaction, transactionally = false)

  private def execute(mutaction: TopLevelDatabaseMutaction, transactionally: Boolean): Future[MutactionResults] = {
    val actionsBuilder = MongoActionsBuilder(mutaction.project.id, client)
    executeTopLevelMutaction(actionsBuilder.database, mutaction, actionsBuilder)
  }

  def executeTopLevelMutaction(
      database: MongoDatabase,
      mutaction: TopLevelDatabaseMutaction,
      mutationBuilder: MongoActionsBuilder
  ): Future[MutactionResults] = {
    mutaction match {
      case m: FurtherNestedMutaction =>
        for {
          result <- interpreterFor(m).mongoAction(mutationBuilder).fn(database)
          childResults <- result match {
                           case result: FurtherNestedMutactionResult =>
                             Future.sequence(m.allNestedMutactions.map(executeNestedMutaction(database, _, result.id, mutationBuilder)))
                           case _ => Future.successful(Vector.empty)
                         }
        } yield MutactionResults(result, childResults)

      case m: FinalMutaction =>
        for {
          result <- interpreterFor(m).mongoAction(mutationBuilder).fn(database)
        } yield MutactionResults(result, Vector.empty)

      case _ => sys.error("not implemented yet")
    }
  }

  def executeNestedMutaction(database: MongoDatabase,
                             mutaction: NestedDatabaseMutaction,
                             parentId: IdGCValue,
                             mutationBuilder: MongoActionsBuilder): Future[MutactionResults] = {
    mutaction match {
      case m: FurtherNestedMutaction =>
        for {
          result <- interpreterFor(m).mongoAction(mutationBuilder, parentId).fn(database)
          childResults <- result match {
                           case result: FurtherNestedMutactionResult =>
                             Future.sequence(m.allNestedMutactions.map(executeNestedMutaction(database, _, result.id, mutationBuilder)))
                           case _ => Future.successful(Vector.empty)
                         }
        } yield MutactionResults(result, childResults)

      case m: FinalMutaction =>
        for {
          result <- interpreterFor(m).mongoAction(mutationBuilder, parentId).fn(database)
        } yield MutactionResults(result, Vector.empty)

      case _ => sys.error("not implemented yet")
    }
  }

  def interpreterFor(mutaction: TopLevelDatabaseMutaction): TopLevelDatabaseMutactionInterpreter = mutaction match {
    case m: TopLevelCreateNode => CreateNodeInterpreter(mutaction = m, includeRelayRow = false)
    case m: TopLevelUpdateNode => UpdateNodeInterpreter(mutaction = m)
    case m: TopLevelUpsertNode => ???
    case m: TopLevelDeleteNode => DeleteNodeInterpreter(mutaction = m)
    case m: UpdateNodes        => ???
    case m: DeleteNodes        => ???
    case m: ResetData          => ResetDataInterpreter(mutaction = m)
    case m: ImportNodes        => ???
    case m: ImportRelations    => ???
    case m: ImportScalarLists  => ???
  }

  def interpreterFor(mutaction: NestedDatabaseMutaction): NestedDatabaseMutactionInterpreter = mutaction match {
    case m: NestedCreateNode => ???
    case m: NestedUpdateNode => ???
    case m: NestedUpsertNode => ???
    case m: NestedDeleteNode => ???
    case m: NestedConnect    => ???
    case m: NestedDisconnect => ???
  }

}

//Slick replacement ideas

//  def run[S, A](database: MongoDatabase, action: MongoAction[S, A]): Future[A] = {
//    action match {
//      case SuccessAction(value) =>
//        Future.successful(value)
//
//      case SimpleMongoAction(fn) =>
//        fn(database)
//
//      case FlatMapAction(source, fn) =>
//        for {
//          result     <- run(database, source)
//          nextResult <- run(database, fn(result))
//        } yield nextResult
//
//      case MapAction(source, fn) =>
//        for {
//          result <- run(database, source)
//        } yield fn(result)
//    }
//  }
