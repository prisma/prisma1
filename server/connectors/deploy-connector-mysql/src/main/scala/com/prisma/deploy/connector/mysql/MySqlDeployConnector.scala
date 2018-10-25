package com.prisma.deploy.connector.mysql

import com.prisma.config.DatabaseConfig
import com.prisma.deploy.connector._
import com.prisma.deploy.connector.jdbc.JdbcTelemetryPersistence
import com.prisma.deploy.connector.mysql.database.{MySqlDeployDatabaseMutationBuilder, MySqlInternalDatabaseSchema}
import com.prisma.deploy.connector.mysql.impls._
import com.prisma.deploy.connector.persistence.{MigrationPersistence, ProjectPersistence, TelemetryPersistence}
import com.prisma.shared.models.ApiConnectorCapability.{MigrationsCapability, NonEmbeddedScalarListCapability}
import com.prisma.shared.models.{Project, ProjectIdEncoder}
import org.joda.time.DateTime
import slick.dbio.Effect.Read
import slick.dbio.{DBIOAction, NoStream}
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.meta.MTable

import scala.concurrent.{ExecutionContext, Future}

case class MySqlDeployConnector(config: DatabaseConfig)(implicit ec: ExecutionContext) extends DeployConnector {
  override def isActive                                      = true
  override def fieldRequirements: FieldRequirementsInterface = FieldRequirementImpl(isActive)

  lazy val internalDatabaseDefs = MySqlInternalDatabaseDefs(config)
  lazy val setupDatabase        = internalDatabaseDefs.setupDatabases
  lazy val databases            = internalDatabaseDefs.managementDatabases
  lazy val managementDatabase   = databases.primary
  lazy val projectDatabase      = databases.primary.database

  override val projectPersistence: ProjectPersistence              = MySqlProjectPersistence(managementDatabase.database)
  override val migrationPersistence: MigrationPersistence          = MySqlMigrationPersistence(managementDatabase.database)
  override val cloudSecretPersistence: MySqlCloudSecretPersistence = MySqlCloudSecretPersistence(managementDatabase.database)
  override val telemetryPersistence: TelemetryPersistence          = JdbcTelemetryPersistence(managementDatabase)

  override def capabilities                                     = Set(MigrationsCapability, NonEmbeddedScalarListCapability)
  override val deployMutactionExecutor: DeployMutactionExecutor = MySqlDeployMutactionExecutor(projectDatabase)

  override def createProjectDatabase(id: String): Future[Unit] = {
    val action = MySqlDeployDatabaseMutationBuilder.createClientDatabaseForProject(projectId = id)
    projectDatabase.run(action)
  }

  override def deleteProjectDatabase(id: String): Future[Unit] = {
    val action = MySqlDeployDatabaseMutationBuilder.deleteProjectDatabase(projectId = id).map(_ => ())
    projectDatabase.run(action)
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

    projectDatabase.run(action)
  }

  override def clientDBQueries(project: Project): ClientDbQueries      = MySqlClientDbQueries(project, projectDatabase)
  override def getOrCreateTelemetryInfo(): Future[TelemetryInfo]       = telemetryPersistence.getOrCreateInfo()
  override def updateTelemetryInfo(lastPinged: DateTime): Future[Unit] = telemetryPersistence.updateTelemetryInfo(lastPinged)
  override def projectIdEncoder: ProjectIdEncoder                      = ProjectIdEncoder('@')

  override def initialize(): Future[Unit] = {
    setupDatabase.primary.database
      .run(MySqlInternalDatabaseSchema.createSchemaActions(internalDatabaseDefs.managementSchemaName, recreate = false))
      .flatMap(_ => internalDatabaseDefs.setupDatabases.shutdown)
  }

  override def reset(): Future[Unit] = truncateTablesInDatabase(managementDatabase.database)

  override def shutdown() = {
    databases.shutdown
  }

  override def databaseIntrospectionInferrer(projectId: String) = EmptyDatabaseIntrospectionInferrer

  protected def truncateTablesInDatabase(database: Database)(implicit ec: ExecutionContext): Future[Unit] = {
    for {
      schemas <- database.run(getTables(internalDatabaseDefs.managementSchemaName))
      _       <- database.run(dangerouslyTruncateTables(schemas))
    } yield ()
  }

  private def getTables(database: String)(implicit ec: ExecutionContext): DBIOAction[Vector[String], NoStream, Read] = {
    for {
      metaTables <- MTable.getTables(cat = Some(database), schemaPattern = None, namePattern = None, types = None)
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
