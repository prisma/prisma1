package com.prisma.api.connector.postgresql.impl

import com.prisma.api.connector._
import com.prisma.api.connector.postgresql.DatabaseMutactionInterpreter
import com.prisma.api.connector.postgresql.database.PostgresApiDatabaseMutationBuilder
import com.prisma.api.schema.UserFacingError
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}

case class PostgresDatabaseMutactionExecutor(clientDb: Database, createRelayIds: Boolean)(implicit ec: ExecutionContext) extends DatabaseMutactionExecutor {

  override def executeTransactionally(mutaction: TopLevelDatabaseMutaction) = execute(mutaction, transactionally = true)

  override def executeNonTransactionally(mutaction: TopLevelDatabaseMutaction) = execute(mutaction, transactionally = false)

  private def execute(mutaction: TopLevelDatabaseMutaction, transactionally: Boolean): Future[DatabaseMutactionResult] = {
    val mutationBuilder = PostgresApiDatabaseMutationBuilder(schemaName = mutaction.project.id, schema = mutaction.project.schema)
    val singleAction = transactionally match {
      case true  => recurse(mutaction, UnitDatabaseMutactionResult, mutationBuilder).transactionally
      case false => recurse(mutaction, UnitDatabaseMutactionResult, mutationBuilder)
    }

    // fixme: should error mapping/handling be directly handled within the interpreters?
    val interpreters        = (mutaction +: mutaction.allMutactions).map(interpreterFor)
    val combinedErrorMapper = interpreters.map(_.errorMapper).reduceLeft(_ orElse _)
    clientDb
      .run(singleAction)
      .recover { case error => throw combinedErrorMapper.lift(error).getOrElse(error) }
  }

  private def recurse(
      mutaction: DatabaseMutaction,
      parentResult: DatabaseMutactionResult,
      mutationBuilder: PostgresApiDatabaseMutationBuilder
  ): DBIO[DatabaseMutactionResult] = {
    mutaction match {
      case m: FurtherNestedMutaction =>
        for {
          result       <- interpreterFor(m).newAction(mutationBuilder, parentResult)
          childResults <- DBIO.sequence(m.allMutactions.map(recurse(_, result, mutationBuilder)))
          //DBIO.sequence(mutation.childs.map(recurse(_, mutationBuilder)))
        } yield result
      case m: FinalMutaction =>
        for {
          result <- interpreterFor(m).newAction(mutationBuilder, parentResult)
        } yield result
    }
  }

  override def execute(mutactions: Vector[DatabaseMutaction], runTransactionally: Boolean): Future[Vector[DatabaseMutactionResult]] = {
//    val interpreters        = mutactions.map(interpreterFor)
//    val combinedErrorMapper = interpreters.map(_.errorMapper).reduceLeft(_ orElse _)
//    val mutationBuilder     = PostgresApiDatabaseMutationBuilder(schemaName = mutactions.head.project.id, schema = mutactions.head.project.schema)
//
//    val singleAction = runTransactionally match {
//      case true  => DBIO.sequence(interpreters.map(_.newAction(mutationBuilder))).transactionally
//      case false => DBIO.sequence(interpreters.map(_.newAction(mutationBuilder)))
//    }
//
//    clientDb
//      .run(singleAction)
//      .recover { case error => throw combinedErrorMapper.lift(error).getOrElse(error) }
    ???
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
    case m: NestedUpsertDataItem              => NestedUpsertDataItemInterpreter(m)
    case m: VerifyConnection                  => VerifyConnectionInterpreter(m)
    case m: VerifyWhere                       => VerifyWhereInterpreter(m)
    case m: CreateDataItemsImport             => CreateDataItemsImportInterpreter(m)
    case m: CreateRelationRowsImport          => CreateRelationRowsImportInterpreter(m)
    case m: PushScalarListsImport             => PushScalarListsImportInterpreter(m)
  }
}
