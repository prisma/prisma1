package com.prisma.deploy.connector.jdbc.database

import com.prisma.deploy.connector._
import slick.jdbc.PostgresProfile.api._
import com.prisma.utils.boolean.BooleanUtils._

case class CreateColumnInterpreter(builder: JdbcDeployDatabaseMutationBuilder) extends SqlMutactionInterpreter[CreateColumn] {
  // todo: that does not consider unique constraints yet
  override def execute(mutaction: CreateColumn, schemaBeforeMigration: DatabaseSchema) = {
//    schemaBeforeMigration.table(mutaction.model.dbName).flatMap(_.column(mutaction.field.dbName)) match {
//      case None =>
//        builder.createColumn(
//          mutaction.project,
//          tableName = mutaction.model.dbName,
//          columnName = mutaction.field.dbName,
//          isRequired = mutaction.field.isRequired,
//          isUnique = mutaction.field.isUnique,
//          isList = mutaction.field.isList,
//          typeIdentifier = mutaction.field.typeIdentifier
//        )
//
//      // This can only happen if an inline relation field has been converted into a scalar field. The foreign key indicates it is a relation column.
//      // Our step order ensures that this relation column already has been deleted. So we just create the scalar column.
//      case Some(c) if c.foreignKey.isDefined =>
//        builder.createColumn(
//          mutaction.project,
//          tableName = mutaction.model.dbName,
//          columnName = mutaction.field.dbName,
//          isRequired = mutaction.field.isRequired,
//          isUnique = mutaction.field.isUnique,
//          isList = mutaction.field.isList,
//          typeIdentifier = mutaction.field.typeIdentifier
//        )
//
//      case Some(c) =>
//        val updateColumn = mustUpdateColumn(c, mutaction).toOption {
//          builder.updateColumn(
//            mutaction.project,
//            model = mutaction.model,
//            oldColumnName = mutaction.field.dbName,
//            newColumnName = mutaction.field.dbName,
//            newIsRequired = mutaction.field.isRequired,
//            newIsList = mutaction.field.isList,
//            newTypeIdentifier = mutaction.field.typeIdentifier
//          )
//        }
//        val addUniqueConstraint = mustAddUniqueConstraint(c, mutaction).toOption {
//          builder.addUniqueConstraint(mutaction.project, mutaction.model.dbName, mutaction.field.dbName, mutaction.field.typeIdentifier)
//        }
//        val removeUniqueConstraint = mustRemoveUniqueConstraint(c, mutaction).toOption {
//          val index = c.table.indexByColumns_!(c.name)
//          builder.removeIndex(mutaction.project, mutaction.model.dbName, indexName = index.name)
//        }
//        val allActions = removeUniqueConstraint ++ updateColumn ++ addUniqueConstraint
//
//        DBIO.seq(allActions.toVector: _*)
//    }
    builder.createColumn(
      mutaction.project,
      tableName = mutaction.model.dbName,
      columnName = mutaction.field.dbName,
      isRequired = mutaction.field.isRequired,
      isUnique = mutaction.field.isUnique,
      isList = mutaction.field.isList,
      typeIdentifier = mutaction.field.typeIdentifier
    )
  }

  private def mustUpdateColumn(column: Column, mutaction: CreateColumn) = {
    column.typeIdentifier != mutaction.field.typeIdentifier ||
    column.isRequired == mutaction.field.isRequired
  }

  private def mustAddUniqueConstraint(column: Column, mutaction: CreateColumn) = {
    val index = column.table.indexByColumns(column.name)
    index.forall(_.unique == false) && mutaction.field.isUnique
  }

  private def mustRemoveUniqueConstraint(column: Column, mutaction: CreateColumn) = {
    val index = column.table.indexByColumns(column.name)
    index.exists(_.unique == true) && !mutaction.field.isUnique
  }

  override def rollback(mutaction: CreateColumn, schemaBeforeMigration: DatabaseSchema) = {
//    schemaBeforeMigration.table(mutaction.model.dbName).flatMap(_.column(mutaction.field.dbName)) match {
//      case None =>
//        builder.deleteColumn(
//          mutaction.project,
//          tableName = mutaction.model.dbName,
//          columnName = mutaction.field.dbName
//        )
//      case Some(_) =>
//        DBIO.successful(())
//    }
    builder.deleteColumn(
      mutaction.project,
      tableName = mutaction.model.dbName,
      columnName = mutaction.field.dbName
    )
  }
}

