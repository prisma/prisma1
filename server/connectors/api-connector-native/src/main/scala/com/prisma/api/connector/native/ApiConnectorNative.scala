package com.prisma.api.connector.native

import java.sql.Driver

import com.prisma.api.connector.postgres.PostgresApiConnector
import com.prisma.api.connector.sqlite.SQLiteApiConnector
import com.prisma.api.connector.mysql.MySqlApiConnector
import com.prisma.api.connector.{ApiConnector, DataResolver, DatabaseMutactionExecutor}
import com.prisma.config.DatabaseConfig
import com.prisma.shared.models.{ConnectorCapabilities, Project, ProjectIdEncoder}

import scala.concurrent.{ExecutionContext, Future}

trait Backup {
  def driver: Driver
}

case class SqliteBackup(driver: Driver) extends Backup
case class MysqlBackup(driver: Driver) extends Backup
case class PostgresBackup(driver: Driver) extends Backup

case class ApiConnectorNative(config: DatabaseConfig, backup: Backup)(implicit ec: ExecutionContext) extends ApiConnector {
  override def initialize(): Future[Unit] = Future.unit
  override def shutdown(): Future[Unit] = Future.unit

  override def databaseMutactionExecutor: DatabaseMutactionExecutor = {
    backup match {
      case SqliteBackup(driver) => {
        val base = SQLiteApiConnector(config, driver)
        NativeDatabaseMutactionExecutor(base.databaseMutactionExecutor.slickDatabase)
      }
      case PostgresBackup(driver) => {
        val base = PostgresApiConnector(config, driver)
        NativeDatabaseMutactionExecutor(base.databaseMutactionExecutor.slickDatabase)
      }
      case MysqlBackup(driver) => {
        val base = MySqlApiConnector(config, driver)
        NativeDatabaseMutactionExecutor(base.databaseMutactionExecutor.slickDatabase)
      }
    }
  }

  override def dataResolver(project: Project): DataResolver       = NativeDataResolver(project)
  override def masterDataResolver(project: Project): DataResolver = NativeDataResolver(project)
  override def projectIdEncoder: ProjectIdEncoder                 = ProjectIdEncoder('_')

  override val capabilities = ConnectorCapabilities.sqliteNative
}
