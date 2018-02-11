package com.prisma.shared.schema_dsl

import com.prisma.shared.models._
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
      projects = projects,
      createdAt = org.joda.time.DateTime.now,
      updatedAt = org.joda.time.DateTime.now
    )
  }
}

object TestProject {
  val empty = this.apply()

  def apply(): Project = {
    Project(id = testProjectId, ownerId = testClientId, schema = Schema())
  }
}
