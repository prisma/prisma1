package com.prisma.deploy.connector.jdbc.database

import com.prisma.connector.shared.jdbc.SlickDatabase
import com.prisma.deploy.connector.DatabaseSchema
import com.prisma.deploy.connector.jdbc.JdbcBase
import com.prisma.shared.models.TypeIdentifier.{ScalarTypeIdentifier, TypeIdentifier}
import com.prisma.shared.models._
import org.jooq.impl.DSL._
import org.jooq.impl.SQLDataType
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext

trait JdbcDeployDatabaseMutationBuilder extends JdbcBase {
  implicit val ec: ExecutionContext

  val slickDatabase: SlickDatabase
  val typeMapper: TypeMapper

  /*
   * Connector-specific functions
   */
  def createSchema(projectId: String): DBIO[_]
  def truncateProjectTables(project: Project): DBIO[_]
  def deleteProjectDatabase(projectId: String): DBIO[_]
  //UpdateModelTable  -> for PostGres and MySQL this is just rename
  //                  -> for SQLite this does everything???

  def addUniqueConstraint(project: Project, field: Field): DBIO[_]
  def removeIndex(project: Project, tableName: String, indexName: String): DBIO[_]
  def createModelTable(project: Project, model: Model): DBIO[_]
  def createScalarListTable(project: Project, model: Model, fieldName: String, typeIdentifier: ScalarTypeIdentifier): DBIO[_]
  def createRelationTable(project: Project, relation: Relation): DBIO[_]
  def createRelationColumn(project: Project, model: Model, references: Model, column: String): DBIO[_]
  def createColumn(project: Project, field: ScalarField): DBIO[_]
  def updateColumn(project: Project, field: ScalarField, oldTableName: String, oldColumnName: String, oldTypeIdentifier: ScalarTypeIdentifier): DBIO[_]
  def deleteRelationColumn(project: Project, model: Model, references: Model, column: String): DBIO[_]
  def deleteColumn(project: Project, tableName: String, columnName: String, model: Option[Model] = None): DBIO[_]
  def renameColumn(project: Project, tableName: String, oldColumnName: String, newColumnName: String, typeIdentifier: TypeIdentifier): DBIO[_]

  /*
   * Connector-agnostic functions
   */

  def updateRelationTable(project: Project, previousRelation: Relation, nextRelation: Relation) = {
    val modelAIdType         = nextRelation.modelA.idField_!.typeIdentifier
    val modelBIdType         = nextRelation.modelB.idField_!.typeIdentifier
    val addOrRemoveId        = addOrRemoveIdColumn(project, previousRelation, nextRelation)
    val renameModelAColumn   = renameColumn(project, previousRelation.relationTableName, previousRelation.modelAColumn, nextRelation.modelAColumn, modelAIdType)
    val renameModelBColumn   = renameColumn(project, previousRelation.relationTableName, previousRelation.modelBColumn, nextRelation.modelBColumn, modelBIdType)
    val renameTableStatement = renameTable(project, previousRelation.relationTableName, nextRelation.relationTableName)

    val all = Vector(addOrRemoveId, renameModelAColumn, renameModelBColumn, renameTableStatement)
    DBIO.sequence(all)
  }

  def createDatabaseForProject(id: String) = {
    DBIO.seq(createSchema(id)).withPinnedSession
  }

  def dropTable(project: Project, tableName: String) = {
    val query = sql.dropTable(name(project.dbName, tableName))
    changeDatabaseQueryToDBIO(query)()
  }

  def dropScalarListTable(project: Project, modelName: String, fieldName: String, dbSchema: DatabaseSchema) = {
    val tableName = s"${modelName}_$fieldName"
    dbSchema.table(tableName) match {
      case Some(_) => dropTable(project, tableName)
      case None    => DBIO.successful(())
    }
  }

  def renameScalarListTable(project: Project, modelName: String, fieldName: String, newModelName: String, newFieldName: String) = {
    renameTable(project, s"${modelName}_$fieldName", s"${newModelName}_$newFieldName")
  }

  def renameTable(project: Project, currentName: String, newName: String) = {
    if (currentName != newName) {
      val query = sql.alterTable(table(name(project.dbName, currentName))).renameTo(name(project.dbName, newName))
      changeDatabaseQueryToDBIO(query)()

    } else {
      DBIO.successful(())
    }
  }

  def addOrRemoveIdColumn(project: Project, previousRelation: Relation, nextRelation: Relation): DBIO[_] = {
    (previousRelation.idColumn, nextRelation.idColumn) match {
      case (Some(idColumn), None) => deleteColumn(project, previousRelation.relationTableName, idColumn)
      case (None, Some(idColumn)) =>
        // We are not adding a primary key column because we don't need it actually.
        // Because of the default this column will also work if the insert does not contain the id column.
        val query = sql.alterTable(name(project.dbName, previousRelation.relationTableName)).addColumn(field(s""""#$idColumn" varchar(40) default null"""))

        changeDatabaseQueryToDBIO(query)()
      case _ => DBIO.successful(())
    }
  }
}
