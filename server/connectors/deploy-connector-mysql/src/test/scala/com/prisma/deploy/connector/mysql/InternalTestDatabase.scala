package com.prisma.deploy.connector.mysql

import com.prisma.config.ConfigLoader
import com.prisma.deploy.connector.mysql.database.MysqlInternalDatabaseSchema
import com.prisma.utils.await.AwaitUtils
import slick.dbio.Effect.Read
import slick.dbio.{DBIOAction, NoStream}
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.meta.MTable

class InternalTestDatabase extends AwaitUtils {
  import scala.concurrent.ExecutionContext.Implicits.global

  val config             = ConfigLoader.load()
  val databaseDefs       = MysqlInternalDatabaseDefs(config.databases.head.copy(pooled = false))
  val managementDatabase = databaseDefs.managementDatabase
  val projectDatabase    = managementDatabase

  def createInternalDatabaseSchema() =
    managementDatabase.run(MysqlInternalDatabaseSchema.createSchemaActions(databaseDefs.managementSchemaName, recreate = true)).await(10)

  def truncateTables(): Unit = {
    val schemas = managementDatabase.run(getTables(databaseDefs.managementSchemaName)).await()
    managementDatabase.run(dangerouslyTruncateTables(schemas)).await()
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

  def run[R](a: DBIOAction[R, NoStream, Nothing]) = projectDatabase.run(a).await()

  def shutdown() = {
    managementDatabase.close()
    projectDatabase.close()
  }
}
