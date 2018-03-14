package com.prisma.api.mutations

import com.prisma.api.ApiDependencies
import com.prisma.api.connector.mysql.database.DataResolver
import com.prisma.api.connector.{DataItem, DisableForeignKeyConstraintChecks, EnableForeignKeyConstraintChecks, TruncateTable}
import com.prisma.shared.models._

import scala.concurrent.Future

case class ResetData(project: Project, dataResolver: DataResolver)(implicit apiDependencies: ApiDependencies) extends SingleItemClientMutation {

  override def prepareMutactions(): Future[PreparedMutactions] = {
    val disableChecks   = Vector(DisableForeignKeyConstraintChecks)
    val removeRelations = project.relations.map(relation => TruncateTable(projectId = project.id, tableName = relation.id)).toVector
    val removeDataItems = project.models.map(model => TruncateTable(projectId = project.id, tableName = model.name)).toVector
    val removeRelayIds  = Vector(TruncateTable(projectId = project.id, tableName = "_RelayId"))
    val enableChecks    = Vector(EnableForeignKeyConstraintChecks)

    Future.successful {
      PreparedMutactions(
        databaseMutactions = disableChecks ++ removeRelations ++ removeDataItems ++ removeRelayIds ++ enableChecks,
        sideEffectMutactions = Vector.empty
      )
    }
  }

  override def getReturnValue: Future[ReturnValueResult] = Future.successful(ReturnValue(DataItem("", Map.empty)))
}
