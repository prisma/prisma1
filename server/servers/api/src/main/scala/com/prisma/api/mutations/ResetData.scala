package com.prisma.api.mutations

import com.prisma.api.ApiDependencies
import com.prisma.api.connector._
import com.prisma.shared.models._

import scala.concurrent.Future

case class ResetData(project: Project, dataResolver: DataResolver)(implicit apiDependencies: ApiDependencies) extends SingleItemClientMutation {

  override def prepareMutactions(): Future[PreparedMutactions] = {
    val disableChecks    = Vector(DisableForeignKeyConstraintChecks)
    val removeRelations  = project.relations.map(relation => TruncateTable(projectId = project.id, tableName = relation.id)).toVector
    val removeDataItems  = project.models.map(model => TruncateTable(projectId = project.id, tableName = model.name)).toVector
    val listTableNames   = project.models.flatMap(model => model.scalarListFields.map(field => s"${model.name}_${field.name}"))
    val removeListValues = listTableNames.map(tableName => TruncateTable(projectId = project.id, tableName = tableName))
    val removeRelayIds   = Vector(TruncateTable(projectId = project.id, tableName = "_RelayId"))
    val enableChecks     = Vector(EnableForeignKeyConstraintChecks)

    Future.successful {
      PreparedMutactions(
        databaseMutactions = disableChecks ++ removeRelations ++ removeDataItems ++ removeListValues ++ removeRelayIds ++ enableChecks,
        sideEffectMutactions = Vector.empty
      )
    }
  }

  override def getReturnValue: Future[ReturnValueResult] = Future.successful(ReturnValue(DataItem("", Map.empty)))
}
