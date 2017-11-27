package cool.graph.shared.database.mutations

import cool.graph.shared.database.{SqlDDL, SqlDDLMutaction}
import cool.graph.shared.models.{Field, Model}
import slick.jdbc.MySQLProfile.api._

import scala.util.Success

case class UpdateColumn(projectId: String, model: Model, oldField: Field, newField: Field) extends SqlDDLMutaction {

  override def execute = {

    // when type changes to/from String we need to change the subpart
    // when fieldName changes we need to update index name
    // recreating an index is expensive, so we might need to make this smarter in the future
    updateFromBeforeStateToAfterState(before = oldField, after = newField)
  }

  override def rollback = updateFromBeforeStateToAfterState(before = newField, after = oldField)

//  override def handleErrors =
//    Some({
//      // https://dev.mysql.com/doc/refman/5.5/en/error-messages-server.html#error_er_dup_entry
//      case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1062 =>
//        ExistingDuplicateDataPreventsUniqueIndex(newField.name)
//    })

  def updateFromBeforeStateToAfterState(before: Field, after: Field) = {

    val hasIndex     = before.isUnique
    val indexIsDirty = before.isRequired != after.isRequired || before.name != after.name || before.typeIdentifier != after.typeIdentifier

    val updateColumnMutation = SqlDDL.updateColumn(
      projectId = projectId,
      tableName = model.name,
      oldColumnName = before.name,
      newColumnName = after.name,
      newIsRequired = after.isRequired,
      newIsUnique = after.isUnique,
      newIsList = after.isList,
      newTypeIdentifier = after.typeIdentifier
    )

    val removeUniqueConstraint =
      SqlDDL.removeUniqueConstraint(projectId = projectId, tableName = model.name, columnName = before.name)

    val addUniqueConstraint = SqlDDL.addUniqueConstraint(projectId = projectId,
                                                         tableName = model.name,
                                                         columnName = after.name,
                                                         typeIdentifier = after.typeIdentifier,
                                                         isList = after.isList)

    val updateColumn = updateColumnMutation

    val updateColumnActions = (hasIndex, indexIsDirty, after.isUnique) match {
      case (true, true, true)  => List(removeUniqueConstraint, updateColumn, addUniqueConstraint)
      case (true, _, false)    => List(removeUniqueConstraint, updateColumn)
      case (true, false, true) => List(updateColumn)
      case (false, _, false)   => List(updateColumn)
      case (false, _, true)    => List(updateColumn, addUniqueConstraint)
    }

    Success(DBIO.seq(updateColumnActions: _*))
  }
}
