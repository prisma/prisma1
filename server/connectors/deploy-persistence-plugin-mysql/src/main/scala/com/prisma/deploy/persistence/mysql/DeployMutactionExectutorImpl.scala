package com.prisma.deploy.persistence.mysql

import com.prisma.deploy.migration.mutactions.{DeployMutactionExecutor, DeployMutaction}
import com.prisma.deploy.mutactions.AnyMutactionInterpreterImpl
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

case class DeployMutactionExectutorImpl(
    database: Database
)(implicit ec: ExecutionContext)
    extends DeployMutactionExecutor {

  override def execute(mutaction: DeployMutaction): Future[Unit] = {
    val action = AnyMutactionInterpreterImpl.execute(mutaction)
    database.run(action).map(_ => ())
  }

  override def rollback(mutaction: DeployMutaction): Future[Unit] = {
    val action = AnyMutactionInterpreterImpl.rollback(mutaction)
    database.run(action).map(_ => ())
  }

}
