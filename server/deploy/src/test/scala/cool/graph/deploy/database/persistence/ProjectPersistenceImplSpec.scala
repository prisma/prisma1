package cool.graph.deploy.database.persistence

import cool.graph.deploy.database.tables.Tables
import cool.graph.deploy.specutils.InternalTestDatabase
import cool.graph.shared.models.Migration
import cool.graph.shared.project_dsl.TestProject
import cool.graph.utils.await.AwaitUtils
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}
import slick.jdbc.MySQLProfile.api._

class ProjectPersistenceImplSpec extends FlatSpec with Matchers with AwaitUtils with InternalTestDatabase with BeforeAndAfterEach {
  import scala.concurrent.ExecutionContext.Implicits.global

  val projectPersistence   = ProjectPersistenceImpl(internalDatabase = internalDatabase)
  val migrationPersistence = MigrationPersistenceImpl(internalDatabase = internalDatabase)
  val project              = TestProject()
  val migration: Migration = Migration.empty(project)

  override def beforeEach(): Unit = {
    super.beforeEach()

    (for {
      _ <- projectPersistence.create(project)
      _ <- migrationPersistence.create(project, migration.copy(hasBeenApplied = true))
    } yield ()).await
  }

  ".load()" should "return None if there's no project yet in the database" in {
    val result = projectPersistence.load("non-existent-id@some-stage").await()
    result should be(None)
  }

  ".load()" should "return the project with the correct revision" in {
    // Create an empty migration to have an unapplied migration with a higher revision
    migrationPersistence.create(project, migration).await

    def loadProject = {
      val result = projectPersistence.load("test-project-id@test-stage").await()
      result shouldNot be(None)
      result
    }

    // Only load the applied revision, which is 1
    loadProject.get.revision shouldEqual 1

    // After another migration is completed, the revision is bumped to the revision of the latest migration
    migrationPersistence.markMigrationAsApplied(migration.copy(revision = 2)).await
    loadProject.get.revision shouldEqual 2
  }

  ".create()" should "store the project in the db" in {
    assertNumberOfRowsInProjectTable(1)
    projectPersistence.create(project.copy(id = "test@test")).await()
    assertNumberOfRowsInProjectTable(2)
  }

  ".loadAll()" should "load all projects (for a user TODO)" in {
    projectPersistence.create(project.copy(id = "test@test")).await()
    projectPersistence.create(project.copy(id = "test2@test")).await()
    projectPersistence.loadAll().await should have(size(3))
  }

  def assertNumberOfRowsInProjectTable(count: Int): Unit = {
    val query = Tables.Projects.size
    runQuery(query.result) should equal(count)
  }

  def runQuery[R](a: DBIOAction[R, NoStream, Nothing]): R = internalDatabase.run(a).await()
}
