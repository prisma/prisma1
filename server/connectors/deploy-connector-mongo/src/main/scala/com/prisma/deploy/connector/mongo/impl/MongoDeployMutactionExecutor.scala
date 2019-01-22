package com.prisma.deploy.connector.mongo.impl

import com.prisma.deploy.connector.mongo.impl.mutactions.MongoAnyMutactionInterpreter
import com.prisma.deploy.connector.{DeployMutaction, DeployMutactionExecutor, DatabaseSchema}
import org.mongodb.scala.{MongoClient, MongoDatabase}

import scala.concurrent.{ExecutionContext, Future}

case class MongoDeployMutactionExecutor(client: MongoClient)(implicit ec: ExecutionContext) extends DeployMutactionExecutor {
  override def execute(mutaction: DeployMutaction, schemaBeforeMigration: DatabaseSchema): Future[Unit] = {
    val action = MongoAnyMutactionInterpreter.execute(mutaction)
    run(client.getDatabase(mutaction.project.dbName), action).map(_ => ())
  }

  override def rollback(mutaction: DeployMutaction, schemaBeforeMigration: DatabaseSchema): Future[Unit] = {
    val action = MongoAnyMutactionInterpreter.rollback(mutaction)
    run(client.getDatabase(mutaction.project.dbName), action).map(_ => ())
  }

  def run(database: MongoDatabase, action: DeployMongoAction): Future[Unit] = action.fn(database)

}
case class DeployMongoAction(fn: MongoDatabase => Future[Unit])
