package com.prisma.deploy.connector.jdbc.database

import com.prisma.connector.shared.jdbc.SlickDatabase
import com.prisma.deploy.connector.DatabaseSchema
import com.prisma.deploy.connector.jdbc.JdbcBase
import com.prisma.shared.models.TypeIdentifier.ScalarTypeIdentifier
import com.prisma.shared.models.{Model, Project, Relation, TypeIdentifier}
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
  def renameTable(projectId: String, currentName: String, newName: String): DBIO[_]
  def addUniqueConstraint(projectId: String, tableName: String, columnName: String, typeIdentifier: ScalarTypeIdentifier): DBIO[_]
  def removeIndex(projectId: String, tableName: String, indexName: String): DBIO[_]

  def createModelTable(projectId: String, model: Model): DBIO[_]
  def createScalarListTable(projectId: String, model: Model, fieldName: String, typeIdentifier: ScalarTypeIdentifier): DBIO[_]
  def createRelationTable(projectId: String, relation: Relation): DBIO[_]
  def createRelationColumn(projectId: String, model: Model, references: Model, column: String): DBIO[_]
  def createColumn(
      projectId: String,
      tableName: String,
      columnName: String,
      isRequired: Boolean,
      isUnique: Boolean,
      isList: Boolean,
      typeIdentifier: TypeIdentifier.ScalarTypeIdentifier
  ): DBIO[_]

  def updateScalarListType(projectId: String, modelName: String, fieldName: String, typeIdentifier: ScalarTypeIdentifier): DBIO[_]
  def updateColumn(
      projectId: String,
      tableName: String,
      oldColumnName: String,
      newColumnName: String,
      newIsRequired: Boolean,
      newIsList: Boolean,
      newTypeIdentifier: ScalarTypeIdentifier
  ): DBIO[_]

  def updateRelationTable(projectId: String, previousRelation: Relation, nextRelation: Relation): DBIO[_]

  def deleteRelationColumn(projectId: String, model: Model, references: Model, column: String): DBIO[_]

  /*
   * Connector-agnostic functions
   */
  def createClientDatabaseForProject(projectId: String) = {
    val schema = createSchema(projectId)

    val table = changeDatabaseQueryToDBIO(
      sql
        .createTable(name(projectId, "_RelayId"))
        .column("id", SQLDataType.VARCHAR(36).nullable(false))
        .column("stableModelIdentifier", SQLDataType.VARCHAR(25).nullable(false))
        .constraint(constraint("pk_RelayId").primaryKey(name(projectId, "_RelayId", "id"))))()

    DBIO.seq(schema, table).withPinnedSession
  }

  def dropTable(projectId: String, tableName: String) = {
    val query = sql.dropTable(name(projectId, tableName))
    changeDatabaseQueryToDBIO(query)()
  }

  def dropScalarListTable(projectId: String, modelName: String, fieldName: String, dbSchema: DatabaseSchema) = {
    val tableName = s"${modelName}_$fieldName"
    dbSchema.table(tableName) match {
      case Some(_) =>
        val query = sql.dropTable(name(projectId, s"${modelName}_$fieldName"))
        changeDatabaseQueryToDBIO(query)()
      case None =>
        DBIO.successful(())
    }
  }

  def renameScalarListTable(projectId: String, modelName: String, fieldName: String, newModelName: String, newFieldName: String) = {
    renameTable(projectId, s"${modelName}_$fieldName", s"${newModelName}_$newFieldName")
  }

//  def renameTable(projectId: String, currentName: String, newName: String) = {
//    val query = sql.alterTable(table(name(projectId, currentName))).renameTo(name(projectId, newName))
//    changeDatabaseQueryToDBIO(query)()
//  }

  def deleteColumn(projectId: String, tableName: String, columnName: String) = {
    val query = sql.alterTable(name(projectId, tableName)).dropColumn(name(columnName))
    changeDatabaseQueryToDBIO(query)()
  }
}
