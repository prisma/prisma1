package com.prisma.api.database.mutactions.mutactions

import com.prisma.api.database.DatabaseMutationBuilder
import com.prisma.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult}
import com.prisma.api.mutations.{NodeSelector, ParentInfo}
import com.prisma.shared.models.{Model, Project}

import scala.concurrent.Future

case class DeleteDataItemByUniqueFieldIfInRelationWith(
    project: Project,
    parentInfo: ParentInfo,
    where: NodeSelector
) extends ClientSqlDataChangeMutaction {

  val aModel: Model           = parentInfo.relation.getModelA_!(project.schema)
  val deleteByUniqueValueForB = aModel.name == parentInfo.model.name

  override def execute: Future[ClientSqlStatementResult[Any]] = Future.successful {
    val action = if (deleteByUniqueValueForB) {
      DatabaseMutationBuilder.deleteDataItemByUniqueValueForBIfInRelationWithGivenA(project.id, parentInfo, where)
    } else {
      DatabaseMutationBuilder.deleteDataItemByUniqueValueForAIfInRelationWithGivenB(project.id, parentInfo, where)
    }
    ClientSqlStatementResult(sqlAction = action)
  }

}
