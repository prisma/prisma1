package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.shared.errors.UserInputErrors
import cool.graph.shared.models.{FunctionBinding, Project}
import cool.graph.system.database.tables.Tables
import scaldi.Injectable
import slick.dbio.Effect.Write
import slick.jdbc.MySQLProfile.api._
import slick.sql.FixedSqlAction

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/**
  * Sets the project to the "ejected" state, in which it can't be modified from the console anymore - only the CLI.
  */
case class EjectProject(project: Project) extends SystemSqlMutaction with Injectable {

  override def execute: Future[SystemSqlStatementResult[Any]] = {
    val ejectProjectAction = setEjectedQuery(true)
    Future.successful(SystemSqlStatementResult(sqlAction = DBIO.seq(ejectProjectAction)))
  }

  override def rollback: Option[Future[SystemSqlStatementResult[Any]]] = Some {
    val resetEjectProjectAction = setEjectedQuery(project.isEjected)
    Future.successful(SystemSqlStatementResult(sqlAction = DBIO.seq(resetEjectProjectAction)))
  }

  override def verify(): Future[Try[MutactionVerificationSuccess]] = Future.successful {
    () match {
      case _ if project.integrations.exists(_.isEnabled) =>
        Failure(UserInputErrors.ProjectEjectFailure("it has enabled integrations. Please migrate all integrations to resolvers first."))

      case _ if project.functions.exists(_.binding == FunctionBinding.PRE_WRITE) =>
        Failure(UserInputErrors.ProjectEjectFailure("it has a Pre_Write RequestPipelineFunction. Please migrate it to Transform_Argument first."))

      case _ =>
        Success(MutactionVerificationSuccess())
    }
  }

  private def setEjectedQuery(isEjected: Boolean): FixedSqlAction[Int, NoStream, Write] = {
    val query = for {
      projectRow <- Tables.Projects
      if projectRow.id === project.id
    } yield projectRow.isEjected

    query.update(isEjected)
  }
}
