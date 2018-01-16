package cool.graph.deploy.migration.mutactions

import cool.graph.deploy.database.DatabaseMutationBuilder
import cool.graph.shared.models.TypeIdentifier.TypeIdentifier

import scala.concurrent.Future

case class CreateScalarListTable(projectId: String, model: String, field: String, typeIdentifier: TypeIdentifier) extends ClientSqlMutaction {
  override def execute: Future[ClientSqlStatementResult[Any]] = {
    Future.successful(ClientSqlStatementResult(
      sqlAction = DatabaseMutationBuilder.createScalarListTable(projectId = projectId, modelName = model, fieldName = field, typeIdentifier = typeIdentifier)))
  }

  override def rollback = Some(DeleteScalarListTable(projectId, model, field, typeIdentifier).execute)
}
