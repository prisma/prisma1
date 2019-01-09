package com.prisma.deploy.connector.jdbc.database

import com.prisma.deploy.connector._
import slick.jdbc.PostgresProfile.api._

case class CreateColumnInterpreter(builder: JdbcDeployDatabaseMutationBuilder) extends SqlMutactionInterpreter[CreateColumn] {
  override def execute(mutaction: CreateColumn, schemaBeforeMigration: DatabaseSchema) = {
    schemaBeforeMigration.table_!(mutaction.model.dbName).column(mutaction.field.dbName) match {
      case None =>
        builder.createColumn(
          projectId = mutaction.projectId,
          tableName = mutaction.model.dbName,
          columnName = mutaction.field.dbName,
          isRequired = mutaction.field.isRequired,
          isUnique = mutaction.field.isUnique,
          isList = mutaction.field.isList,
          typeIdentifier = mutaction.field.typeIdentifier
        )
      case Some(_) =>
        DBIO.successful(())
    }
  }

  override def rollback(mutaction: CreateColumn, schemaBeforeMigration: DatabaseSchema) = {
    schemaBeforeMigration.table_!(mutaction.model.dbName).column(mutaction.field.dbName) match {
      case None =>
        builder.deleteColumn(
          projectId = mutaction.projectId,
          tableName = mutaction.model.dbName,
          columnName = mutaction.field.dbName
        )
      case Some(_) =>
        DBIO.successful(())
    }
  }
}

case class DeleteColumnInterpreter(builder: JdbcDeployDatabaseMutationBuilder) extends SqlMutactionInterpreter[DeleteColumn] {
  override def execute(mutaction: DeleteColumn, schemaBeforeMigration: DatabaseSchema) = {
    builder.deleteColumn(
      projectId = mutaction.projectId,
      tableName = mutaction.model.dbName,
      columnName = mutaction.field.dbName
    )
  }

  override def rollback(mutaction: DeleteColumn, schemaBeforeMigration: DatabaseSchema) = {
    builder.createColumn(
      projectId = mutaction.projectId,
      tableName = mutaction.model.dbName,
      columnName = mutaction.field.dbName,
      isRequired = mutaction.field.isRequired,
      isUnique = mutaction.field.isUnique,
      isList = mutaction.field.isList,
      typeIdentifier = mutaction.field.typeIdentifier
    )
  }
}

case class UpdateColumnInterpreter(builder: JdbcDeployDatabaseMutationBuilder) extends SqlMutactionInterpreter[UpdateColumn] {
  override def execute(mutaction: UpdateColumn, schemaBeforeMigration: DatabaseSchema) = {
    if (shouldUpdateClientDbColumn(mutaction)) {
      // when type changes to/from String we need to change the subpart
      // when fieldName changes we need to update index name
      // recreating an index is expensive, so we might need to make this smarter in the future
      updateFromBeforeStateToAfterState(mutaction)
    } else {
      DBIO.successful(())
    }
  }

  override def rollback(mutaction: UpdateColumn, schemaBeforeMigration: DatabaseSchema) = {
    val oppositeMutaction = mutaction.copy(oldField = mutaction.newField, newField = mutaction.oldField)
    execute(oppositeMutaction, schemaBeforeMigration)
  }

  def shouldUpdateClientDbColumn(mutaction: UpdateColumn): Boolean = {
    val oldField = mutaction.oldField
    val newField = mutaction.newField
    if (oldField.isScalar) {
      oldField.isRequired != newField.isRequired ||
      oldField.dbName != newField.dbName ||
      oldField.typeIdentifier != newField.typeIdentifier ||
      oldField.isList != newField.isList ||
      oldField.isUnique != newField.isUnique
    } else {
      false
    }
  }

  private def updateFromBeforeStateToAfterState(mutaction: UpdateColumn): DBIOAction[Any, NoStream, Effect.All] = {
    val before               = mutaction.oldField
    val after                = mutaction.newField
    val indexMustBeRecreated = before.isRequired != after.isRequired || before.dbName != after.dbName || before.typeIdentifier != after.typeIdentifier

    val updateColumn = builder.updateColumn(
      projectId = mutaction.projectId,
      tableName = mutaction.model.dbName,
      oldColumnName = before.dbName,
      newColumnName = after.dbName,
      newIsRequired = after.isRequired,
      newIsList = after.isList,
      newTypeIdentifier = after.typeIdentifier
    )

    val removeUniqueConstraint = builder.removeUniqueConstraint(
      projectId = mutaction.projectId,
      tableName = mutaction.model.dbName,
      columnName = before.dbName
    )

    val addUniqueConstraint = builder.addUniqueConstraint(
      projectId = mutaction.projectId,
      tableName = mutaction.model.dbName,
      columnName = after.dbName,
      typeIdentifier = after.typeIdentifier
    )

    val updateColumnActions = (before.isUnique, indexMustBeRecreated, after.isUnique) match {
      case (true, true, true)  => List(removeUniqueConstraint, updateColumn, addUniqueConstraint)
      case (true, _, false)    => List(removeUniqueConstraint, updateColumn)
      case (true, false, true) => List(updateColumn)
      case (false, _, false)   => List(updateColumn)
      case (false, _, true)    => List(updateColumn, addUniqueConstraint)
    }

    DBIO.seq(updateColumnActions: _*)
  }
}
