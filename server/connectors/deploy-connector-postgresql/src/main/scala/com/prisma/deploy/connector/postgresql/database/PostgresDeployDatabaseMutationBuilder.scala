package com.prisma.deploy.connector.postgresql.database

import java.sql.PreparedStatement

import com.prisma.shared.models.TypeIdentifier.TypeIdentifier
import com.prisma.shared.models.{Field, Model, Project, TypeIdentifier}
import slick.dbio.DBIOAction
import slick.jdbc.PostgresProfile.api._

object PostgresDeployDatabaseMutationBuilder {

  def createClientDatabaseForProject(projectId: String) = {

    val createFunction = SimpleDBIO[Unit] { x =>
      val query                             = s"""CREATE OR REPLACE FUNCTION "$projectId".raise_exception(text)""" + """RETURNS void as $$
                                                     BEGIN
                                                        RAISE EXCEPTION '%', $1;
                                                     END;
                                                     $$ Language plpgsql;"""
      val functionInsert: PreparedStatement = x.connection.prepareStatement(query)
      functionInsert.execute()
    }

    DBIO.seq(
      sqlu"""CREATE SCHEMA "#$projectId";""",
      sqlu"""CREATE TABLE "#$projectId"."_RelayId" (
            "id" VARCHAR (25) NOT NULL,
            "stableModelIdentifier" VARCHAR (25) NOT NULL,
             PRIMARY KEY ("id"))""",
      createFunction
    )
  }

  def truncateProjectTables(project: Project) = {
    val listTableNames: List[String] =
      project.models.flatMap(model => model.fields.collect { case field if field.isScalar && field.isList => s"${model.dbName}_${field.dbName}" })

    val tables = Vector("_RelayId") ++ project.models.map(_.dbName) ++ project.relations.map(_.relationTableName) ++ listTableNames

    DBIO.seq(tables.map(name => sqlu"""TRUNCATE TABLE  "#${project.id}"."#$name" CASCADE """): _*)
  }

  def deleteProjectDatabase(projectId: String) = sqlu"""DROP SCHEMA IF EXISTS "#$projectId" CASCADE"""

  def dropTable(projectId: String, tableName: String)                              = sqlu"""DROP TABLE "#$projectId"."#$tableName""""
  def dropScalarListTable(projectId: String, modelName: String, fieldName: String) = sqlu"""DROP TABLE "#$projectId"."#${modelName}_#${fieldName}""""

  def createTable(projectId: String, name: String, nameOfIdField: String) = {

    sqlu"""CREATE TABLE "#$projectId"."#$name"
    ("#$nameOfIdField" VARCHAR (25) NOT NULL,
    PRIMARY KEY ("#$nameOfIdField")
    )"""
  }

  def createScalarListTable(projectId: String, model: Model, fieldName: String, typeIdentifier: TypeIdentifier) = {
    val sqlType = sqlTypeForScalarTypeIdentifier(typeIdentifier)
    sqlu"""CREATE TABLE "#$projectId"."#${model.dbName}_#$fieldName"
    ("nodeId" VARCHAR (25) NOT NULL REFERENCES "#$projectId"."#${model.dbName}" ("#${model.dbNameOfIdField_!}"),
    "position" INT NOT NULL,
    "value" #$sqlType NOT NULL,
    PRIMARY KEY ("nodeId", "position")
    )"""
  }

  def updateScalarListType(projectId: String, modelName: String, fieldName: String, typeIdentifier: TypeIdentifier) = {
    val sqlType = sqlTypeForScalarTypeIdentifier(typeIdentifier)
    sqlu"""ALTER TABLE "#$projectId"."#${modelName}_#${fieldName}" DROP INDEX "value", CHANGE COLUMN "value" "value" #$sqlType, ADD INDEX "value" ("value" ASC)"""
  }

  def renameScalarListTable(projectId: String, modelName: String, fieldName: String, newModelName: String, newFieldName: String) = {
    sqlu"""ALTER TABLE "#$projectId"."#${modelName}_#${fieldName}" RENAME TO "#${newModelName}_#${newFieldName}""""
  }

  def renameTable(projectId: String, name: String, newName: String) = sqlu"""ALTER TABLE "#$projectId"."#$name" RENAME TO "#$newName";"""

  def createColumn(projectId: String,
                   tableName: String,
                   columnName: String,
                   isRequired: Boolean,
                   isUnique: Boolean,
                   isList: Boolean,
                   typeIdentifier: TypeIdentifier.TypeIdentifier) = {

    val sqlType    = sqlTypeForScalarTypeIdentifier(typeIdentifier)
    val nullString = if (isRequired) "NOT NULL" else "NULL"
    val uniqueAction = isUnique match {
      case true  => sqlu"""CREATE UNIQUE INDEX "#$projectId.#$tableName.#$columnName._UNIQUE" ON "#$projectId"."#$tableName"("#$columnName" ASC);"""
      case false => DBIOAction.successful(())
    }

    val addColumn = sqlu"""ALTER TABLE "#$projectId"."#$tableName" ADD COLUMN "#$columnName" #$sqlType #$nullString"""

    DBIOAction.seq(addColumn, uniqueAction)
  }

