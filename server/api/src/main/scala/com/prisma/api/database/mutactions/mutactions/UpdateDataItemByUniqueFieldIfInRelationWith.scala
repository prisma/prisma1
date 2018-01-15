package com.prisma.api.database.mutactions.mutactions

import com.prisma.api.database.DatabaseMutationBuilder
import com.prisma.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult}
import com.prisma.api.mutations.{CoolArgs, NodeSelector, ParentInfo}
import com.prisma.shared.models.{Model, Project}
import slick.dbio.DBIOAction

import scala.concurrent.Future

case class UpdateDataItemByUniqueFieldIfInRelationWith(project: Project, parentInfo: ParentInfo, where: NodeSelector, args: CoolArgs)
    extends ClientSqlDataChangeMutaction {

  val aModel: Model           = parentInfo.relation.getModelA_!(project.schema)
  val updateByUniqueValueForB = aModel.name == parentInfo.model.name
  val scalarArgs              = args.nonListScalarArgumentsAsCoolArgs(where.model)

  override def execute: Future[ClientSqlStatementResult[Any]] = Future.successful {
    val action = if (updateByUniqueValueForB) {
      DatabaseMutationBuilder.updateDataItemByUniqueValueForBIfInRelationWithGivenA(project.id, parentInfo, where, scalarArgs.raw)
    } else {
      DatabaseMutationBuilder.updateDataItemByUniqueValueForAIfInRelationWithGivenB(project.id, parentInfo, where, scalarArgs.raw)
    }

    if (scalarArgs.isNonEmpty) {
      ClientSqlStatementResult(sqlAction = action)
    } else {
      ClientSqlStatementResult(sqlAction = DBIOAction.successful(()))
    }
  }
}
