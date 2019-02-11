package com.prisma.deploy.connector.jdbc.database

import com.prisma.deploy.connector._
import com.prisma.shared.models.TypeIdentifier.ScalarTypeIdentifier
import com.prisma.shared.models.{Project, ScalarField}
import com.prisma.utils.boolean.BooleanUtils._
import slick.jdbc.PostgresProfile.api._

case class CreateColumnInterpreter(builder: JdbcDeployDatabaseMutationBuilder) extends SqlMutactionInterpreter[CreateColumn] {
  override def execute(mutaction: CreateColumn, schemaBeforeMigration: DatabaseSchema) = {
    schemaBeforeMigration.table(mutaction.model.dbName).flatMap(_.column(mutaction.field.dbName)) match {
      case None =>
        CreateColumnHelper.withIndexIfNecessary(builder, mutaction.project, mutaction.field)
      // This can only happen if an inline relation field has been converted into a scalar field. The foreign key indicates it is a relation column.
      // Our step order ensures that this relation column already has been deleted. So we just create the scalar column.
      case Some(c) if c.foreignKey.isDefined =>
        CreateColumnHelper.withIndexIfNecessary(builder, mutaction.project, mutaction.field)

      case Some(c) =>
        val updateColumn = mustUpdateColumn(c, mutaction).toOption {
          builder.updateColumn(
            mutaction.project,
            mutaction.field,
            oldTableName = c.table.name,
            oldColumnName = mutaction.field.dbName,
            oldTypeIdentifier = c.typeIdentifier.asInstanceOf[ScalarTypeIdentifier]
          )
        }
        val addUniqueConstraint = mustAddUniqueConstraint(c, mutaction).toOption {
          builder.addUniqueConstraint(mutaction.project, mutaction.field)
        }
        val removeUniqueConstraint = mustRemoveUniqueConstraint(c, mutaction).toOption {
          val index = c.table.indexByColumns_!(c.name)
          builder.removeIndex(mutaction.project, mutaction.model.dbName, indexName = index.name)
        }
        val allActions = removeUniqueConstraint ++ updateColumn ++ addUniqueConstraint

        DBIO.seq(allActions.toVector: _*)
    }
  }

  private def mustUpdateColumn(column: Column, mutaction: CreateColumn) = {
    column.typeIdentifier != mutaction.field.typeIdentifier ||
    column.isRequired != mutaction.field.isRequired
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
    schemaBeforeMigration.table(mutaction.model.dbName).flatMap(_.column(mutaction.field.dbName)) match {
      case None    => builder.deleteColumn(mutaction.project, tableName = mutaction.model.dbName, columnName = mutaction.field.dbName)
      case Some(_) => DBIO.successful(())
    }
  }
}

case class DeleteColumnInterpreter(builder: JdbcDeployDatabaseMutationBuilder) extends SqlMutactionInterpreter[DeleteColumn] {
  override def execute(mutaction: DeleteColumn, schemaBeforeMigration: DatabaseSchema) = {
    schemaBeforeMigration.table(mutaction.model.dbName).flatMap(_.column(mutaction.field.dbName)) match {
      case Some(_) => builder.deleteColumn(mutaction.project, tableName = mutaction.model.dbName, columnName = mutaction.field.dbName)
      case None    => DBIO.successful(())
    }
  }

  override def rollback(mutaction: DeleteColumn, schemaBeforeMigration: DatabaseSchema) = {
    schemaBeforeMigration.table(mutaction.model.dbName).flatMap(_.column(mutaction.field.dbName)) match {
      case Some(_) => CreateColumnHelper.withIndexIfNecessary(builder, mutaction.project, mutaction.field)
      case None    => DBIO.successful(())
    }
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
    val before          = mutaction.oldField
    val after           = mutaction.newField
    val typeChanges     = before.typeIdentifier != after.typeIdentifier
    val requiredChanges = before.isRequired != after.isRequired

    def updateColumn =
      builder.updateColumn(mutaction.project,
                           after,
                           oldTableName = before.model.dbName,
                           oldColumnName = before.dbName,
                           oldTypeIdentifier = before.typeIdentifier)

    def removeUniqueConstraint = builder.removeIndex(
      mutaction.project,
      tableName = mutaction.model.dbName,
      indexName = schemaBeforeMigration.table_!(mutaction.model.dbName).indexByColumns_!(before.dbName).name
    )

    def addUniqueConstraint = builder.addUniqueConstraint(mutaction.project, after)

    def updateColumnActions = (before.isUnique, typeChanges, requiredChanges, after.isUnique) match {
      // type changes, after unique
      case (true, true, true, true)   => Vector(updateColumn, addUniqueConstraint)
      case (false, true, true, true)  => Vector(updateColumn, addUniqueConstraint)
      case (true, true, false, true)  => Vector(updateColumn, addUniqueConstraint)
      case (false, true, false, true) => Vector(updateColumn, addUniqueConstraint)
      //type changes, after not unique
      case (true, true, true, false)   => Vector(updateColumn)
      case (false, true, true, false)  => Vector(updateColumn)
      case (true, true, false, false)  => Vector(updateColumn)
      case (false, true, false, false) => Vector(updateColumn)
      // type does not change, after is unique
      case (true, false, true, true)   => Vector(removeUniqueConstraint, updateColumn, addUniqueConstraint)
      case (false, false, true, true)  => Vector(updateColumn, addUniqueConstraint)
      case (true, false, false, true)  => Vector(updateColumn)
      case (false, false, false, true) => Vector(updateColumn, addUniqueConstraint)
      // type does not change, after is not unique
      case (true, false, true, false)   => Vector(removeUniqueConstraint, updateColumn)
      case (false, false, true, false)  => Vector(updateColumn)
      case (true, false, false, false)  => Vector(removeUniqueConstraint, updateColumn)
      case (false, false, false, false) => Vector(updateColumn)
    }

    schemaBeforeMigration.table(mutaction.model.dbName).flatMap(_.column(before.dbName)) match {
      case Some(_) => DBIO.seq(updateColumnActions: _*)
      case None    => CreateColumnHelper.withIndexIfNecessary(builder, mutaction.project, mutaction.newField)
    }
  }

}
object CreateColumnHelper {
  def withIndexIfNecessary(builder: JdbcDeployDatabaseMutationBuilder, project: Project, field: ScalarField) = {
    val createColumn        = builder.createColumn(project, field)
    val addUniqueConstraint = if (field.isUnique) builder.addUniqueConstraint(project, field) else DBIO.successful(())
    DBIO.seq(createColumn, addUniqueConstraint)
  }
}
