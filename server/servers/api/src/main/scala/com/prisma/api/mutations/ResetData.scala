package com.prisma.api.mutations

import com.prisma.api.ApiDependencies
import com.prisma.api.connector._
import com.prisma.shared.models._

import scala.concurrent.Future

case class ResetData(project: Project, dataResolver: DataResolver)(implicit apiDependencies: ApiDependencies) extends SingleItemClientMutation {

  override def prepareMutactions(): Future[PreparedMutactions] = {
    val relationTableNames = project.relations.map(_.relationTableName).toVector
    val modelTableNames    = project.models.map(_.name).toVector
    val listTableNames     = project.models.flatMap(model => model.scalarListFields.map(field => s"${model.name}_${field.name}"))
    val relayTableName     = "_RelayId"

    val resetData = ResetDataMutaction(project, relationTableNames ++ modelTableNames ++ listTableNames :+ relayTableName)

    Future.successful {
      PreparedMutactions(
        databaseMutactions = Vector(resetData),
        sideEffectMutactions = Vector.empty
      )
    }
  }

  override def getReturnValue(results: MutactionResults): Future[ReturnValueResult] = Future.successful(ReturnValue(PrismaNode.dummy))
}
