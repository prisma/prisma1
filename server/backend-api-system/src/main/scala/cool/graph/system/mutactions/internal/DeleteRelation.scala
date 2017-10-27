package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.shared.models.{Project, Relation}
import cool.graph.system.database.client.ClientDbQueries
import cool.graph.system.database.tables.{RelationTable, RelayIdTable}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future

case class DeleteRelation(
    relation: Relation,
    project: Project,
    clientDbQueries: ClientDbQueries
) extends SystemSqlMutaction {
  override def execute: Future[SystemSqlStatementResult[Any]] = {
    val relations = TableQuery[RelationTable]
    val relayIds  = TableQuery[RelayIdTable]

    Future.successful(
      SystemSqlStatementResult(sqlAction = DBIO.seq(relations.filter(_.id === relation.id).delete, relayIds.filter(_.id === relation.id).delete)))
  }

  override def rollback = Some(CreateRelation(relation = relation, project = project, clientDbQueries = clientDbQueries).execute)

}
