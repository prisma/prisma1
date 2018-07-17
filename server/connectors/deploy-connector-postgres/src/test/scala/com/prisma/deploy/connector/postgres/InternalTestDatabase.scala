package com.prisma.deploy.connector.postgres

import com.prisma.config.ConfigLoader
import com.prisma.deploy.connector.postgres.database.InternalDatabaseSchema
import com.prisma.utils.await.AwaitUtils
import slick.dbio.{DBIOAction, NoStream}
import slick.jdbc.PostgresProfile.api._

class InternalTestDatabase extends AwaitUtils {
  import scala.concurrent.ExecutionContext.Implicits.global

  val config             = ConfigLoader.load()
  val databaseDefs       = PostgresInternalDatabaseDefs(config.databases.head.copy(pooled = false))
  val setupDatabase      = databaseDefs.setupDatabase
  val managementDatabase = databaseDefs.managementDatabase
  val projectDatabase    = managementDatabase

  def createInternalDatabaseSchema() =
    setupDatabase
      .run(InternalDatabaseSchema.createDatabaseAction(databaseDefs.dbName))
      .transformWith { _ =>
        val action = InternalDatabaseSchema.createSchemaActions(databaseDefs.managementSchemaName, recreate = false)
        managementDatabase.run(action)
      }
      .await(10)

  def truncateTables(): Unit = {
    val schemas = managementDatabase.run(getTables).await()
    managementDatabase.run(dangerouslyTruncateTables(schemas)).await()
  }

  private def dangerouslyTruncateTables(tableNames: Vector[String]): DBIOAction[Unit, NoStream, Effect] = {
    DBIO.seq(tableNames.map(name => sqlu"""TRUNCATE TABLE "#$name" CASCADE"""): _*)
  }

  private def getTables = {
    sql"""SELECT table_name
          FROM information_schema.tables
          WHERE table_schema = '#${databaseDefs.managementSchemaName}'
          AND table_type = 'BASE TABLE';""".as[String]
  }

  def run[R](a: DBIOAction[R, NoStream, Nothing]) = managementDatabase.run(a).await()

  def shutdown() = {
    setupDatabase.close()
    projectDatabase.close()
    managementDatabase.close()
  }
}