  def deleteColumn(projectId: String, tableName: String, columnName: String) = {
    sqlu"""ALTER TABLE "#$projectId"."#$tableName" DROP COLUMN "#$columnName""""
  }

  def updateColumn(projectId: String,
                   tableName: String,
                   oldColumnName: String,
                   newColumnName: String,
                   newIsRequired: Boolean,
                   newIsList: Boolean,
                   newTypeIdentifier: TypeIdentifier) = {
    val nulls   = if (newIsRequired) { "SET NOT NULL" } else { "DROP NOT NULL" }
    val sqlType = sqlTypeForScalarTypeIdentifier(newTypeIdentifier)
    val renameIfNecessary =
      if (oldColumnName != newColumnName) sqlu"""ALTER TABLE "#$projectId"."#$tableName" RENAME COLUMN "#$oldColumnName" TO "#$newColumnName""""
      else DBIOAction.successful(())

    DBIOAction.seq(
      sqlu"""ALTER TABLE "#$projectId"."#$tableName" ALTER COLUMN "#$oldColumnName" TYPE #$sqlType""",
      sqlu"""ALTER TABLE "#$projectId"."#$tableName" ALTER COLUMN "#$oldColumnName" #$nulls""",
      renameIfNecessary
    )
  }

  def addUniqueConstraint(projectId: String, tableName: String, columnName: String, typeIdentifier: TypeIdentifier, isList: Boolean) = {
    sqlu"""CREATE UNIQUE INDEX "#$projectId.#$tableName.#$columnName._UNIQUE" ON "#$projectId"."#$tableName"("#$columnName" ASC);"""
  }

  def removeUniqueConstraint(projectId: String, tableName: String, columnName: String) = {
    sqlu"""DROP INDEX "#$projectId"."#$projectId.#$tableName.#$columnName._UNIQUE""""
  }

  def createRelationTable(projectId: String, relationTableName: String, modelA: Model, modelB: Model) = {

    val tableCreate = sqlu"""CREATE TABLE "#$projectId"."#$relationTableName" (
    "id" CHAR(25)  NOT NULL,
    PRIMARY KEY ("id"),
    "A" VARCHAR (25)  NOT NULL,
    "B" VARCHAR (25)  NOT NULL,
    FOREIGN KEY ("A") REFERENCES "#$projectId"."#${modelA.dbName}"("#${modelA.dbNameOfIdField_!}") ON DELETE CASCADE,
    FOREIGN KEY ("B") REFERENCES "#$projectId"."#${modelB.dbName}"("#${modelA.dbNameOfIdField_!}") ON DELETE CASCADE)
    ;"""

    val indexCreate = sqlu"""CREATE UNIQUE INDEX "#${relationTableName}_AB_unique" on  "#$projectId"."#$relationTableName" ("A" ASC, "B" ASC)"""

    DBIOAction.seq(tableCreate, indexCreate)
  }

  def createRelationColumn(projectId: String, model: Model, field: Option[Field], references: Model, column: String) = {
    val sqlType    = sqlTypeForScalarTypeIdentifier(TypeIdentifier.GraphQLID)
    val isRequired = false //field.exists(_.isRequired)
    val nullString = if (isRequired) "NOT NULL" else "NULL"
    val addColumn  = sqlu"""ALTER TABLE "#$projectId"."#${model.dbName}" ADD COLUMN "#$column" #$sqlType #$nullString
                            REFERENCES "#$projectId"."#${references.dbName}"(#${references.dbNameOfIdField_!}) ON DELETE SET NULL;"""
    addColumn
  }

  private def sqlTypeForScalarTypeIdentifier(typeIdentifier: TypeIdentifier): String = {
    typeIdentifier match {
      case TypeIdentifier.String    => "text"
      case TypeIdentifier.Boolean   => "boolean"
      case TypeIdentifier.Int       => "int"
      case TypeIdentifier.Float     => "Decimal(65,30)"
      case TypeIdentifier.GraphQLID => "varchar (25)"
      case TypeIdentifier.Enum      => "text"
      case TypeIdentifier.Json      => "text"
      case TypeIdentifier.DateTime  => "timestamp (3)"
      case TypeIdentifier.Relation  => sys.error("Relation is not a scalar type. Are you trying to create a db column for a relation?")
    }
  }

}
