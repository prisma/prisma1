package cool.graph.shared.database.mutations

import cool.graph.shared.database.{SqlDDL, SqlDDLMutaction}

import scala.util.Success

case class DeleteClientDatabaseForProject(projectId: String) extends SqlDDLMutaction {
  override def execute =
    Success(
      SqlDDL
        .deleteProjectDatabase(projectId = projectId))

  override def rollback = CreateClientDatabaseForProject(projectId).execute
}
