package com.prisma.api.database.mutactions.mutactions

import com.prisma.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult, MutactionVerificationSuccess}
import com.prisma.api.database._
import com.prisma.api.schema.APIErrors
import com.prisma.shared.models.IdType.Id
import com.prisma.shared.models.{Model, Project}
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

case class DeleteDataItem(project: Project, model: Model, id: Id, previousValues: DataItem, requestId: Option[String] = None)
    extends ClientSqlDataChangeMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = {
    val relayIds = TableQuery(new ProjectRelayIdTable(_, project.id))
    Future.successful(
      ClientSqlStatementResult(
        sqlAction = DBIO.seq(
          DatabaseMutationBuilder.deleteDataItemById(project.id, model.name, id),
          relayIds.filter(_.id === id).delete
        )
      )
    )
  }

  override def verify(resolver: DataResolver): Future[Try[MutactionVerificationSuccess]] = {
    if (!NameConstraints.isValidDataItemId(id))
      return Future.successful(Failure(APIErrors.IdIsInvalid(id)))

    resolver.existsByModelAndId(model, id) map {
      case false => Failure(APIErrors.DataItemDoesNotExist(model.name, id))
      case true  => Success(MutactionVerificationSuccess())
    }
  }
}
