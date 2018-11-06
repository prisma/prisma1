package com.prisma.deploy.connector.postgres

import com.prisma.config.DatabaseConfig
import com.prisma.deploy.connector._
import com.prisma.deploy.connector.postgres.database.{InternalDatabaseSchema, PostgresDeployDatabaseMutationBuilder, TelemetryTable}
import com.prisma.deploy.connector.postgres.impls._
import com.prisma.shared.models.ApiConnectorCapability._
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

  lazy val internalDatabaseDefs = PostgresInternalDatabaseDefs(dbConfig)
  lazy val projectDatabase      = internalDatabaseDefs.managementDatabase
  lazy val managementDatabase   = internalDatabaseDefs.managementDatabase

  override lazy val projectPersistence: ProjectPersistence           = ProjectPersistenceImpl(managementDatabase)
  override lazy val migrationPersistence: MigrationPersistence       = MigrationPersistenceImpl(managementDatabase)
  override lazy val deployMutactionExecutor: DeployMutactionExecutor = PostgresDeployMutactionExecutor(projectDatabase)
  override def capabilities: Set[ConnectorCapability] = {
    val common: Set[ConnectorCapability] = Set(TransactionalExecutionCapability, JoinRelationsFilterCapability)
    if (isActive) common ++ Set(MigrationsCapability, NonEmbeddedScalarListCapability) else common
  }

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
  override def getOrCreateTelemetryInfo(): Future[TelemetryInfo]       = managementDatabase.run(TelemetryTable.getOrCreateInfo())
  override def updateTelemetryInfo(lastPinged: DateTime): Future[Unit] = managementDatabase.run(TelemetryTable.updateInfo(lastPinged)).map(_ => ())
  override def projectIdEncoder: ProjectIdEncoder                      = ProjectIdEncoder('$')
  override def cloudSecretPersistence: CloudSecretPersistence          = CloudSecretPersistenceImpl(managementDatabase)

  override def initialize(): Future[Unit] = {
    // We're ignoring failures for createDatabaseAction as there is no "create if not exists" in psql
    internalDatabaseDefs.setupDatabase
      .run(InternalDatabaseSchema.createDatabaseAction(internalDatabaseDefs.dbName))
      .transformWith { _ =>
        val action = InternalDatabaseSchema.createSchemaActions(internalDatabaseDefs.managementSchemaName, recreate = false)
        projectDatabase.run(action)
      }
      .flatMap(_ => internalDatabaseDefs.setupDatabase.shutdown)
  }

  override def reset(): Future[Unit] = truncateManagementTablesInDatabase(managementDatabase)

  override def shutdown() = {
    for {
      _ <- projectDatabase.shutdown
      _ <- managementDatabase.shutdown
    } yield ()
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
          WHERE table_schema = '#${internalDatabaseDefs.managementSchemaName}'
          AND table_type = 'BASE TABLE';""".as[String]
  }

  private def dangerouslyTruncateTables(tableNames: Vector[String]): DBIOAction[Unit, NoStream, Effect] = {
    DBIO.seq(tableNames.map(name => sqlu"""TRUNCATE TABLE "#$name" cascade"""): _*)
  }
}
