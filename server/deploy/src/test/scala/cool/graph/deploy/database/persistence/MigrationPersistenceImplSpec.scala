package cool.graph.deploy.database.persistence

import cool.graph.deploy.InternalTestDatabase
import cool.graph.deploy.database.tables.Tables
import cool.graph.shared.models.{Migration, Project}
import cool.graph.shared.project_dsl.TestProject
import cool.graph.utils.await.AwaitUtils
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}
import slick.jdbc.MySQLProfile.api._

class MigrationPersistenceImplSpec extends FlatSpec with Matchers with AwaitUtils with InternalTestDatabase with BeforeAndAfterEach {
  import scala.concurrent.ExecutionContext.Implicits.global

  val projectPersistence   = ProjectPersistenceImpl(internalDatabase = internalDatabase)
  val migrationPersistence = MigrationPersistenceImpl(internalDatabase = internalDatabase)
  val project              = TestProject()
  val migration: Migration = Migration.empty(project)

  override def beforeEach(): Unit = {
    super.beforeEach()
    setupProject(project)
  }

  def setupProject(project: Project): Unit = {
    projectPersistence.create(project).await
    migrationPersistence.create(project, Migration.empty(project).copy(hasBeenApplied = true)).await
  }

  ".create()" should "store the migration in the db and increment the revision accordingly" in {
    assertNumberOfRowsInMigrationTable(1)
    val savedMigration = migrationPersistence.create(project, Migration.empty(project)).await()
    assertNumberOfRowsInMigrationTable(2)
    savedMigration.revision shouldEqual 2
  }

  ".loadAll()" should "return all migrations for a project" in {
    // 1 applied, 2 unapplied migrations (+ 1 from setup)
    migrationPersistence.create(project, Migration.empty(project).copy(hasBeenApplied = true)).await
    migrationPersistence.create(project, Migration.empty(project)).await
    migrationPersistence.create(project, Migration.empty(project)).await

    val migrations = migrationPersistence.loadAll(project.id).await
    migrations should have(size(4))
  }

  ".getUnappliedMigration()" should "return an unapplied migration from any project" in {
    val project2 = project.copy(id = "test@test")
    setupProject(project2)

    // 2 unapplied migrations
    migrationPersistence.create(project, migration).await
    migrationPersistence.create(project2, migration.copy(projectId = project2.id)).await

    val unapplied = migrationPersistence.getUnappliedMigration().await()
    unapplied.isDefined shouldEqual true

    migrationPersistence.markMigrationAsApplied(unapplied.get.migration).await()
    val unapplied2 = migrationPersistence.getUnappliedMigration().await()

    unapplied2.isDefined shouldEqual true
    unapplied2.get.migration.projectId shouldNot equal(unapplied.get.migration.projectId)
    migrationPersistence.markMigrationAsApplied(unapplied2.get.migration).await()

    migrationPersistence.getUnappliedMigration().await().isDefined shouldEqual false
  }

  ".markMigrationAsApplied()" should "mark a migration as applied (duh)" in {
    val createdMigration = migrationPersistence.create(project, Migration.empty(project)).await
    migrationPersistence.markMigrationAsApplied(createdMigration).await
    migrationPersistence.getLastMigration(project.id).await.get.revision shouldEqual createdMigration.revision
  }

  ".getLastMigration()" should "get the last migration applied to a project" in {
    migrationPersistence.getLastMigration(project.id).await.get.revision shouldEqual 1
  }

  ".getNextMigration()" should "get the next migration to be applied to a project" in {
    val createdMigration = migrationPersistence.create(project, Migration.empty(project)).await
    migrationPersistence.getNextMigration(project.id).await.get.revision shouldEqual createdMigration.revision
  }
  def assertNumberOfRowsInMigrationTable(count: Int): Unit = {
    val query = Tables.Migrations.size
    runQuery(query.result) should equal(count)
  }

  def runQuery[R](a: DBIOAction[R, NoStream, Nothing]): R = internalDatabase.run(a).await()
}
