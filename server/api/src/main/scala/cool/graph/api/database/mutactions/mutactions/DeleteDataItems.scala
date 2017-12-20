package cool.graph.api.database.mutactions.mutactions

import cool.graph.api.database.DatabaseMutationBuilder
import cool.graph.api.database.Types.DataItemFilterCollection
import cool.graph.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult}
import cool.graph.shared.models.{Model, Project}

import scala.concurrent.Future

case class DeleteDataItems(
    project: Project,
    model: Model,
    where: DataItemFilterCollection
) extends ClientSqlDataChangeMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = Future.successful(
    ClientSqlStatementResult(sqlAction = DatabaseMutationBuilder.deleteDataItems(project, model, where))
  )
}
