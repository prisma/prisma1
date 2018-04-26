package com.prisma.api.connector.postgresql

import com.prisma.api.connector._
import com.prisma.api.connector.postgresql.database.{Databases, PostgresDataResolver}
import com.prisma.api.connector.postgresql.impl._
import com.prisma.config.DatabaseConfig
import com.prisma.shared.models.{Project, ProjectIdEncoder}
import slick.jdbc.TransactionIsolation

import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.PostgresProfile.api._

case class PassivePostgresApiConnector(config: DatabaseConfig)(implicit ec: ExecutionContext) extends ApiConnector {
  lazy val databases = Databases.initialize(config)

  val activeConnector = PostgresApiConnector(config)

  override def initialize() = {
    databases
    Future.unit
  }

  override def shutdown() = {
    for {
      _ <- databases.master.shutdown
      _ <- databases.readOnly.shutdown
    } yield ()
  }

  override def databaseMutactionExecutor: DatabaseMutactionExecutor =
    PassiveDatabaseMutactionExecutorImpl(activeConnector.databaseMutactionExecutor.asInstanceOf[DatabaseMutactionExecutorImpl])
  override def dataResolver(project: Project)       = activeConnector.dataResolver(project)
  override def masterDataResolver(project: Project) = activeConnector.masterDataResolver(project)

  override def projectIdEncoder: ProjectIdEncoder = activeConnector.projectIdEncoder
}

case class PassiveDatabaseMutactionExecutorImpl(activeExecutor: DatabaseMutactionExecutorImpl)(implicit ec: ExecutionContext)
    extends DatabaseMutactionExecutor {
//  override def execute(mutactions: Vector[DatabaseMutaction], runTransactionally: Boolean) = {
//
//    activeExecutor.execute(mutactions, runTransactionally)
//  }

  override def execute(mutactions: Vector[DatabaseMutaction], runTransactionally: Boolean): Future[Unit] = {
    val interpreters        = mutactions.map(interpreterFor)
    val combinedErrorMapper = interpreters.map(_.errorMapper).reduceLeft(_ orElse _)

    val singleAction = runTransactionally match {
      case true  => DBIO.seq(interpreters.map(_.action): _*).transactionally
      case false => DBIO.seq(interpreters.map(_.action): _*)
    }

    activeExecutor.clientDb
      .run(singleAction.withTransactionIsolation(TransactionIsolation.ReadCommitted))
      .recover { case error => throw combinedErrorMapper.lift(error).getOrElse(error) }
      .map(_ => ())
  }

  def interpreterFor(mutaction: DatabaseMutaction): DatabaseMutactionInterpreter = mutaction match {
    case m: CreateDataItem => CreateDataItemInterpreter(m, includeRelayRow = false)
    case mutaction         => activeExecutor.interpreterFor(mutaction)
  }
}
