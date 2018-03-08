package com.prisma.deploy

import com.prisma.deploy.database.DatabaseMutationBuilder
import com.prisma.deploy.database.persistence.mysql.MigrationPersistenceImpl
import com.prisma.deploy.database.persistence.{MigrationPersistence, ProjectPersistence, ProjectPersistenceImpl}
import com.prisma.deploy.database.schema.InternalDatabaseSchema
import com.prisma.deploy.migration.mutactions.DeployMutactionExecutor
import com.prisma.deploy.persistence.mysql.DeployMutactionExectutorImpl
import com.prisma.deploy.persistence.{DatabaseSize, DeployPersistencePlugin}
import slick.dbio.Effect.Read
import slick.dbio.{DBIOAction, NoStream}
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.meta.MTable

import scala.concurrent.{ExecutionContext, Future}

class MySqlDeployPersistencePlugin()(implicit ec: ExecutionContext) extends DeployPersistencePlugin with TableTruncationHelpers {
  val dbDriver             = new org.mariadb.jdbc.Driver
  val internalDatabaseRoot = Database.forConfig("internalRoot", driver = dbDriver)
  val internalDatabase     = Database.forConfig("internal", driver = dbDriver)

  override val projectPersistence: ProjectPersistence           = ProjectPersistenceImpl(internalDatabase)
  override val migrationPersistence: MigrationPersistence       = MigrationPersistenceImpl(internalDatabase)
  override val deployMutactionExecutor: DeployMutactionExecutor = DeployMutactionExectutorImpl(internalDatabase)

  override def createProjectDatabase(id: String): Future[Unit] = {
    val action = DatabaseMutationBuilder.createClientDatabaseForProject(projectId = id)
    internalDatabase.run(action)
  }

  override def deleteProjectDatabase(id: String): Future[Unit] = {
    val action = DatabaseMutationBuilder.deleteProjectDatabase(projectId = id).map(_ => ())
    internalDatabase.run(action)
  }

  override def getAllDatabaseSizes(): Future[Vector[DatabaseSize]] = {
    val action = {
      val query = sql"""
           SELECT table_schema, sum( data_length + index_length) / 1024 / 1024 FROM information_schema.TABLES GROUP BY table_schema
         """
      query.as[(String, Double)].map { tuples =>
        tuples.map { tuple =>
          DatabaseSize(tuple._1, tuple._2)
        }
      }
    }
    internalDatabase.run(action)
  }

  override def initialize(): Future[Unit] = {
    val action = InternalDatabaseSchema.createSchemaActions(recreate = false)
    internalDatabaseRoot.run(action)
  }

  override def reset(): Future[Unit] = truncateTablesInDatabse(internalDatabase)

  override def shutdown() = {
    for {
      _ <- internalDatabaseRoot.shutdown
      _ <- internalDatabase.shutdown
    } yield ()
  }
}

trait TableTruncationHelpers {
  // copied from InternalTestDatabase

  protected def truncateTablesInDatabse(database: Database)(implicit ec: ExecutionContext): Future[Unit] = {
    for {
      schemas <- database.run(getTables("graphcool"))
      _       <- database.run(dangerouslyTruncateTables(schemas))
    } yield ()
  }

  private def getTables(projectId: String)(implicit ec: ExecutionContext): DBIOAction[Vector[String], NoStream, Read] = {
    for {
      metaTables <- MTable.getTables(cat = Some(projectId), schemaPattern = None, namePattern = None, types = None)
    } yield metaTables.map(table => table.name.name)
  }

  private def dangerouslyTruncateTables(tableNames: Vector[String]): DBIOAction[Unit, NoStream, Effect] = {
    DBIO.seq(
      List(sqlu"""SET FOREIGN_KEY_CHECKS=0""") ++
        tableNames.map(name => sqlu"TRUNCATE TABLE `#$name`") ++
        List(sqlu"""SET FOREIGN_KEY_CHECKS=1"""): _*
    )
  }
}
