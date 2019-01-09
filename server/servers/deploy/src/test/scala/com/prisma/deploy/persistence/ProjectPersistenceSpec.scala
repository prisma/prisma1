package com.prisma.deploy.persistence

import com.prisma.deploy.specutils.{DeploySpecBase, TestProject}
import com.prisma.shared.models._
import org.scalatest.{FlatSpec, Matchers}

class ProjectPersistenceSpec extends FlatSpec with Matchers with DeploySpecBase {

  val projectPersistence   = testDependencies.projectPersistence
  val migrationPersistence = testDependencies.migrationPersistence

  ".load()" should "return None if there's no project yet in the database" in {
    val result = projectPersistence.load("non-existent-id@some-stage").await()
    result should be(None)
  }

  ".load()" should "return the project with the correct revision" in {
    val (project, _) = setupProject(basicTypesGql)

    // Create an empty migration to have an unapplied migration with a higher revision
    migrationPersistence.create(Migration.empty(project.id)).await

    def loadProject = {
      val result = projectPersistence.load(project.id).await()
      result shouldNot be(None)
      result
    }

    // Load the applied revision, which is 2 (2 steps are done in setupProject)
    loadProject.get.revision shouldEqual 2

    // After another migration is completed, the revision is bumped to the revision of the latest migration
    migrationPersistence.updateMigrationStatus(MigrationId(project.id, 3), MigrationStatus.Success).await
    loadProject.get.revision shouldEqual 3
  }

  ".create()" should "store the project in the db" in {
    val project = TestProject().copy(
      secrets = Vector("foo"),
      functions = List(
        ServerSideSubscriptionFunction("function", true, WebhookDelivery("url", Vector.empty), "query")
      )
    )
    projectPersistence.create(project).await()
  }

  ".loadAll()" should "load all projects (for a user TODO)" in {
    setupProject(basicTypesGql, stage = "stage1")
    setupProject(basicTypesGql, stage = "stage2")

    projectPersistence.loadAll().await should have(size(2))
  }

  ".update()" should "update a project" in {
    val (project, _) = setupProject(basicTypesGql)
    println(project.id)

    val updatedProject = project.copy(secrets = Vector("Some", "secrets"))
    projectPersistence.update(updatedProject).await()

    val reloadedProject = projectPersistence.load(project.id).await.get
    reloadedProject.secrets should contain allOf (
      "Some",
      "secrets"
    )
  }

  ".delete()" should "delete a project" in {
    val (project, _) = setupProject(basicTypesGql)
    println(project.id)

    projectPersistence.delete(project.id).await()
    projectPersistence.load(project.id).await should be(None)
  }
}
