package cool.graph.api.database.mutactions.mutactions

import cool.graph.api.database.DatabaseMutationBuilder
import cool.graph.api.database.Types.DataItemFilterCollection
import cool.graph.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult}
import cool.graph.api.mutations.CoolArgs
import cool.graph.shared.models.{Model, Project}
import slick.dbio.DBIOAction

import scala.concurrent.Future

case class UpdateDataItems(
    project: Project,
    model: Model,
    updateArgs: CoolArgs,
    where: DataItemFilterCollection
) extends ClientSqlDataChangeMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = Future.successful(
    ClientSqlStatementResult(sqlAction = DatabaseMutationBuilder.updateDataItems(project, model, updateArgs, where))
  )
}
