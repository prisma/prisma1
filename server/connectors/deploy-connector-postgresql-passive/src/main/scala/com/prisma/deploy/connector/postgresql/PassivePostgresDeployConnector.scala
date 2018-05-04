package com.prisma.deploy.connector.postgresql

import com.prisma.config.DatabaseConfig
import com.prisma.deploy.connector.DeployConnector
import com.prisma.shared.models.Project

import scala.concurrent.ExecutionContext

class PassivePostgresDeployConnector(dbConfig: DatabaseConfig)(implicit ec: ExecutionContext)
    extends PostgresDeployConnector(dbConfig)
    with DeployConnector
    with TableTruncationHelpers {

  override def isActive = false

  override def databaseIntrospectionInferrer(project: Project) = {
    DatabaseIntrospectionInferrerImpl(clientDatabase, project.id)
  }
}
