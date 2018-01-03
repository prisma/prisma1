package cool.graph.api.database.mutactions.mutactions

import cool.graph.api.database.DatabaseMutationBuilder
import cool.graph.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult}
import cool.graph.api.mutations.NodeSelector
import cool.graph.shared.models.Project

import scala.concurrent.Future

case class TriggerWhere(project: Project, where: NodeSelector) extends ClientSqlDataChangeMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = Future.successful(
    ClientSqlStatementResult(sqlAction = DatabaseMutationBuilder.whereFailureTrigger(project, where))
  )
}
