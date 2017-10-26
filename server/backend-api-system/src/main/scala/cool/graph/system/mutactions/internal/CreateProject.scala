package cool.graph.system.mutactions.internal

import java.sql.SQLIntegrityConstraintViolationException

import cool.graph._
import cool.graph.shared.errors.UserInputErrors.ProjectWithAliasAlreadyExists
import cool.graph.shared.models.{Client, Project}
import cool.graph.system.database.ModelToDbMapper
import cool.graph.system.database.finder.ProjectQueries
import cool.graph.system.database.tables.{ProjectTable, RelayIdTable}
import cool.graph.system.mutactions.internal.validations.ProjectValidations
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.MySQLProfile.backend.DatabaseDef
import slick.lifted.TableQuery

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

case class CreateProject(
    client: Client,
    project: Project,
    internalDatabase: DatabaseDef,
    projectQueries: ProjectQueries,
    ignoreDuplicateNameVerificationError: Boolean = false
) extends SystemSqlMutaction {

  override def execute: Future[SystemSqlStatementResult[Any]] = {
    val projects   = TableQuery[ProjectTable]
    val relayIds   = TableQuery[RelayIdTable]
    val addProject = projects += ModelToDbMapper.convertProject(project.copy(ownerId = client.id))
    val addRelayId = relayIds += cool.graph.system.database.tables.RelayId(project.id, "Project")

    Future.successful {
      SystemSqlStatementResult(
        sqlAction = DBIO.seq(addProject, addRelayId)
      )
    }
  }

  override def rollback = Some(DeleteProject(client, project, projectQueries = projectQueries, internalDatabase = internalDatabase).execute)

  override def handleErrors =
    Some({
      // https://dev.mysql.com/doc/refman/5.5/en/error-messages-server.html#error_er_dup_entry
      case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1062 =>
        ProjectWithAliasAlreadyExists(alias = project.alias.getOrElse(""))
    })

  override def verify(): Future[Try[MutactionVerificationSuccess]] = {
    val projectValidations = ProjectValidations(client, project, projectQueries)
    projectValidations.verify()
  }
}
