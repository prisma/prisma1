package cool.graph.deploy

import cool.graph.deploy.database.persistence.ModelToDbMapper
import cool.graph.deploy.database.schema.InternalDatabaseSchema
import cool.graph.deploy.database.tables.Tables
import cool.graph.shared.project_dsl.TestClient
import cool.graph.util.AwaitUtils
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}
import slick.dbio.DBIOAction
import slick.dbio.Effect.Read
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.meta.MTable

import scala.concurrent.Future

trait InternalTestDatabase extends BeforeAndAfterAll with BeforeAndAfterEach with AwaitUtils { this: Suite =>
  import scala.concurrent.ExecutionContext.Implicits.global

  val dbDriver             = new org.mariadb.jdbc.Driver
  val internalDatabaseRoot = Database.forConfig("internalRoot", driver = dbDriver)
  val internalDatabase     = Database.forConfig("internal", driver = dbDriver)

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    createInternalDatabaseSchema
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    truncateTables()
    createTestClient
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    val shutdowns = Vector(internalDatabase.shutdown, internalDatabaseRoot.shutdown)
    Future.sequence(shutdowns).await()
  }

  private def createInternalDatabaseSchema = internalDatabaseRoot.run(InternalDatabaseSchema.createSchemaActions(recreate = true)).await(10)
  private def createTestClient             = internalDatabase.run { Tables.Clients += ModelToDbMapper.convert(TestClient()) }

  protected def truncateTables(): Unit = {
    val schemas = internalDatabase.run(getTables("graphcool")).await()
    internalDatabase.run(dangerouslyTruncateTable(schemas)).await()
  }

  private def dangerouslyTruncateTable(tableNames: Vector[String]): DBIOAction[Unit, NoStream, Effect] = {
    DBIO.seq(
      List(sqlu"""SET FOREIGN_KEY_CHECKS=0""") ++
        tableNames.map(name => sqlu"TRUNCATE TABLE `#$name`") ++
        List(sqlu"""SET FOREIGN_KEY_CHECKS=1"""): _*
    )
  }

  private def getTables(projectId: String): DBIOAction[Vector[String], NoStream, Read] = {
    for {
      metaTables <- MTable.getTables(cat = Some(projectId), schemaPattern = None, namePattern = None, types = None)
    } yield metaTables.map(table => table.name.name)
  }
}
