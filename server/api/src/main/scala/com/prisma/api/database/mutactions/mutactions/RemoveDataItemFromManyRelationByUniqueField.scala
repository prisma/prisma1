package com.prisma.api.database.mutactions.mutactions

import com.prisma.api.database.DatabaseMutationBuilder
import com.prisma.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult}
import com.prisma.api.mutations.{NodeSelector, ParentInfo}
import com.prisma.shared.models.{Model, Project}

import scala.concurrent.Future

case class RemoveDataItemFromManyRelationByUniqueField(project: Project, parentInfo: ParentInfo, where: NodeSelector) extends ClientSqlDataChangeMutaction {

  val aModel: Model               = parentInfo.relation.getModelA_!(project.schema)
  val bModel: Model               = parentInfo.relation.getModelB_!(project.schema)
  val disconnectByUniqueValueForB = aModel.name == parentInfo.model.name

  override def execute: Future[ClientSqlStatementResult[Any]] = Future.successful {
    val action = if (disconnectByUniqueValueForB) {
      DatabaseMutationBuilder.deleteRelationRowByUniqueValueForB(project.id, parentInfo, where)
    } else {
      DatabaseMutationBuilder.deleteRelationRowByUniqueValueForA(project.id, parentInfo, where)
    }
    ClientSqlStatementResult(sqlAction = action)
  }
}
