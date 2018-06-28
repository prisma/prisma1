package com.prisma.api.mutations

import com.prisma.api.ApiDependencies
import com.prisma.api.connector._
import com.prisma.shared.models._

import scala.concurrent.Future

case class ResetData(project: Project, dataResolver: DataResolver)(implicit apiDependencies: ApiDependencies) extends SingleItemClientMutation {

  override def prepareMutactions(): Future[TopLevelDatabaseMutaction] = {
    Future.successful(ResetData(project))
  }

  override def getReturnValue(results: MutactionResults): Future[ReturnValueResult] = Future.successful(ReturnValue(PrismaNode.dummy))
}
