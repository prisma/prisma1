package com.prisma.deploy.connector.postgres

import com.prisma.config.DatabaseConfig
import com.prisma.deploy.connector._
import com.prisma.deploy.connector.jdbc.{JdbcCloudSecretPersistence, JdbcTelemetryPersistence}
import com.prisma.deploy.connector.persistence.{CloudSecretPersistence, MigrationPersistence, ProjectPersistence, TelemetryPersistence}
import com.prisma.deploy.connector.postgres.database.{InternalDatabaseSchema, PostgresDeployDatabaseMutationBuilder}
import com.prisma.deploy.connector.postgres.impls._
import com.prisma.shared.models.ApiConnectorCapability.{MigrationsCapability, NonEmbeddedScalarListCapability}
import com.prisma.shared.models.{ConnectorCapability, Project, ProjectIdEncoder}
import org.joda.time.DateTime
import slick.dbio.Effect.Read
import slick.dbio.{DBIOAction, NoStream}
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}

case class PostgresDeployConnector(
    dbConfig: DatabaseConfig,
    isActive: Boolean
)(implicit ec: ExecutionContext)
    extends DeployConnector {

  override def fieldRequirements: FieldRequirementsInterface = FieldRequirementImpl(isActive)

  lazy val internalDatabases   = PostgresInternalDatabaseDefs(dbConfig)
  lazy val setupDatabases      = internalDatabases.setupDatabase
  lazy val managementDatabases = internalDatabases.managementDatabase
  lazy val managementDatabase  = managementDatabases.primary.database
  lazy val projectDatabase     = managementDatabase

  override lazy val projectPersistence: ProjectPersistence         = PostgresProjectPersistence(managementDatabase)
  override lazy val migrationPersistence: MigrationPersistence     = PostgresMigrationPersistence(managementDatabase)
  override lazy val cloudSecretPersistence: CloudSecretPersistence = JdbcCloudSecretPersistence(managementDatabases.primary)
  override lazy val telemetryPersistence: TelemetryPersistence     = JdbcTelemetryPersistence(managementDatabases.primary)

  override lazy val deployMutactionExecutor: DeployMutactionExecutor = PostgresDeployMutactionExecutor(projectDatabase)
  override def capabilities: Set[ConnectorCapability]                = if (isActive) Set(MigrationsCapability, NonEmbeddedScalarListCapability) else Set.empty

  override def createProjectDatabase(id: String): Future[Unit] = {
    val action = PostgresDeployDatabaseMutationBuilder.createClientDatabaseForProject(projectId = id)
    projectDatabase.run(action)
  }

  override def deleteProjectDatabase(id: String): Future[Unit] = {
    val action = PostgresDeployDatabaseMutationBuilder.deleteProjectDatabase(projectId = id).map(_ => ())
    projectDatabase.run(action)
  }

  override def getAllDatabaseSizes(): Future[Vector[DatabaseSize]] = {
    val action = {
      val query = sql"""
           SELECT schemaname, SUM(pg_total_relation_size(quote_ident(schemaname) || '.' || quote_ident(tablename))) / 1024 / 1024 FROM pg_tables GROUP BY schemaname
         """
      query.as[(String, Double)].map { tuples =>
        tuples.map { tuple =>
          DatabaseSize(tuple._1, tuple._2)
        }
      }
    }

    projectDatabase.run(action)
  }

  override def clientDBQueries(project: Project): ClientDbQueries      = PostgresClientDbQueries(project, projectDatabase)
  override def getOrCreateTelemetryInfo(): Future[TelemetryInfo]       = telemetryPersistence.getOrCreateInfo()
  override def updateTelemetryInfo(lastPinged: DateTime): Future[Unit] = telemetryPersistence.updateTelemetryInfo(lastPinged)
  override def projectIdEncoder: ProjectIdEncoder                      = ProjectIdEncoder('$')

  override def initialize(): Future[Unit] = {
    // We're ignoring failures for createDatabaseAction as there is no "create if not exists" in psql
    setupDatabases.primary.database
      .run(InternalDatabaseSchema.createDatabaseAction(internalDatabases.dbName))
      .transformWith { _ =>
        val action = InternalDatabaseSchema.createSchemaActions(internalDatabases.managementSchemaName, recreate = false)
        projectDatabase.run(action)
      }
      .flatMap(_ => setupDatabases.shutdown)
  }

  override def reset(): Future[Unit] = truncateManagementTablesInDatabase(managementDatabase)

  override def shutdown() = {
    managementDatabases.shutdown
  }

  override def databaseIntrospectionInferrer(projectId: String): DatabaseIntrospectionInferrer = {
    if (isActive) {
      EmptyDatabaseIntrospectionInferrer
    } else {
      val schema = dbConfig.schema.getOrElse(projectId).toLowerCase
      DatabaseIntrospectionInferrerImpl(projectDatabase, schema)
    }
  }

  protected def truncateManagementTablesInDatabase(database: Database)(implicit ec: ExecutionContext): Future[Unit] = {
    for {
      schemas <- database.run(getTables())
      _       <- database.run(dangerouslyTruncateTables(schemas))
    } yield ()
  }

  private def getTables()(implicit ec: ExecutionContext): DBIOAction[Vector[String], NoStream, Read] = {
    sql"""SELECT table_name
          FROM information_schema.tables
          WHERE table_schema = '#${internalDatabases.managementSchemaName}'
          AND table_type = 'BASE TABLE';""".as[String]
  }

  private def dangerouslyTruncateTables(tableNames: Vector[String]): DBIOAction[Unit, NoStream, Effect] = {
    DBIO.seq(tableNames.map(name => sqlu"""TRUNCATE TABLE "#$name" cascade"""): _*)
  }

}
