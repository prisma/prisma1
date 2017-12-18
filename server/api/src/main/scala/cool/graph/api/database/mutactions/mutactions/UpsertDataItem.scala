package cool.graph.api.database.mutactions.mutactions

import cool.graph.api.database.DatabaseMutationBuilder
import cool.graph.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult}
import cool.graph.api.mutations.{CoolArgs, NodeSelector}
import cool.graph.shared.models.{Model, Project}
import slick.dbio.DBIOAction

import scala.concurrent.Future

case class UpsertDataItem(
    project: Project,
    model: Model,
    createArgs: CoolArgs,
    updateArgs: CoolArgs,
    where: NodeSelector
) extends ClientSqlDataChangeMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = Future.successful {
    val updateAction = DatabaseMutationBuilder.updateDataItemByUnique(project, model, updateArgs, where)
    val createAction = DatabaseMutationBuilder.createDataItemIfUniqueDoesNotExist(project, model, createArgs, where)
    ClientSqlStatementResult(DBIOAction.seq(updateAction, createAction))
  }
}
