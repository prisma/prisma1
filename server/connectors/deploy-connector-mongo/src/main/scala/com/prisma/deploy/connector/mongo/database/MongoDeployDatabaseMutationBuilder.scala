package com.prisma.deploy.connector.mongo.database

import com.prisma.deploy.connector.mongo.impl.DeployMongoAction
import com.prisma.shared.models.Project
import com.prisma.shared.models.TypeIdentifier.ScalarTypeIdentifier
import org.mongodb.scala.MongoNamespace

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object NoAction {
  val unit = {
    DeployMongoAction { database =>
      Future.successful(())
    }
  }
}

object MongoDeployDatabaseMutationBuilder {

  def createClientDatabaseForProject(projectId: String) = DeployMongoAction { database =>
    database.listCollectionNames().toFuture().map(_ => ())
  }
  def truncateProjectTables(project: Project) = DeployMongoAction { database =>
    database.drop().toFuture().map(_ -> Unit)
  }

  def deleteProjectDatabase(projectId: String) = DeployMongoAction { database =>
    database.drop().toFuture().map(_ -> Unit)
  }

  def dropTable(projectId: String, tableName: String) = DeployMongoAction { database =>
    database.getCollection(tableName).drop().toFuture().map(_ -> Unit)
  }

  def renameTable(projectId: String, name: String, newName: String) = DeployMongoAction { database =>
    database.getCollection(name).renameCollection(MongoNamespace(projectId, newName)).toFuture().map(_ -> Unit)
  }

  def removeUniqueConstraint(projectId: String, tableName: String, columnName: String)                                                     = NoAction.unit
  def addUniqueConstraint(projectId: String, tableName: String, columnName: String, typeIdentifier: ScalarTypeIdentifier, isList: Boolean) = NoAction.unit
}
