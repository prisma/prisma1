package cool.graph.system.mutactions.internal

import java.sql.SQLIntegrityConstraintViolationException

import cool.graph._
import cool.graph.shared.errors.UserInputErrors.ProjectWithAliasAlreadyExists
import cool.graph.shared.models.{Client, Project}
import cool.graph.system.database.ModelToDbMapper
import cool.graph.system.database.finder.ProjectQueries
import cool.graph.system.database.tables.ProjectTable
import cool.graph.system.mutactions.internal.validations.ProjectValidations
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.MySQLProfile.backend.DatabaseDef
import slick.lifted.TableQuery

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

case class UpdateProject(
    client: Client,
    oldProject: Project,
    project: Project,
    internalDatabase: DatabaseDef,
    projectQueries: ProjectQueries,
    bumpRevision: Boolean = true
) extends SystemSqlMutaction {

  override def verify(): Future[Try[MutactionVerificationSuccess]] = {
    val projectValidations = ProjectValidations(client, project, projectQueries)
    projectValidations.verify()
  }

  override def execute: Future[SystemSqlStatementResult[Any]] = {
    val projects = TableQuery[ProjectTable]
    Future.successful(SystemSqlStatementResult(sqlAction = DBIO.seq({

      // todo: update sangria-relay and introduce proper null support in the system api
      val nullableAlias: Option[String] = project.alias match {
        case Some("") => null
        case x        => x
      }
      val newRevision   = if (bumpRevision) oldProject.revision + 1 else oldProject.revision
      val actualProject = project.copy(revision = newRevision, alias = nullableAlias)

      projects.filter(_.id === project.id).update(ModelToDbMapper.convertProject(actualProject))
    })))
  }

  override def handleErrors =
    Some({
      // https://dev.mysql.com/doc/refman/5.5/en/error-messages-server.html#error_er_dup_entry
      case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1062 =>
        ProjectWithAliasAlreadyExists(alias = project.alias.getOrElse(""))
    })

  override def rollback = Some {
    UpdateProject(
      client = client,
      oldProject = oldProject,
      project = oldProject,
      internalDatabase = internalDatabase,
      projectQueries = projectQueries
    ).execute
  }
}
