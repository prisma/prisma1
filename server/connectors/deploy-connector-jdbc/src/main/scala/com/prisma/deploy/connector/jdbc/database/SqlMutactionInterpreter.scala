package com.prisma.deploy.connector.jdbc.database

import com.prisma.deploy.connector.{DeployMutaction, Tables}
import slick.dbio.DBIO

trait SqlMutactionInterpreter[T <: DeployMutaction] {
  def execute(mutaction: T, schemaBeforeMigration: Tables): DBIO[Any]
  def rollback(mutaction: T, schemaBeforeMigration: Tables): DBIO[Any]
}
