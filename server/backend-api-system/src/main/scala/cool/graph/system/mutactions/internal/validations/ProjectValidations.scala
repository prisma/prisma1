package cool.graph.system.mutactions.internal.validations

import cool.graph.shared.errors.UserInputErrors.{ProjectAliasEqualsAnExistingId, ProjectWithNameAlreadyExists}
import cool.graph.client.database.DataResolver
import cool.graph.shared.errors.UserInputErrors
import cool.graph.shared.models.{Client, Project}
import cool.graph.system.database.finder.ProjectQueries
import cool.graph.MutactionVerificationSuccess
import cool.graph.shared.NameConstraints

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

case class ProjectValidations(client: Client, project: Project, projectQueries: ProjectQueries)(implicit ec: ExecutionContext)
    extends MutactionVerificationUtil {

  def verify(): Future[Try[MutactionVerificationSuccess]] = {
    () match {
      case _ if !NameConstraints.isValidProjectName(project.name) =>
        Future.successful(Failure[MutactionVerificationSuccess](UserInputErrors.InvalidName(name = project.name)))

      case _ if project.alias.isDefined && !NameConstraints.isValidProjectAlias(project.alias.get) =>
        Future.successful(Failure(UserInputErrors.InvalidProjectAlias(alias = project.alias.get)))

      case _ =>
        serializeVerifications(List(verifyNameIsUnique, verifyAliasIsNotEqualToAProjectId))
    }
  }

  def verifyNameIsUnique(): Future[Try[MutactionVerificationSuccess]] = {
    projectQueries.loadByName(clientId = client.id, name = project.name).map {
      case None                                                  => Success(MutactionVerificationSuccess())
      case Some(loadedProject) if loadedProject.id == project.id => Success(MutactionVerificationSuccess())
      case _                                                     => Failure(ProjectWithNameAlreadyExists(name = project.name))
    }
  }

  def verifyAliasIsNotEqualToAProjectId(): Future[Try[MutactionVerificationSuccess]] = {
    project.alias match {
      case Some(alias) =>
        projectQueries.loadById(alias).map {
          case None    => Success(MutactionVerificationSuccess())
          case Some(_) => Failure(ProjectAliasEqualsAnExistingId(alias = alias))
        }
      case None =>
        Future.successful(Success(MutactionVerificationSuccess()))
    }
  }
}
