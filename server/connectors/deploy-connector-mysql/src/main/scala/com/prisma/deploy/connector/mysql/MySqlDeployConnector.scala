package com.prisma.deploy.connector.mysql

import com.prisma.config.DatabaseConfig
import com.prisma.deploy.connector._
import com.prisma.deploy.connector.mysql.database.{DeployDatabaseMutationBuilderMySql, InternalDatabaseSchema, TelemetryTable}
import com.prisma.deploy.connector.mysql.impls.{ClientDbQueriesImpl, MigrationPersistenceImpl, MySqlDeployMutactionExectutor, ProjectPersistenceImpl}
import com.prisma.shared.models.{Project, ProjectIdEncoder}
import org.joda.time.DateTime
import slick.dbio.Effect.Read
import slick.dbio.{DBIOAction, NoStream}
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.meta.MTable

import scala.concurrent.{ExecutionContext, Future}

case class MySqlDeployConnector(config: DatabaseConfig)(implicit ec: ExecutionContext) extends DeployConnector with TableTruncationHelpers {
  override def isActive         = true
  lazy val internalDatabaseDefs = InternalDatabaseDefs(config)
  lazy val internalDatabaseRoot = internalDatabaseDefs.internalDatabaseRoot
  lazy val internalDatabase     = internalDatabaseDefs.internalDatabase
  lazy val clientDatabase       = internalDatabaseDefs.internalDatabaseRoot

  override val projectPersistence: ProjectPersistence           = ProjectPersistenceImpl(internalDatabase)
  override val migrationPersistence: MigrationPersistence       = MigrationPersistenceImpl(internalDatabase)
  override val deployMutactionExecutor: DeployMutactionExecutor = MySqlDeployMutactionExectutor(clientDatabase)

  override def createProjectDatabase(id: String): Future[Unit] = {
    val action = DeployDatabaseMutationBuilderMySql.createClientDatabaseForProject(projectId = id)
    clientDatabase.run(action)
  }

  override def deleteProjectDatabase(id: String): Future[Unit] = {
    val action = DeployDatabaseMutationBuilderMySql.deleteProjectDatabase(projectId = id).map(_ => ())
    clientDatabase.run(action)
  }

  override def getAllDatabaseSizes(): Future[Vector[DatabaseSize]] = {
    val action = {
      val query = sql"""SELECT table_schema, sum( data_length + index_length) / 1024 / 1024 FROM information_schema.TABLES GROUP BY table_schema"""
      query.as[(String, Double)].map { tuples =>
        tuples.map { tuple =>
          DatabaseSize(tuple._1, tuple._2)
        }
      }
    }

    clientDatabase.run(action)
  }

  override def clientDBQueries(project: Project): ClientDbQueries      = ClientDbQueriesImpl(project, clientDatabase)
  override def getOrCreateTelemetryInfo(): Future[TelemetryInfo]       = internalDatabaseRoot.run(TelemetryTable.getOrCreateInfo())
  override def updateTelemetryInfo(lastPinged: DateTime): Future[Unit] = internalDatabaseRoot.run(TelemetryTable.updateInfo(lastPinged)).map(_ => ())
  override def projectIdEncoder: ProjectIdEncoder                      = ProjectIdEncoder('@')

  override def initialize(): Future[Unit] = {
    val action = InternalDatabaseSchema.createSchemaActions(recreate = false)
    internalDatabaseRoot.run(action)
  }

  override def reset(): Future[Unit] = truncateTablesInDatabase(internalDatabase)

  override def shutdown() = {
    for {
      _ <- internalDatabaseRoot.shutdown
      _ <- internalDatabase.shutdown
    } yield ()
  }

  override def databaseIntrospectionInferrer(project: Project) = EmptyDatabaseIntrospectionInferrer
}

trait TableTruncationHelpers {
  // copied from InternalTestDatabase

  protected def truncateTablesInDatabase(database: Database)(implicit ec: ExecutionContext): Future[Unit] = {
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
