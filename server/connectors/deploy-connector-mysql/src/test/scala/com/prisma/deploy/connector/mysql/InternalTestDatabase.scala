package com.prisma.deploy.connector.mysql

import com.prisma.config.ConfigLoader
import com.prisma.deploy.connector.mysql.database.InternalDatabaseSchema
import com.prisma.utils.await.AwaitUtils
import slick.dbio.Effect.Read
import slick.dbio.{DBIOAction, NoStream}
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.meta.MTable

import scala.util.{Failure, Success}

class InternalTestDatabase extends AwaitUtils {
  import scala.concurrent.ExecutionContext.Implicits.global

  val config = ConfigLoader.load() match {
    case Success(c)   => c
    case Failure(err) => sys.error(s"Unable to load Prisma config: $err")
  }

  val databaseDefs         = InternalDatabaseDefs(config.databases.head)
  val internalDatabaseRoot = databaseDefs.internalDatabaseRoot
  val internalDatabase     = databaseDefs.internalDatabase

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

  def run[R](a: DBIOAction[R, NoStream, Nothing]) = internalDatabase.run(a).await()

  def shutdown() = {
    internalDatabaseRoot.close()
    internalDatabase.close()
  }
}
