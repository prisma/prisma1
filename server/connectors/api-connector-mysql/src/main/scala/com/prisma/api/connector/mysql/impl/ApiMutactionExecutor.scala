package com.prisma.api.connector.mysql.impl

import com.prisma.api.connector.mysql.DatabaseMutactionInterpreter
import com.prisma.api.connector.{DatabaseMutaction, DatabaseMutactionExecutor}
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

case class DatabaseMutactionExecutorImpl(
    databaseMutactionInterpreter: DatabaseMutactionInterpreter[DatabaseMutaction],
    clientDb: Database
)(implicit ec: ExecutionContext)
    extends DatabaseMutactionExecutor {
  override def execute(mutaction: DatabaseMutaction): Future[Unit] = {
    mutaction match {
      case m: DatabaseMutaction =>
        val dbAction = databaseMutactionInterpreter.action(m)
        clientDb.run(dbAction).map(_ => ())
    }
  }
}
