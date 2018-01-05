package cool.graph.deploy.database.persistence

import cool.graph.deploy.database.tables.Tables
import cool.graph.deploy.specutils.{DeploySpecBase, TestProject}
import cool.graph.shared.models.{Migration, MigrationStatus}
import org.scalatest.{FlatSpec, Matchers}
import slick.jdbc.MySQLProfile.api._

class MigrationPersistenceImplSpec extends FlatSpec with Matchers with DeploySpecBase {

  val migrationPersistence: MigrationPersistenceImpl = testDependencies.migrationPersistence
  val projectPersistence: ProjectPersistenceImpl     = testDependencies.projectPersistence

  ".create()" should "store the migration in the db and increment the revision accordingly" in {
    val project = setupProject(basicTypesGql)
    assertNumberOfRowsInMigrationTable(2)

    val savedMigration = migrationPersistence.create(Migration.empty(project.id)).await()
    assertNumberOfRowsInMigrationTable(3)
    savedMigration.revision shouldEqual 3
  }

  ".loadAll()" should "return all migrations for a project" in {
    val project = setupProject(basicTypesGql)

    // 1 successful, 2 pending migrations (+ 2 from setup)
    migrationPersistence.create(Migration.empty(project.id).copy(status = MigrationStatus.Success)).await
    migrationPersistence.create(Migration.empty(project.id)).await
    migrationPersistence.create(Migration.empty(project.id)).await

    val migrations = migrationPersistence.loadAll(project.id).await
    migrations should have(size(5))
  }

//  ".getUnappliedMigration()" should "return an unapplied migration from the specified project" in {
//    val project  = setupProject(basicTypesGql)
//    val project2 = setupProject(basicTypesGql)
//
//    // 2 unapplied migrations
//    migrationPersistence.create(project, Migration.empty(project)).await
//    migrationPersistence.create(project2, Migration.empty(project2)).await
//
//    val unapplied = migrationPersistence.getUnappliedMigration(project.id).await()
//    unapplied.isDefined shouldEqual true
//    unapplied.get.previousProject.id shouldEqual project.id
//
//    migrationPersistence.markMigrationAsApplied(unapplied.get.migration).await()
//
//    val unapplied2 = migrationPersistence.getUnappliedMigration(project2.id).await()
//    unapplied2.isDefined shouldEqual true
//    unapplied2.get.previousProject.id shouldEqual project2.id
//
//    migrationPersistence.markMigrationAsApplied(unapplied2.get.migration).await()
//    migrationPersistence.getUnappliedMigration(project.id).await().isDefined shouldEqual false
//  }

  ".updateMigrationStatus()" should "update a migration status correctly" in {
    val project          = setupProject(basicTypesGql)
    val createdMigration = migrationPersistence.create(Migration.empty(project.id)).await

    migrationPersistence.updateMigrationStatus(createdMigration, MigrationStatus.Success).await

    val lastMigration = migrationPersistence.getLastMigration(project.id).await.get
    lastMigration.revision shouldEqual createdMigration.revision
    lastMigration.status shouldEqual MigrationStatus.Success
  }

  ".getLastMigration()" should "get the last migration applied to a project" in {
    val project = setupProject(basicTypesGql)
    migrationPersistence.getLastMigration(project.id).await.get.revision shouldEqual 2
  }

  ".getNextMigration()" should "get the next migration to be applied to a project" in {
    val project          = setupProject(basicTypesGql)
    val createdMigration = migrationPersistence.create(Migration.empty(project.id)).await

    migrationPersistence.getNextMigration(project.id).await.get.revision shouldEqual createdMigration.revision
  }

  "loadDistinctUnmigratedProjectIds()" should "load all distinct project ids that have open migrations" in {
    val migratedProject               = TestProject()
    val unmigratedProject             = TestProject()
    val unmigratedProjectWithMultiple = TestProject()

    // Create base projects
    projectPersistence.create(migratedProject).await()
    projectPersistence.create(unmigratedProject).await()
    projectPersistence.create(unmigratedProjectWithMultiple).await()

    // Create pending migrations
    migrationPersistence.create(Migration.empty(unmigratedProject.id)).await
    migrationPersistence.create(Migration.empty(unmigratedProjectWithMultiple.id)).await
    migrationPersistence.create(Migration.empty(unmigratedProjectWithMultiple.id)).await

    val projectIds = migrationPersistence.loadDistinctUnmigratedProjectIds().await
    projectIds should have(size(2))
  }

  def assertNumberOfRowsInMigrationTable(count: Int): Unit = {
    val query = Tables.Migrations.size
    internalDb.run(query.result) should equal(count)
  }
}
