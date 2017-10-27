package cool.graph.system.mutactions.client

import cool.graph.{ClientMutactionNoop, _}
import cool.graph.client.database.DatabaseMutationBuilder
import cool.graph.shared.RelationFieldMirrorColumn
import cool.graph.shared.models.{Project, Relation}

import scala.concurrent.Future

case class CopyRelationTableData(sourceProject: Project, sourceRelation: Relation, targetProjectId: String, targetRelation: Relation)
    extends ClientSqlSchemaChangeMutaction {
  override def execute: Future[ClientSqlStatementResult[Any]] = {
    val columns = List[String]("id", "A", "B") ++ sourceRelation.fieldMirrors
      .map(mirror => RelationFieldMirrorColumn.mirrorColumnName(sourceProject, sourceProject.getFieldById_!(mirror.fieldId), sourceRelation))
    Future.successful(
      ClientSqlStatementResult(
        sqlAction = DatabaseMutationBuilder.copyTableData(sourceProject.id, sourceRelation.id, columns, targetProjectId, targetRelation.id)))
  }

  override def rollback = Some(ClientMutactionNoop().execute) // consider truncating table

}
