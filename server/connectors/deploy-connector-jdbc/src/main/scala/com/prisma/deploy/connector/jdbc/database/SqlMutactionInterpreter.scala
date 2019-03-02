package com.prisma.deploy.connector.jdbc.database

import com.prisma.deploy.connector.{DeployMutaction, DatabaseSchema}
import slick.dbio.DBIO

trait SqlMutactionInterpreter[T <: DeployMutaction] {
  def execute(mutaction: T, schemaBeforeMigration: DatabaseSchema): DBIO[Any]
  def rollback(mutaction: T, schemaBeforeMigration: DatabaseSchema): DBIO[Any]
}
