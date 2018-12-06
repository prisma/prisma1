package com.prisma.deploy.connector.jdbc.database

import com.prisma.connector.shared.jdbc.SlickDatabase
import com.prisma.deploy.connector.jdbc.JdbcBase
import com.prisma.shared.models.TypeIdentifier.ScalarTypeIdentifier
import com.prisma.shared.models.{Model, Project, Relation, TypeIdentifier}
import org.jooq.impl.DSL._
import org.jooq.impl.SQLDataType
import slick.dbio.{DBIO, DBIOAction, Effect, NoStream}

import scala.concurrent.ExecutionContext

trait JdbcDeployDatabaseMutationBuilder extends JdbcBase {
  implicit val ec: ExecutionContext

  val slickDatabase: SlickDatabase
  val typeMapper: TypeMapper

  /*
   * Connector-specific functions
   */
  def truncateProjectTables(project: Project): DBIO[Any]
  def deleteProjectDatabase(projectId: String): DBIO[Any]
  def renameTable(projectId: String, currentName: String, newName: String): DBIO[Any]
  def addUniqueConstraint(projectId: String, tableName: String, columnName: String, typeIdentifier: ScalarTypeIdentifier): DBIO[Any]
  def removeUniqueConstraint(projectId: String, tableName: String, columnName: String): DBIO[Any]

  def createModelTable(projectId: String, model: Model): DBIO[Any]
  def createScalarListTable(projectId: String, model: Model, fieldName: String, typeIdentifier: ScalarTypeIdentifier): DBIO[Any]
  def createRelationTable(projectId: String, relation: Relation): DBIO[Any]
  def createRelationColumn(projectId: String, model: Model, references: Model, column: String): DBIO[Any]
  def createColumn(
      projectId: String,
      tableName: String,
      columnName: String,
      isRequired: Boolean,
      isUnique: Boolean,
      isList: Boolean,
      typeIdentifier: TypeIdentifier.ScalarTypeIdentifier
  ): DBIO[Any]

  def updateScalarListType(projectId: String, modelName: String, fieldName: String, typeIdentifier: ScalarTypeIdentifier): DBIO[Any]
  def updateColumn(
      projectId: String,
      tableName: String,
      oldColumnName: String,
      newColumnName: String,
      newIsRequired: Boolean,
      newIsList: Boolean,
      newTypeIdentifier: ScalarTypeIdentifier
  ): DBIO[Any]

  /*
   * Connector-agnostic functions
   */
  def createClientDatabaseForProject(projectId: String) = {
    val schema = changeDatabaseQueryToDBIO(sql.createSchema(projectId))()
    val table = changeDatabaseQueryToDBIO(
      sql
        .createTable(name(projectId, "_RelayId"))
        .column("id", SQLDataType.VARCHAR(36).nullable(false))
        .column("stableModelIdentifier", SQLDataType.VARCHAR(25).nullable(false))
        .constraint(constraint("pk_RelayId").primaryKey(name(projectId, "_RelayId", "id"))))()

    DBIO.seq(schema, table)
  }

  def dropTable(projectId: String, tableName: String) = {
    val query = sql.dropTable(name(projectId, tableName))
    changeDatabaseQueryToDBIO(query)()
  }

  def dropScalarListTable(projectId: String, modelName: String, fieldName: String) = {
    val query = sql.dropTable(name(projectId, s"${modelName}_$fieldName"))
    changeDatabaseQueryToDBIO(query)()
  }

  def renameScalarListTable(projectId: String, modelName: String, fieldName: String, newModelName: String, newFieldName: String) = {
    val query = sql.alterTable(name(projectId, s"${modelName}_$fieldName")).renameTo(name(projectId, s"${newModelName}_$newFieldName"))
    changeDatabaseQueryToDBIO(query)()
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
