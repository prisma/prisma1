package com.prisma.deploy.connector.mongo.database

import com.prisma.deploy.connector.mongo.impl.DeployMongoAction
import com.prisma.shared.models.Project
import org.mongodb.scala.bson.BsonNull
import org.mongodb.scala.{MongoNamespace, _}
import org.mongodb.scala.model.{Filters, IndexModel, IndexOptions, Indexes}
import org.mongodb.scala.model.Sorts.ascending

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

  //Project
  def createClientDatabaseForProject = DeployMongoAction { database =>
    database.listCollectionNames().toFuture().map(_ => ())
  }

  //Fixme This deletes all indexes
  def truncateProjectTables(project: Project) = DeployMongoAction { database =>
    database.drop().toFuture().map(_ -> Unit)
  }

  def deleteProjectDatabase = DeployMongoAction { database =>
    database.drop().toFuture().map(_ -> Unit)
  }

  //Collection
  def createCollection(collectionName: String) = DeployMongoAction { database =>
    database.createCollection(collectionName).toFuture().map(_ -> Unit)
  }

  def dropCollection(collectionName: String) = DeployMongoAction { database =>
    database.getCollection(collectionName).drop().toFuture().map(_ -> Unit)
  }

  def renameCollection(projectId: String, collectionName: String, newName: String) = DeployMongoAction { database =>
    database.getCollection(collectionName).renameCollection(MongoNamespace(projectId, newName)).toFuture().map(_ -> Unit)
  }

  //Fields
  def createField(collectionName: String, fieldName: String) = addUniqueConstraint(collectionName, fieldName)

//  def deleteField(collectionName: String) = DeployMongoAction { database =>
//    database.getCollection(collectionName).drop().toFuture().map(_ -> Unit)
//  }
//
//  def updateField(name: String, newName: String) = DeployMongoAction { database =>
//    database.getCollection(name).renameCollection(MongoNamespace(projectId, newName)).toFuture().map(_ -> Unit)
//  }

  def addUniqueConstraint(collectionName: String, fieldName: String) = DeployMongoAction { database =>
    database
      .getCollection(collectionName)
      .createIndex(ascending(fieldName), IndexOptions().unique(true).sparse(true).name(s"${collectionName}_${fieldName}_UNIQUE"))
      .toFuture()
      .map(_ => Unit)
  }

  def removeUniqueConstraint(collectionName: String, fieldName: String) = NoAction.unit
}
