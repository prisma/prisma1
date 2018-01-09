package cool.graph.deploy.database.persistence

import cool.graph.deploy.database.tables.Tables
import cool.graph.deploy.specutils.{DeploySpecBase, TestProject}
import cool.graph.shared.models.{Migration, MigrationId, MigrationStatus}
import org.scalatest.{FlatSpec, Matchers}
import slick.jdbc.MySQLProfile.api._

class MigrationPersistenceImplSpec extends FlatSpec with Matchers with DeploySpecBase {

  val migrationPersistence: MigrationPersistenceImpl = testDependencies.migrationPersistence
  val projectPersistence: ProjectPersistenceImpl     = testDependencies.projectPersistence

  ".byId()" should "load a migration by project ID and revision" in {
    val (project1, _) = setupProject(basicTypesGql)
    val (project2, _) = setupProject(basicTypesGql)

    val migration0Project1 = migrationPersistence.byId(MigrationId(project1.id, 1)).await.get
    val migration1Project1 = migrationPersistence.byId(MigrationId(project1.id, 2)).await.get

    migration0Project1.projectId shouldEqual project1.id
    migration0Project1.revision shouldEqual 1
    migration1Project1.projectId shouldEqual project1.id
    migration1Project1.revision shouldEqual 2

    val migration0Project2 = migrationPersistence.byId(MigrationId(project2.id, 1)).await.get
    val migration1Project2 = migrationPersistence.byId(MigrationId(project2.id, 2)).await.get

    migration0Project2.projectId shouldEqual project2.id
    migration0Project2.revision shouldEqual 1
    migration1Project2.projectId shouldEqual project2.id
    migration1Project2.revision shouldEqual 2
  }

  ".create()" should "store the migration in the db and increment the revision accordingly" in {
    val (project, _) = setupProject(basicTypesGql)
    assertNumberOfRowsInMigrationTable(2)

    val savedMigration = migrationPersistence.create(Migration.empty(project.id)).await()
    assertNumberOfRowsInMigrationTable(3)
    savedMigration.revision shouldEqual 3
  }

  ".loadAll()" should "return all migrations for a project" in {
    val (project, _) = setupProject(basicTypesGql)

    // 1 successful, 2 pending migrations (+ 2 from setup)
    migrationPersistence.create(Migration.empty(project.id).copy(status = MigrationStatus.Success)).await
    migrationPersistence.create(Migration.empty(project.id)).await
    migrationPersistence.create(Migration.empty(project.id)).await

    val migrations = migrationPersistence.loadAll(project.id).await
    migrations should have(size(5))
  }

  ".updateMigrationStatus()" should "update a migration status correctly" in {
    val (project, _)     = setupProject(basicTypesGql)
    val createdMigration = migrationPersistence.create(Migration.empty(project.id)).await

    migrationPersistence.updateMigrationStatus(createdMigration.id, MigrationStatus.Success).await

    val lastMigration = migrationPersistence.getLastMigration(project.id).await.get
    lastMigration.revision shouldEqual createdMigration.revision
    lastMigration.status shouldEqual MigrationStatus.Success
  }

  ".updateMigrationErrors()" should "update the migration errors correctly" in {
    val (project, _)     = setupProject(basicTypesGql)
    val createdMigration = migrationPersistence.create(Migration.empty(project.id)).await
    val errors           = Vector("This is a serious issue", "Another one, oh noes.")

    migrationPersistence.updateMigrationErrors(createdMigration.id, errors).await

    val reloadedMigration = migrationPersistence.byId(createdMigration.id).await.get
    reloadedMigration.errors shouldEqual errors
  }

  ".updateMigrationApplied()" should "update the migration applied counter correctly" in {
    val (project, _)     = setupProject(basicTypesGql)
    val createdMigration = migrationPersistence.create(Migration.empty(project.id)).await

    migrationPersistence.updateMigrationApplied(createdMigration.id, 1).await

    val reloadedMigration = migrationPersistence.byId(createdMigration.id).await.get
    reloadedMigration.applied shouldEqual 1
  }

  ".updateMigrationRolledBack()" should "update the migration rolled back counter correctly" in {
    val (project, _)     = setupProject(basicTypesGql)
    val createdMigration = migrationPersistence.create(Migration.empty(project.id)).await

    migrationPersistence.updateMigrationRolledBack(createdMigration.id, 1).await

    val reloadedMigration = migrationPersistence.byId(createdMigration.id).await.get
    reloadedMigration.rolledBack shouldEqual 1
  }

  ".getLastMigration()" should "get the last migration applied to a project" in {
    val (project, _) = setupProject(basicTypesGql)
    migrationPersistence.getLastMigration(project.id).await.get.revision shouldEqual 2
  }

  ".getNextMigration()" should "get the next migration to be applied to a project" in {
    val (project, _)     = setupProject(basicTypesGql)
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
