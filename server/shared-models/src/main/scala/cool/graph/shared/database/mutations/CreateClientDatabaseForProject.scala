package cool.graph.shared.database.mutations

import cool.graph.shared.database.{SqlDDL, SqlDDLMutaction}

import scala.util.Success

case class CreateClientDatabaseForProject(projectId: String) extends SqlDDLMutaction {

  override def execute = Success(SqlDDL.createClientDatabaseForProject(projectId))

  override def rollback = DeleteClientDatabaseForProject(projectId).execute
}
