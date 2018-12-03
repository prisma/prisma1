package com.prisma.deploy.connector.postgres.database

import com.prisma.connector.shared.jdbc.SlickDatabase
import com.prisma.deploy.connector.jdbc.database.{JdbcDeployDatabaseMutationBuilder, TypeMapper}
import com.prisma.shared.models.{Model, Project, Relation}
import com.prisma.shared.models.TypeIdentifier.ScalarTypeIdentifier
import org.jooq.impl.DSL
import slick.dbio.{DBIOAction => DatabaseAction}

import scala.concurrent.ExecutionContext

case class PostgresJdbcDeployDatabaseMutationBuilder(
    slickDatabase: SlickDatabase,
    typeMapper: TypeMapper
)(implicit val ec: ExecutionContext)
    extends JdbcDeployDatabaseMutationBuilder {

  import slickDatabase.profile.api._

  override def truncateProjectTables(project: Project): DatabaseAction[Any, NoStream, Effect.All] = {
    val listTableNames: List[String] = project.models.flatMap { model =>
      model.fields.collect { case field if field.isScalar && field.isList => s"${model.dbName}_${field.dbName}" }
    }

    val tables = Vector("_RelayId") ++ project.models.map(_.dbName) ++ project.relations.map(_.relationTableName) ++ listTableNames
    val queries = tables.map(tableName => {
      changeDatabaseQueryToDBIO(sql.truncate(DSL.name(project.id, tableName)).cascade())()
    })

    DBIO.seq(queries: _*)
  }

  def deleteProjectDatabase(projectId: String) = {
    val query = sql.dropSchemaIfExists(projectId).cascade()
    changeDatabaseQueryToDBIO(query)()
  }

  override def createModelTable(projectId: String, model: Model): DBIOAction[Any, NoStream, Effect.All] = {
    val idField    = model.idField_!
    val idFieldSQL = typeMapper.rawSQLForField(idField)

    sqlu"""
         CREATE TABLE #${qualify(projectId, model.dbName)} (
            #$idFieldSQL,
            PRIMARY KEY (#${qualify(idField.dbName)})
         )
      """
  }

  override def createScalarListTable(projectId: String,
                                     model: Model,
                                     fieldName: String,
                                     typeIdentifier: ScalarTypeIdentifier): DBIOAction[Any, NoStream, Effect.All] = {
    val sqlType = typeMapper.rawSqlTypeForScalarTypeIdentifier(isList = false, typeIdentifier)

    sqlu"""
           CREATE TABLE #${qualify(projectId, s"${model.dbName}_$fieldName")} (
              "nodeId" VARCHAR (25) NOT NULL REFERENCES #${qualify(projectId, model.dbName)} (#${qualify(model.dbNameOfIdField_!)}),
              "position" INT NOT NULL,
              "value" #$sqlType NOT NULL,
              PRIMARY KEY ("nodeId", "position")
           )
      """
  }

  override def createRelationTable(
      projectId: String,
      relation: Relation
  ): DBIOAction[Any, NoStream, Effect.All] = {
    val relationTableName = relation.relationTableName
    val modelA            = relation.modelA
    val modelB            = relation.modelB
    val modelAColumn      = relation.modelAColumn
    val modelBColumn      = relation.modelBColumn
    val aColSql           = typeMapper.rawSQLFromParts(modelAColumn, isRequired = true, isList = false, modelA.idField_!.typeIdentifier)
    val bColSql           = typeMapper.rawSQLFromParts(modelBColumn, isRequired = true, isList = false, modelB.idField_!.typeIdentifier)
    val tableCreate       = sqlu"""
                        CREATE TABLE #${qualify(projectId, relationTableName)} (
                            "id" CHAR(25) NOT NULL,
                            PRIMARY KEY ("id"),
                            #$aColSql,
                            #$bColSql,
                            FOREIGN KEY ("#$modelAColumn") REFERENCES #${qualify(projectId, modelA.dbName)} (#${qualify(modelA.dbNameOfIdField_!)}) ON DELETE CASCADE,
                            FOREIGN KEY ("#$modelBColumn") REFERENCES #${qualify(projectId, modelB.dbName)} (#${qualify(modelA.dbNameOfIdField_!)}) ON DELETE CASCADE
                        );"""

    val indexCreate =
      sqlu"""CREATE UNIQUE INDEX "#${relationTableName}_AB_unique" on #${qualify(projectId, relationTableName)} ("#$modelAColumn" ASC, "#$modelBColumn" ASC)"""
    val indexA = sqlu"""CREATE INDEX #${qualify(s"${relationTableName}_A")} on #${qualify(projectId, relationTableName)} ("#$modelAColumn" ASC)"""
    val indexB = sqlu"""CREATE INDEX #${qualify(s"${relationTableName}_B")} on #${qualify(projectId, relationTableName)} ("#$modelBColumn" ASC)"""

    DatabaseAction.seq(tableCreate, indexCreate, indexA, indexB)
  }

  override def createRelationColumn(projectId: String, model: Model, references: Model, column: String): DBIOAction[Any, NoStream, Effect.All] = {
    val colSql = typeMapper.rawSQLFromParts(column, isRequired = false, isList = model.idField_!.isList, references.idField_!.typeIdentifier)

    sqlu"""ALTER TABLE #${qualify(projectId, model.dbName)} ADD COLUMN #$colSql
           REFERENCES #${qualify(projectId, references.dbName)} (#${qualify(references.dbNameOfIdField_!)}) ON DELETE SET NULL;"""
  }

  override def createColumn(projectId: String,
                            tableName: String,
                            columnName: String,
                            isRequired: Boolean,
                            isUnique: Boolean,
                            isList: Boolean,
                            typeIdentifier: ScalarTypeIdentifier): DBIOAction[Any, NoStream, Effect.All] = {
    val fieldSQL = typeMapper.rawSQLFromParts(columnName, isRequired, isList, typeIdentifier)
    val uniqueAction = isUnique match {
      case true  => addUniqueConstraint(projectId, tableName, columnName, typeIdentifier)
      case false => DatabaseAction.successful(())
    }

    val addColumn = sqlu"""ALTER TABLE #${qualify(projectId, tableName)} ADD COLUMN #$fieldSQL"""
    DatabaseAction.seq(addColumn, uniqueAction)
  }

  override def updateScalarListType(projectId: String, modelName: String, fieldName: String, typeIdentifier: ScalarTypeIdentifier) = {
    val sqlType = typeMapper.rawSqlTypeForScalarTypeIdentifier(isList = false, typeIdentifier)
    sqlu"""ALTER TABLE #${qualify(projectId, s"${modelName}_$fieldName")} DROP INDEX "value", CHANGE COLUMN "value" "value" #$sqlType, ADD INDEX "value" ("value" ASC)"""
  }

  override def updateColumn(projectId: String,
                            tableName: String,
                            oldColumnName: String,
                            newColumnName: String,
                            newIsRequired: Boolean,
                            newIsList: Boolean,
                            newTypeIdentifier: ScalarTypeIdentifier): DBIOAction[Any, NoStream, Effect.All] = {
    val nulls   = if (newIsRequired) { "SET NOT NULL" } else { "DROP NOT NULL" }
    val sqlType = typeMapper.rawSqlTypeForScalarTypeIdentifier(newIsList, newTypeIdentifier)
    val renameIfNecessary = if (oldColumnName != newColumnName) {
      sqlu"""ALTER TABLE #${qualify(projectId, tableName)} RENAME COLUMN #${qualify(oldColumnName)} TO #${qualify(newColumnName)}"""
    } else {
      DatabaseAction.successful(())
    }

    DatabaseAction.seq(
      sqlu"""ALTER TABLE #${qualify(projectId, tableName)} ALTER COLUMN #${qualify(oldColumnName)} TYPE #$sqlType""",
      sqlu"""ALTER TABLE #${qualify(projectId, tableName)} ALTER COLUMN #${qualify(oldColumnName)} #$nulls""",
      renameIfNecessary
    )
  }

  override def renameTable(projectId: String, currentName: String, newName: String): DatabaseAction[Any, NoStream, Effect.All] = {
    sqlu"""ALTER TABLE #${qualify(projectId, currentName)} RENAME TO #${qualify(newName)}"""
  }

  override def addUniqueConstraint(projectId: String,
                                   tableName: String,
                                   columnName: String,
                                   typeIdentifier: ScalarTypeIdentifier): DatabaseAction[Any, NoStream, Effect.All] = {
    sqlu"""CREATE UNIQUE INDEX #${qualify(s"$projectId.$tableName.$columnName._UNIQUE")} ON #${qualify(projectId, tableName)}(#${qualify(columnName)} ASC);"""
  }

  override def removeUniqueConstraint(projectId: String, tableName: String, columnName: String): DatabaseAction[Any, NoStream, Effect.All] = {
    sqlu"""DROP INDEX #${qualify(projectId, s"$projectId.$tableName.$columnName._UNIQUE")}"""
  }
}
