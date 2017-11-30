package cool.graph.deploy.migration.mutactions

import cool.graph.deploy.database.DatabaseMutationBuilder
import cool.graph.shared.models.{Project, Relation}

import scala.concurrent.Future

case class DeleteRelationTable(project: Project, relation: Relation) extends ClientSqlMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = Future.successful {
    ClientSqlStatementResult(sqlAction = DatabaseMutationBuilder.dropTable(projectId = project.id, tableName = relation.id))
  }

  override def rollback = Some(CreateRelationTable(project, relation).execute)

}
