package cool.graph.system.mutactions.client

import cool.graph.{ClientMutactionNoop, _}
import cool.graph.client.database.DatabaseMutationBuilder
import cool.graph.shared.RelationFieldMirrorColumn
import cool.graph.shared.models.{Field, Project, Relation}

import scala.concurrent.Future

case class PopulateRelationFieldMirrorColumn(project: Project, relation: Relation, field: Field) extends ClientSqlSchemaChangeMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = {

    val model = project.getModelByFieldId_!(field.id)

    Future.successful(
      ClientSqlStatementResult(
        sqlAction = DatabaseMutationBuilder.populateRelationFieldMirror(
          projectId = project.id,
          modelTable = model.name,
          mirrorColumn = RelationFieldMirrorColumn.mirrorColumnName(project, field, relation),
          column = field.name,
          relationSide = relation.fieldSide(project, field).toString,
          relationTable = relation.id
        )))
  }

  override def rollback = Some(ClientMutactionNoop().execute)
}
