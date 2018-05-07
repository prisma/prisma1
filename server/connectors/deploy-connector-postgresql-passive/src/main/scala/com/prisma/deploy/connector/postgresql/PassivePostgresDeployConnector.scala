package com.prisma.deploy.connector.postgresql

import com.prisma.config.DatabaseConfig
import com.prisma.deploy.connector.DeployConnector

import scala.concurrent.ExecutionContext

class PassivePostgresDeployConnector(dbConfig: DatabaseConfig)(implicit ec: ExecutionContext)
    extends PostgresDeployConnector(dbConfig)
    with DeployConnector
    with TableTruncationHelpers {

  override def isActive = false

  override def databaseIntrospectionInferrer(projectId: String) = {
    val schema = dbConfig.schema.getOrElse(projectId)
    DatabaseIntrospectionInferrerImpl(internalDatabase, schema)
  }
}
