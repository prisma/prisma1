package com.prisma.deploy.connector.mongo.impl.mutactions

import com.prisma.deploy.connector.CreateColumn
import com.prisma.deploy.connector.mongo.database.{MongoDeployDatabaseMutationBuilder, NoAction}

object CreateColumnInterpreter extends MongoMutactionInterpreter[CreateColumn] {
  override def execute(mutaction: CreateColumn) = {
    if (mutaction.field.isUnique) {
      MongoDeployDatabaseMutationBuilder.createField(
        collectionName = mutaction.model.dbName,
        fieldName = mutaction.field.name
      )
    } else {
      NoAction.unit
    }
  }

  override def rollback(mutaction: CreateColumn) = {
    if (mutaction.field.isUnique) {

      MongoDeployDatabaseMutationBuilder.deleteField(
        projectId = mutaction.projectId,
        collectionName = mutaction.model.dbName,
        fieldName = mutaction.field.dbName
      )
    } else {

      NoAction.unit
    }
  }
}

//object DeleteColumnInterpreter extends MongoMutactionInterpreter[DeleteColumn] {
//  override def execute(mutaction: DeleteColumn) = {
//    MongoDeployDatabaseMutationBuilder.deleteColumn(
//      projectId = mutaction.projectId,
//      tableName = mutaction.model.name,
//      columnName = mutaction.field.name
//    )
//  }
//
//  override def rollback(mutaction: DeleteColumn) = {
//    MongoDeployDatabaseMutationBuilder.createColumn(
//      projectId = mutaction.projectId,
//      tableName = mutaction.model.name,
//      columnName = mutaction.field.name,
//      isRequired = mutaction.field.isRequired,
//      isUnique = mutaction.field.isUnique,
//      isList = mutaction.field.isList,
//      typeIdentifier = mutaction.field.typeIdentifier
//    )
//  }
//}
//
//object UpdateColumnInterpreter extends MongoMutactionInterpreter[UpdateColumn] {
//  override def execute(mutaction: UpdateColumn) = {
//    if (shouldUpdateClientDbColumn(mutaction)) {
//      // when type changes to/from String we need to change the subpart
//      // when fieldName changes we need to update index name
//      // recreating an index is expensive, so we might need to make this smarter in the future
//      updateFromBeforeStateToAfterState(mutaction)
//    } else {
//      DBIO.successful(())
//    }
//  }
//
//  override def rollback(mutaction: UpdateColumn) = {
//    val oppositeMutaction = mutaction.copy(oldField = mutaction.newField, newField = mutaction.oldField)
//    execute(oppositeMutaction)
//  }
//
//  def shouldUpdateClientDbColumn(mutaction: UpdateColumn): Boolean = {
//    val oldField = mutaction.oldField
//    val newField = mutaction.newField
//    if (oldField.isScalar) {
//      oldField.isRequired != newField.isRequired ||
//      oldField.name != newField.name ||
//      oldField.typeIdentifier != newField.typeIdentifier ||
//      oldField.isList != newField.isList ||
//      oldField.isUnique != newField.isUnique
//    } else {
//      false
//    }
//  }
//
//  private def updateFromBeforeStateToAfterState(mutaction: UpdateColumn): DBIOAction[Any, NoStream, Effect.All] = {
//    val before       = mutaction.oldField
//    val after        = mutaction.newField
//    val hasIndex     = before.isUnique
//    val indexIsDirty = before.isRequired != after.isRequired || before.name != after.name || before.typeIdentifier != after.typeIdentifier
//
//    val updateColumn = MongoDeployDatabaseMutationBuilder.updateColumn(
//      projectId = mutaction.projectId,
//      tableName = mutaction.model.name,
//      oldColumnName = before.name,
//      newColumnName = after.name,
//      newIsRequired = after.isRequired,
//      newIsList = after.isList,
//      newTypeIdentifier = after.typeIdentifier
//    )
//
//    val removeUniqueConstraint = MongoDeployDatabaseMutationBuilder.removeUniqueConstraint(
//      projectId = mutaction.projectId,
//      tableName = mutaction.model.name,
//      columnName = before.name
//    )
//
//    val addUniqueConstraint = MongoDeployDatabaseMutationBuilder.addUniqueConstraint(
//      projectId = mutaction.projectId,
//      tableName = mutaction.model.name,
//      columnName = after.name,
//      typeIdentifier = after.typeIdentifier,
//      isList = after.isList
//    )
//
//    val updateColumnActions = (hasIndex, indexIsDirty, after.isUnique) match {
//      case (true, true, true)  => List(removeUniqueConstraint, updateColumn, addUniqueConstraint)
//      case (true, _, false)    => List(removeUniqueConstraint, updateColumn)
//      case (true, false, true) => List(updateColumn)
//      case (false, _, false)   => List(updateColumn)
//      case (false, _, true)    => List(updateColumn, addUniqueConstraint)
//    }
//
//    DBIO.seq(updateColumnActions: _*)
//  }
//}
