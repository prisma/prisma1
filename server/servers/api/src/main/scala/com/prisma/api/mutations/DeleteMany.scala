package com.prisma.api.mutations

import com.prisma.api.ApiDependencies
import com.prisma.api.connector._
import com.prisma.api.mutactions.DatabaseMutactions
import com.prisma.shared.models.{Model, Project}

import scala.concurrent.Future

case class DeleteMany(
    project: Project,
    model: Model,
    whereFilter: Option[Filter],
    dataResolver: DataResolver
)(implicit apiDependencies: ApiDependencies)
    extends ClientMutation[BatchPayload] {

  def prepareMutactions(): Future[TopLevelDatabaseMutaction] = Future.successful(DatabaseMutactions(project).getMutactionsForDeleteMany(model, whereFilter))

  override def getReturnValue(results: MutactionResults): Future[BatchPayload] = results.results.head match {
    case ManyNodesResult(_, count) => Future.successful(BatchPayload(count = count))
    case _                         => sys.error("DeleteMany should always return a ManyNodesResult")
  }

}
