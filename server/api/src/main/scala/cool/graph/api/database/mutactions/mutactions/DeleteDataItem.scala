package cool.graph.api.database.mutactions.mutactions

import cool.graph.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult, MutactionVerificationSuccess}
import cool.graph.api.database.{DataItem, DataResolver, DatabaseMutationBuilder, ProjectRelayIdTable}
import cool.graph.api.schema.APIErrors
import cool.graph.shared.database.NameConstraints
import cool.graph.shared.models.IdType.Id
import cool.graph.shared.models.{Model, Project}
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
        sqlAction = DBIO.seq(DatabaseMutationBuilder.deleteDataItemById(project.id, model.name, id), relayIds.filter(_.id === id).delete)))
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
