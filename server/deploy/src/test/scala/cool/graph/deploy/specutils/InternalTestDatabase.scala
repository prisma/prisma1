package cool.graph.deploy.specutils

import cool.graph.deploy.database.schema.InternalDatabaseSchema
import cool.graph.utils.await.AwaitUtils
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}
import slick.dbio.DBIOAction
import slick.jdbc.MySQLProfile.api._
import slick.dbio.Effect.Read
import slick.jdbc.meta.MTable

import scala.concurrent.Future

class InternalTestDatabase extends AwaitUtils { //this: Suite =>
  import scala.concurrent.ExecutionContext.Implicits.global

  val dbDriver             = new org.mariadb.jdbc.Driver
  val internalDatabaseRoot = Database.forConfig("internalRoot", driver = dbDriver)
  val internalDatabase     = Database.forConfig("internal", driver = dbDriver)

  def createInternalDatabaseSchema() = internalDatabaseRoot.run(InternalDatabaseSchema.createSchemaActions(recreate = true)).await(10)

  def truncateTables(): Unit = {
    val schemas = internalDatabase.run(getTables("graphcool")).await()
    internalDatabase.run(dangerouslyTruncateTables(schemas)).await()
  }

  private def dangerouslyTruncateTables(tableNames: Vector[String]): DBIOAction[Unit, NoStream, Effect] = {
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

  def shutdown() = {
    internalDatabaseRoot.close()
    internalDatabase.close()
  }
}
