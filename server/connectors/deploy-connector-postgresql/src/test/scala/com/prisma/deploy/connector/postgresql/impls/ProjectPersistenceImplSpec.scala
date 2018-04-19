package com.prisma.deploy.connector.postgresql.impls

import com.prisma.deploy.connector.postgresql.SpecBase
import com.prisma.deploy.connector.postgresql.database.Tables
import com.prisma.shared.models.{Migration, MigrationId, MigrationStatus}
import org.scalatest.{FlatSpec, Matchers}
import slick.jdbc.MySQLProfile.api._

class ProjectPersistenceImplSpec extends FlatSpec with Matchers with SpecBase {

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

    // Load the applied revision, which is 1 (setup does add revision 1)
    loadProject.get.revision shouldEqual 1

    // After another migration is completed, the revision is bumped to the revision of the latest migration
    migrationPersistence.updateMigrationStatus(MigrationId(project.id, 2), MigrationStatus.Success).await
    loadProject.get.revision shouldEqual 2
  }

  ".create()" should "store the project in the db" in {
    assertNumberOfRowsInProjectTable(0)
    projectPersistence.create(newTestProject()).await()
    assertNumberOfRowsInProjectTable(1)
  }

  ".loadAll()" should "load all projects (for a user TODO)" in {
    setupProject(basicTypesGql)
    setupProject(basicTypesGql)

    projectPersistence.loadAll().await should have(size(2))
  }

  ".update()" should "update a project" in {
    val (project, _) = setupProject(basicTypesGql)

    val updatedProject = project.copy(secrets = Vector("Some", "secrets"))
    projectPersistence.update(updatedProject).await()

    val reloadedProject = projectPersistence.load(project.id).await.get
    reloadedProject.secrets should contain allOf (
      "Some",
      "secrets"
    )
  }

  def assertNumberOfRowsInProjectTable(count: Int): Unit = {
    val query = Tables.Projects.size
    internalDb.run(query.result) should equal(count)
  }
}
