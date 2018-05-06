package com.prisma.deploy.connector.mysql.impls

import com.prisma.deploy.connector.mysql.impls.mutactions.MySqlAnyMutactionInterpreter
import com.prisma.deploy.connector.{DeployMutaction, DeployMutactionExecutor}
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

case class MySqlDeployMutactionExectutor(database: Database)(implicit ec: ExecutionContext) extends DeployMutactionExecutor {
  override def execute(mutaction: DeployMutaction): Future[Unit] = {
    val action = MySqlAnyMutactionInterpreter.execute(mutaction)
    database.run(action).map(_ => ())
  }

  override def rollback(mutaction: DeployMutaction): Future[Unit] = {
    val action = MySqlAnyMutactionInterpreter.rollback(mutaction)
    database.run(action).map(_ => ())
  }

}
