package com.prisma.deploy.connector.mongo.database

import com.prisma.deploy.connector.mongo.impl.DeployMongoAction
import com.prisma.shared.models.Project
import com.prisma.shared.models.TypeIdentifier.ScalarTypeIdentifier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object NoAction {
  val unit = {
    //    println("Mongo not Implemented")
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

  def dropTable(projectId: String, tableName: String)                                                                                      = NoAction.unit
  def createTable(projectId: String, name: String)                                                                                         = NoAction.unit
  def renameTable(projectId: String, name: String, newName: String)                                                                        = NoAction.unit
  def deleteColumn(projectId: String, tableName: String, columnName: String)                                                               = NoAction.unit
  def addUniqueConstraint(projectId: String, tableName: String, columnName: String, typeIdentifier: ScalarTypeIdentifier, isList: Boolean) = NoAction.unit
  def removeUniqueConstraint(projectId: String, tableName: String, columnName: String)                                                     = NoAction.unit
  def createRelationTable(projectId: String, tableName: String, aTableName: String, bTableName: String)                                    = NoAction.unit

  def createColumn(
      projectId: String,
      tableName: String,
      columnName: String,
      isRequired: Boolean,
      isUnique: Boolean,
      isList: Boolean,
      typeIdentifier: ScalarTypeIdentifier
  ) = NoAction.unit

  def updateColumn(
      projectId: String,
      tableName: String,
      oldColumnName: String,
      newColumnName: String,
      newIsRequired: Boolean,
      newIsList: Boolean,
      newTypeIdentifier: ScalarTypeIdentifier
  ) = NoAction.unit

}
