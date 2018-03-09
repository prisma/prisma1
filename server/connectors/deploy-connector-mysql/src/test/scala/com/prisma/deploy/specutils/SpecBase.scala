package com.prisma.deploy.specutils

import com.prisma.deploy.connector.mysql.impls.{MigrationPersistenceImpl, ProjectPersistenceImpl}
import com.prisma.shared.models._
import com.prisma.utils.await.AwaitUtils
import cool.graph.cuid.Cuid
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}
import play.api.libs.json.JsString

trait SpecBase extends BeforeAndAfterEach with BeforeAndAfterAll with AwaitUtils { self: Suite =>
  import scala.concurrent.ExecutionContext.Implicits.global

  val internalDb = new InternalTestDatabase

  val basicTypesGql =
    """
      |type TestModel {
      |  id: ID! @unique
      |}
    """.stripMargin.trim()

  val projectPersistence   = ProjectPersistenceImpl(internalDb.internalDatabase)
  val migrationPersistence = MigrationPersistenceImpl(internalDb.internalDatabase)

  def newTestProject(projectId: String = Cuid.createCuid()) = {
    Project(id = projectId, ownerId = Cuid.createCuid(), schema = Schema())
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    internalDb.createInternalDatabaseSchema()
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    internalDb.shutdown()
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    internalDb.truncateTables()
  }

  def setupProject(
      schema: String,
      name: String = Cuid.createCuid(),
      stage: String = Cuid.createCuid()
  ): (Project, Migration) = {
    val projectId = name + "@" + stage
    val project   = newTestProject(projectId)
    projectPersistence.create(project).await()

    val migration = Migration.empty(project.id)
    val result    = migrationPersistence.create(migration).await()
    migrationPersistence.updateMigrationStatus(result.id, MigrationStatus.Success).await()

    (
      projectPersistence.load(projectId).await.get,
      migrationPersistence.byId(MigrationId(projectId, result.revision)).await.get
    )
  }

  def formatSchema(schema: String): String = JsString(schema).toString()
  def escapeString(str: String): String    = JsString(str).toString()
}
