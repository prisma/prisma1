package com.prisma.deploy.connector.mongo.database

import com.prisma.deploy.connector.mongo.impls.mutactions.DeployMongoAction
import com.prisma.shared.models.Project
import com.prisma.shared.models.TypeIdentifier.ScalarTypeIdentifier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

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
    noAction
  }

  def dropScalarListTable(projectId: String, modelName: String, fieldName: String) = DeployMongoAction { database =>
    noAction
  }

  def createTable(projectId: String, name: String) = DeployMongoAction { database =>
    noAction
  }

  def createScalarListTable(projectId: String, modelName: String, fieldName: String, typeIdentifier: ScalarTypeIdentifier) = DeployMongoAction { database =>
    noAction
  }

  def updateScalarListType(projectId: String, modelName: String, fieldName: String, typeIdentifier: ScalarTypeIdentifier) = DeployMongoAction { database =>
    noAction
  }

  def renameScalarListTable(projectId: String, modelName: String, fieldName: String, newModelName: String, newFieldName: String) = DeployMongoAction {
    database =>
      noAction
  }

  def renameTable(projectId: String, name: String, newName: String) = DeployMongoAction { database =>
    noAction
  }

  def createColumn(
      projectId: String,
      tableName: String,
      columnName: String,
      isRequired: Boolean,
      isUnique: Boolean,
      isList: Boolean,
      typeIdentifier: ScalarTypeIdentifier
  ) = DeployMongoAction { database =>
    noAction
  }

  def deleteColumn(projectId: String, tableName: String, columnName: String) = DeployMongoAction { database =>
    noAction
  }

  def updateColumn(
      projectId: String,
      tableName: String,
      oldColumnName: String,
      newColumnName: String,
      newIsRequired: Boolean,
      newIsList: Boolean,
      newTypeIdentifier: ScalarTypeIdentifier
  ) = DeployMongoAction { database =>
    noAction
  }

  def addUniqueConstraint(projectId: String, tableName: String, columnName: String, typeIdentifier: ScalarTypeIdentifier, isList: Boolean) = DeployMongoAction {
    database =>
      noAction
  }

  def removeUniqueConstraint(projectId: String, tableName: String, columnName: String) = DeployMongoAction { database =>
    noAction
  }

  def createRelationTable(projectId: String, tableName: String, aTableName: String, bTableName: String) = DeployMongoAction { database =>
    noAction
  }

  def noAction = {
    println("Mongo not Implemented")
    Future.successful(())
  }
}
