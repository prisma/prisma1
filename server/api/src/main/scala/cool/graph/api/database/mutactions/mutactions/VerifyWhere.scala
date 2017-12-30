package cool.graph.api.database.mutactions.mutactions

import java.sql.SQLException

import cool.graph.api.database._
import cool.graph.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult}
import cool.graph.api.mutations.NodeSelector
import cool.graph.api.schema.APIErrors
import cool.graph.shared.models.Project

import scala.concurrent.Future

case class VerifyWhere(project: Project, where: NodeSelector) extends ClientSqlDataChangeMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = {
    Future.successful(ClientSqlStatementResult(sqlAction = DatabaseMutationBuilder.whereFailureTrigger(project, where)))
  }

  override def handleErrors = {Some({ case e: SQLException if e.getErrorCode == 1242 && causedByThisMutaction(e.getCause.toString) => throw APIErrors.NodeNotFoundForWhereError(where)})}

  def causedByThisMutaction(cause: String) = cause.contains(s"`${where.model.name}` where `${where.fieldName}` =")  && cause.contains(s"parameters ['${where.fieldValueAsString}',")

}