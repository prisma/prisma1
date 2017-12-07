package cool.graph.deploy.migration.mutactions

import cool.graph.deploy.database.DatabaseMutationBuilder
import cool.graph.shared.models.{Project, Relation}

import scala.concurrent.Future

case class CreateRelationTable(project: Project, relation: Relation) extends ClientSqlMutaction {
  override def execute: Future[ClientSqlStatementResult[Any]] = {

    val aModel = project.getModelById_!(relation.modelAId)
    val bModel = project.getModelById_!(relation.modelBId)

    Future.successful(
      ClientSqlStatementResult(
        sqlAction = DatabaseMutationBuilder
          .createRelationTable(projectId = project.id, tableName = relation.id, aTableName = aModel.name, bTableName = bModel.name)))
  }

  override def rollback = Some(DeleteRelationTable(project, relation).execute)
}
