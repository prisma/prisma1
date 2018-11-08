package com.prisma.deploy.connector.jdbc.database

import com.prisma.connector.shared.jdbc.SlickDatabase
import com.prisma.deploy.connector.jdbc.JdbcBase
import com.prisma.shared.models.TypeIdentifier.ScalarTypeIdentifier
import com.prisma.shared.models.{Model, Project, TypeIdentifier}
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
  def truncateProjectTables(project: Project): DBIOAction[Any, NoStream, Effect.All]
  def deleteProjectDatabase(projectId: String): DBIOAction[Any, NoStream, Effect.All]
  def renameTable(projectId: String, currentName: String, newName: String): DBIOAction[Any, NoStream, Effect.All]

  def createModelTable(projectId: String, model: Model): DBIOAction[Any, NoStream, Effect.All]
  def createScalarListTable(projectId: String, model: Model, fieldName: String, typeIdentifier: ScalarTypeIdentifier): DBIOAction[Any, NoStream, Effect.All]
  def createRelationTable(projectId: String, relationTableName: String, modelA: Model, modelB: Model): DBIOAction[Any, NoStream, Effect.All]
  def createRelationColumn(projectId: String, model: Model, references: Model, column: String): DBIOAction[Any, NoStream, Effect.All]
  def createColumn(
      projectId: String,
      tableName: String,
      columnName: String,
      isRequired: Boolean,
      isUnique: Boolean,
      isList: Boolean,
      typeIdentifier: TypeIdentifier.ScalarTypeIdentifier
  ): DBIOAction[Any, NoStream, Effect.All]

  def updateScalarListType(projectId: String, modelName: String, fieldName: String, typeIdentifier: ScalarTypeIdentifier): DBIOAction[Any, NoStream, Effect.All]
  def updateColumn(
      projectId: String,
      tableName: String,
      oldColumnName: String,
      newColumnName: String,
      newIsRequired: Boolean,
      newIsList: Boolean,
      newTypeIdentifier: ScalarTypeIdentifier
  ): DBIOAction[Any, NoStream, Effect.All]

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

  // Important: This is only using the _UNIQUE suffix for legacy and cross-compatibility reasons, however, postgres for example
  // truncates the index name, causing the index name to be "<...>._" rather than "<...>._UNIQUE"
  // TODO bug or feature?
  def addUniqueConstraint(projectId: String, tableName: String, columnName: String) = {
    val query = sql.createUniqueIndex(name(s"$projectId.$tableName.$columnName._UNIQUE")).on(name(projectId, tableName), name(columnName))
    changeDatabaseQueryToDBIO(query)()
  }

  def removeUniqueConstraint(projectId: String, tableName: String, columnName: String) = {
    val query = sql.dropIndex(name(projectId, s"$projectId.$tableName.$columnName._UNIQUE")).on(table(name(projectId, tableName)))
    changeDatabaseQueryToDBIO(query)()
  }
}
