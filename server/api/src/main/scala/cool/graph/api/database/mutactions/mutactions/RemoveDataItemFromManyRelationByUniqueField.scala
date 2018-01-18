package cool.graph.api.database.mutactions.mutactions

import cool.graph.api.database.DatabaseMutationBuilder
import cool.graph.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult}
import cool.graph.api.mutations.{NodeSelector, ParentInfo}
import cool.graph.shared.models.Project

import scala.concurrent.Future

case class RemoveDataItemFromManyRelationByUniqueField(project: Project, parentInfo: ParentInfo, where: NodeSelector) extends ClientSqlDataChangeMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] =
    Future.successful(ClientSqlStatementResult(DatabaseMutationBuilder.deleteRelationRowByUniqueValueForA(project.id, parentInfo, where)))
}
