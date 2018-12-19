package com.prisma.deploy.connector.jdbc.database

import com.prisma.deploy.connector.DeployMutaction
import slick.dbio.DBIO

trait SqlMutactionInterpreter[T <: DeployMutaction] {
  def execute(mutaction: T): DBIO[Any]
  def rollback(mutaction: T): DBIO[Any]
}
