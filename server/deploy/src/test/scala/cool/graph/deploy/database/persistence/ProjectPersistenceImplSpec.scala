package cool.graph.deploy.database.persistence

import cool.graph.deploy.database.tables.Tables
import cool.graph.deploy.specutils.{DeploySpecBase, TestProject}
import cool.graph.shared.models.Migration
import org.scalatest.{FlatSpec, Matchers}
import slick.jdbc.MySQLProfile.api._

class ProjectPersistenceImplSpec extends FlatSpec with Matchers with DeploySpecBase {

  val projectPersistence   = testDependencies.projectPersistence
  val migrationPersistence = testDependencies.migrationPersistence

  ".load()" should "return None if there's no project yet in the database" in {
    val result = projectPersistence.load("non-existent-id@some-stage").await()
    result should be(None)
  }

  ".load()" should "return the project with the correct revision" in {
    val project = setupProject(basicTypesGql)

    // Create an empty migration to have an unapplied migration with a higher revision
    migrationPersistence.create(project, Migration.empty(project)).await

    def loadProject = {
      val result = projectPersistence.load(project.id).await()
      result shouldNot be(None)
      result
    }

    // Only load the applied revision, which is 2 (setup does add + deploy = revisions 0, 1)
    loadProject.get.revision shouldEqual 2

    // After another migration is completed, the revision is bumped to the revision of the latest migration
    migrationPersistence.markMigrationAsApplied(Migration.empty(project).copy(revision = 3)).await
    loadProject.get.revision shouldEqual 3
  }

  ".create()" should "store the project in the db" in {
    assertNumberOfRowsInProjectTable(0)
    projectPersistence.create(TestProject()).await()
    assertNumberOfRowsInProjectTable(1)
  }

  ".loadAll()" should "load all projects (for a user TODO)" in {
    projectPersistence.create(TestProject()).await()
    projectPersistence.create(TestProject()).await()
    projectPersistence.loadAll().await should have(size(2))
  }

  ".loadProjectsWithUnappliedMigrations()" should "load all distinct projects with unapplied migrations" in {
    val migratedProject               = TestProject()
    val unmigratedProject             = TestProject()
    val unmigratedProjectWithMultiple = TestProject()

    // Create base projects
    projectPersistence.create(migratedProject).await()
    projectPersistence.create(unmigratedProject).await()
    projectPersistence.create(unmigratedProjectWithMultiple).await()

    // Create pending migrations

  }

  def assertNumberOfRowsInProjectTable(count: Int): Unit = {
    val query = Tables.Projects.size
    internalDb.run(query.result) should equal(count)
  }
}
