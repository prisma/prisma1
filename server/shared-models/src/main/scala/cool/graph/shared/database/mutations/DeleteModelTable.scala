package cool.graph.shared.database.mutations

import cool.graph.shared.database.{SqlDDL, SqlDDLMutaction}
import cool.graph.shared.models.Model
import slick.jdbc.MySQLProfile.api._

import scala.util.Success

case class DeleteModelTable(projectId: String, model: Model) extends SqlDDLMutaction {

  override def execute = {
//    val relayIds = TableQuery(new ProjectRelayIdTable(_, projectId))

    Success(
      DBIO.seq(SqlDDL.dropTable(projectId = projectId, tableName = model.name)
      //, relayIds.filter(_.modelId === model.id).delete))
      ))
  }

  override def rollback = CreateModelTable(projectId, model).execute
}
