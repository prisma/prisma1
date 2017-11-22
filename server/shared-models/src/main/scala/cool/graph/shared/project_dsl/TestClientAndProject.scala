package cool.graph.shared.project_dsl

import cool.graph.shared.models._
import TestIds._

object TestClient {
  def apply(project: Project): Client = apply(Some(project))

  def apply(project: Option[Project] = None): Client = {
    val projects = project match {
      case Some(project) => List(project)
      case None          => List.empty
    }
    Client(
      id = testClientId,
      auth0Id = Some(testAuth0Id),
      isAuth0IdentityProviderEmail = true,
      name = testClientId,
      email = testEmail,
      hashedPassword = "",
      resetPasswordSecret = Some(testResetPasswordToken),
      source = CustomerSource.DOCS,
      projects = projects,
      createdAt = org.joda.time.DateTime.now,
      updatedAt = org.joda.time.DateTime.now
    )
  }
}

object TestProject {
  val empty = this.apply()

  def apply(): Project = {
    Project(id = testProjectId, ownerId = testClientId, name = s"Test Project", alias = Some(testProjectAlias), projectDatabase = database)
  }

  def database =
    ProjectDatabase(id = testProjectDatabaseId, region = Region.EU_WEST_1, name = "client1", isDefaultForRegion = true)
}
