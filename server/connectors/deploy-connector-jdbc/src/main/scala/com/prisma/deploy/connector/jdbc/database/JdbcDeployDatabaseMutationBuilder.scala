package com.prisma.deploy.connector.jdbc.database

import com.prisma.connector.shared.jdbc.SlickDatabase
import com.prisma.deploy.connector.{DatabaseInspector, DatabaseSchema}
import com.prisma.deploy.connector.jdbc.{DatabaseInspectorImpl, JdbcBase}
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
  def truncateProjectTables(project: Project): DBIO[_]
  def deleteProjectDatabase(projectId: String): DBIO[_]
  def renameTable(project: Project, currentName: String, newName: String): DBIO[_]
  def addUniqueConstraint(project: Project, tableName: String, columnName: String, typeIdentifier: ScalarTypeIdentifier): DBIO[_]
  def removeIndex(project: Project, tableName: String, indexName: String): DBIO[_]

  def createModelTable(project: Project, model: Model): DBIO[_]
  def createScalarListTable(project: Project, model: Model, fieldName: String, typeIdentifier: ScalarTypeIdentifier): DBIO[_]
  def createRelationTable(project: Project, relation: Relation): DBIO[_]
  def createRelationColumn(project: Project, model: Model, references: Model, column: String): DBIO[_]
  def createColumn(
      project: Project,
      tableName: String,
      columnName: String,
      isRequired: Boolean,
      isUnique: Boolean,
      isList: Boolean,
      typeIdentifier: TypeIdentifier.ScalarTypeIdentifier
  ): DBIO[_]

  def updateScalarListType(project: Project, modelName: String, fieldName: String, typeIdentifier: ScalarTypeIdentifier): DBIO[_]
  def updateColumn(
      project: Project,
      tableName: String,
      oldColumnName: String,
      newColumnName: String,
      newIsRequired: Boolean,
      newIsList: Boolean,
      newTypeIdentifier: ScalarTypeIdentifier
  ): DBIO[_]

  def updateRelationTable(project: Project, previousRelation: Relation, nextRelation: Relation): DBIO[_]

  def deleteRelationColumn(project: Project, model: Model, references: Model, column: String): DBIO[_]

  /*
   * Connector-agnostic functions
   */
  def createDatabaseForProject(id: String) = {
    val schema = changeDatabaseQueryToDBIO(sql.createSchema(id))().asTry.map(_ => ())
    val table = changeDatabaseQueryToDBIO(
      sql
        .createTableIfNotExists(name(id, "_RelayId"))
        .column("id", SQLDataType.VARCHAR(36).nullable(false))
        .column("stableModelIdentifier", SQLDataType.VARCHAR(25).nullable(false))
        .constraint(constraint("pk_RelayId").primaryKey(name(id, "_RelayId", "id"))))()

    DBIO.seq(schema, table)
  }

  def dropTable(project: Project, tableName: String) = {
    val query = sql.dropTable(name(project.dbName, tableName))
    changeDatabaseQueryToDBIO(query)()
  }

  def dropScalarListTable(project: Project, modelName: String, fieldName: String, dbSchema: DatabaseSchema) = {
    val tableName = s"${modelName}_$fieldName"
    dbSchema.table(tableName) match {
      case Some(_) =>
        val query = sql.dropTable(name(project.dbName, s"${modelName}_$fieldName"))
        changeDatabaseQueryToDBIO(query)()
      case None =>
        DBIO.successful(())
    }
  }

  def renameScalarListTable(project: Project, modelName: String, fieldName: String, newModelName: String, newFieldName: String) = {
    val query = sql.alterTable(name(project.dbName, s"${modelName}_$fieldName")).renameTo(name(project.id, s"${newModelName}_$newFieldName"))
    changeDatabaseQueryToDBIO(query)()
  }

//  def renameTable(project: Project, currentName: String, newName: String) = {
//    val query = sql.alterTable(table(name(projectId, currentName))).renameTo(name(projectId, newName))
//    changeDatabaseQueryToDBIO(query)()
//  }

  def deleteColumn(project: Project, tableName: String, columnName: String) = {
    val query = sql.alterTable(name(project.dbName, tableName)).dropColumn(name(columnName))
    changeDatabaseQueryToDBIO(query)()
  }
}
