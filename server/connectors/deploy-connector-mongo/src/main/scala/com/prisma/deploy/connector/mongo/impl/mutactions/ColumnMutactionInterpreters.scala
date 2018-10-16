package com.prisma.deploy.connector.mongo.impl.mutactions

import com.prisma.deploy.connector.{CreateColumn, DeleteColumn, UpdateColumn}
import com.prisma.deploy.connector.mongo.database.{MongoDeployDatabaseMutationBuilder, NoAction}

object CreateColumnInterpreter extends MongoMutactionInterpreter[CreateColumn] {
  override def execute(mutaction: CreateColumn) = {
    if (mutaction.field.isUnique && !mutaction.model.isEmbedded) {
      MongoDeployDatabaseMutationBuilder.createField(
        collectionName = mutaction.model.dbName,
        fieldName = mutaction.field.name
      )
    } else {
      NoAction.unit
    }
  }

  override def rollback(mutaction: CreateColumn) = {
    if (mutaction.field.isUnique && !mutaction.model.isEmbedded) {

      MongoDeployDatabaseMutationBuilder.deleteField(
        collectionName = mutaction.model.dbName,
        fieldName = mutaction.field.name
      )
    } else {
      NoAction.unit
    }
  }
}

object DeleteColumnInterpreter extends MongoMutactionInterpreter[DeleteColumn] {
  override def execute(mutaction: DeleteColumn) = {
    if (mutaction.field.isUnique && !mutaction.model.isEmbedded) {
      MongoDeployDatabaseMutationBuilder.deleteField(
        collectionName = mutaction.model.dbName,
        fieldName = mutaction.field.name
      )
    } else {
      NoAction.unit
    }
  }

  override def rollback(mutaction: DeleteColumn) = {
    if (mutaction.field.isUnique && !mutaction.model.isEmbedded) {
      MongoDeployDatabaseMutationBuilder.createField(
        collectionName = mutaction.model.dbName,
        fieldName = mutaction.field.name
      )
    } else {
      NoAction.unit
    }
  }
}

//Fixme index names cannot be changed in Mongo/ This is a problem after renames since we derive the index name from modelname and fieldname
object UpdateColumnInterpreter extends MongoMutactionInterpreter[UpdateColumn] {
  override def execute(mutaction: UpdateColumn) = {
    NoAction.unit //Fixme -> add/remove unique index
  }

  override def rollback(mutaction: UpdateColumn) = {
    NoAction.unit //Fixme -> remove/add unique index
  }
}