case class DeleteColumnInterpreter(builder: JdbcDeployDatabaseMutationBuilder) extends SqlMutactionInterpreter[DeleteColumn] {
  override def execute(mutaction: DeleteColumn, schemaBeforeMigration: DatabaseSchema) = {
//    schemaBeforeMigration.table(mutaction.model.dbName).flatMap(_.column(mutaction.field.dbName)) match {
//      case Some(_) =>
//        builder.deleteColumn(
//          mutaction.project,
//          tableName = mutaction.model.dbName,
//          columnName = mutaction.field.dbName
//        )
//      case None =>
//        DBIO.successful(())
//    }
    builder.deleteColumn(
      mutaction.project,
      tableName = mutaction.model.dbName,
      columnName = mutaction.field.dbName
    )
  }

  override def rollback(mutaction: DeleteColumn, schemaBeforeMigration: DatabaseSchema) = {
//    schemaBeforeMigration.table(mutaction.model.dbName).flatMap(_.column(mutaction.field.dbName)) match {
//      case Some(_) =>
//        builder.createColumn(
//          mutaction.project,
//          tableName = mutaction.model.dbName,
//          columnName = mutaction.field.dbName,
//          isRequired = mutaction.field.isRequired,
//          isUnique = mutaction.field.isUnique,
//          isList = mutaction.field.isList,
//          typeIdentifier = mutaction.field.typeIdentifier
//        )
//      case None =>
//        DBIO.successful(())
//    }
    builder.createColumn(
      mutaction.project,
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
      updateFromBeforeStateToAfterState(mutaction, schemaBeforeMigration)
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

  private def updateFromBeforeStateToAfterState(mutaction: UpdateColumn, schemaBeforeMigration: DatabaseSchema): DBIO[_] = {
    val before               = mutaction.oldField
    val after                = mutaction.newField
    val indexMustBeRecreated = before.isRequired != after.isRequired || before.dbName != after.dbName || before.typeIdentifier != after.typeIdentifier

    def updateColumn = builder.updateColumn(
      mutaction.project,
      model = mutaction.model,
      oldColumnName = before.dbName,
      newColumnName = after.dbName,
      newIsRequired = after.isRequired,
      newIsList = after.isList,
      newTypeIdentifier = after.typeIdentifier
    )

//    def createColumn = builder.createColumn(
//      mutaction.project,
//      tableName = mutaction.model.dbName,
//      columnName = after.dbName,
//      isRequired = after.isRequired,
//      isUnique = after.isUnique,
//      isList = after.isList,
//      typeIdentifier = after.typeIdentifier
//    )

    def removeUniqueConstraint = builder.removeIndex(
      mutaction.project,
      tableName = mutaction.model.dbName,
      indexName = schemaBeforeMigration.table_!(mutaction.model.dbName).indexByColumns_!(before.dbName).name
    )

    def addUniqueConstraint = builder.addUniqueConstraint(
      mutaction.project,
      tableName = mutaction.model.dbName,
      columnName = after.dbName,
      typeIdentifier = after.typeIdentifier
    )

    def updateColumnActions = (before.isUnique, indexMustBeRecreated, after.isUnique) match {
      case (true, true, true)  => Vector(removeUniqueConstraint, updateColumn, addUniqueConstraint)
      case (true, _, false)    => Vector(removeUniqueConstraint, updateColumn)
      case (true, false, true) => Vector(updateColumn)
      case (false, _, false)   => Vector(updateColumn)
      case (false, _, true)    => Vector(updateColumn, addUniqueConstraint)
    }

//    schemaBeforeMigration.table(mutaction.model.dbName).flatMap(_.column(before.dbName)) match {
//      case Some(_) => DBIO.seq(updateColumnActions: _*)
//      case None    => createColumn
//    }
    DBIO.seq(updateColumnActions: _*)
  }
}
