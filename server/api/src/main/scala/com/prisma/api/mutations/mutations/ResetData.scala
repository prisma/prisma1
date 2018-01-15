package com.prisma.api.mutations.mutations

import com.prisma.api.ApiDependencies
import com.prisma.api.database.mutactions.mutactions._
import com.prisma.api.database.mutactions.{MutactionGroup, TransactionMutaction}
import com.prisma.api.database.{DataItem, DataResolver}
import com.prisma.api.mutations.{SingleItemClientMutation, ReturnValue, ReturnValueResult}
import com.prisma.shared.models._

import scala.concurrent.Future

case class ResetData(project: Project, dataResolver: DataResolver)(implicit apiDependencies: ApiDependencies) extends SingleItemClientMutation {

  override def prepareMutactions(): Future[List[MutactionGroup]] = {
    val disableChecks   = List(DisableForeignKeyConstraintChecks())
    val removeRelations = project.relations.map(relation => TruncateTable(projectId = project.id, tableName = relation.id))
    val removeDataItems = project.models.map(model => TruncateTable(projectId = project.id, tableName = model.name))
    val removeRelayIds  = List(TruncateTable(projectId = project.id, tableName = "_RelayId"))
    val enableChecks    = List(EnableForeignKeyConstraintChecks())

    val transactionMutaction = TransactionMutaction(disableChecks ++ removeRelations ++ removeDataItems ++ removeRelayIds ++ enableChecks, dataResolver)
    Future.successful(List(MutactionGroup(mutactions = List(transactionMutaction), async = false)))
  }

  override def getReturnValue: Future[ReturnValueResult] = Future.successful(ReturnValue(DataItem("", Map.empty)))
}
