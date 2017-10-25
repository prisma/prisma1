package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.shared.errors.SystemErrors
import cool.graph.shared.models.{Client, Project}
import cool.graph.system.database.finder.ProjectQueries
import cool.graph.system.database.tables.{ProjectTable, RelayIdTable}
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.MySQLProfile.backend.DatabaseDef
import slick.lifted.TableQuery

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class DeleteProject(client: Client, project: Project, projectQueries: ProjectQueries, willBeRecreated: Boolean = false, internalDatabase: DatabaseDef)
    extends SystemSqlMutaction {
  override def execute: Future[SystemSqlStatementResult[Any]] = {
    val projects = TableQuery[ProjectTable]
    val relayIds = TableQuery[RelayIdTable]

    Future.successful(SystemSqlStatementResult(sqlAction = DBIO.seq(projects.filter(_.id === project.id).delete, relayIds.filter(_.id === project.id).delete)))
  }

  override def rollback = Some(CreateProject(client, project, internalDatabase, projectQueries).execute)

  override def verify(): Future[Try[MutactionVerificationSuccess]] = {
    val numberOfProjects = TableQuery[ProjectTable].filter(p => p.clientId === client.id).length

    internalDatabase.run(numberOfProjects.result).map { remainingCount =>
      if (remainingCount == 1 && !willBeRecreated) {
        Failure(SystemErrors.CantDeleteLastProject())
      } else {
        Success(MutactionVerificationSuccess())
      }
    }
  }
}
