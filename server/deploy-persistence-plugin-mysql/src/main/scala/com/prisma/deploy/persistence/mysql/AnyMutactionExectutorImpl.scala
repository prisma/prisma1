package com.prisma.deploy.persistence.mysql

import com.prisma.deploy.migration.mutactions.{AnyMutactionExecutor, ClientSqlMutaction}
import slick.jdbc.MySQLProfile.api._

case class AnyMutactionExectutorImpl(
    database: Database
) extends AnyMutactionExecutor {
  override def execute(mutaction: ClientSqlMutaction) = ???

  override def rollback(mutaction: ClientSqlMutaction) = ???
}
