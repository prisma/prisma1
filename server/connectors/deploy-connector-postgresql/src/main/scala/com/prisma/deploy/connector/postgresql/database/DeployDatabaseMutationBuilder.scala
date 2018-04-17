package com.prisma.deploy.connector.postgresql.database

import java.sql.PreparedStatement

import com.prisma.shared.models.TypeIdentifier.TypeIdentifier
import com.prisma.shared.models.{Project, TypeIdentifier}
import slick.jdbc.PostgresProfile.api._

object DeployDatabaseMutationBuilder {
  def createClientDatabaseForProject(projectId: String) = {

    val createFunction = SimpleDBIO[Unit] { x =>
      val query                             = """CREATE OR REPLACE FUNCTION raise_exception(text) RETURNS void as $$
                                                     BEGIN
                                                        RAISE EXCEPTION '%', $1;
                                                     END;
                                                     $$ Language plpgsql;"""
      val functionInsert: PreparedStatement = x.connection.prepareStatement(query)
      functionInsert.execute()
    }
    DBIO.seq(
      sqlu"""CREATE SCHEMA "#$projectId";""",
      sqlu"""CREATE TABLE "#$projectId"."_RelayId" ("id" CHAR(25) NOT NULL, "stableModelIdentifier" CHAR(25) NOT NULL, PRIMARY KEY ("id"))""",
      createFunction
    )
  }

  def truncateProjectTables(project: Project) = {
    val listTableNames: List[String] =
      project.models.flatMap(model => model.fields.collect { case field if field.isScalar && field.isList => s"${model.name}_${field.name}" })

    val tables = Vector("_RelayId") ++ project.models.map(_.name) ++ project.relations.map(_.relationTableName) ++ listTableNames

    DBIO.seq(tables.map(name => sqlu"""TRUNCATE TABLE  "#${project.id}"."#$name" CASCADE """): _*)
  }

  def deleteProjectDatabase(projectId: String) = sqlu"""DROP SCHEMA IF EXISTS "#$projectId" CASCADE"""

  def dropTable(projectId: String, tableName: String) = sqlu"""DROP TABLE "#$projectId"."#$tableName""""

  def createTable(projectId: String, name: String) = {
    sqlu"""CREATE TABLE "#$projectId"."#$name"
    ("id" CHAR(25) NOT NULL,
    "createdAt" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
    )"""
  }

  def dropScalarListTable(projectId: String, modelName: String, fieldName: String) = sqlu"""DROP TABLE "#$projectId"."#${modelName}_#${fieldName}""""

  def createScalarListTable(projectId: String, modelName: String, fieldName: String, typeIdentifier: TypeIdentifier) = {
    val sqlType = sqlTypeForScalarTypeIdentifier(typeIdentifier)
    sqlu"""CREATE TABLE "#$projectId"."#${modelName}_#${fieldName}"
    ("nodeId" CHAR(25) NOT NULL,
    "position" INT NOT NULL,
    "value" #$sqlType NOT NULL,
    PRIMARY KEY ("nodeId", "position")
    )"""
  }

  def updateScalarListType(projectId: String, modelName: String, fieldName: String, typeIdentifier: TypeIdentifier) = {
    val sqlType = sqlTypeForScalarTypeIdentifier(typeIdentifier)
    val indexSize = sqlType match {
      case "text" => "(191)"
      case _      => ""
    }

    sqlu"""ALTER TABLE "#$projectId"."#${modelName}_#${fieldName}" DROP INDEX "value", CHANGE COLUMN "value" "value" #$sqlType, ADD INDEX "value" ("value"#$indexSize ASC)"""
  }

  def renameScalarListTable(projectId: String, modelName: String, fieldName: String, newModelName: String, newFieldName: String) = {
    sqlu"""RENAME TABLE "#$projectId"."#${modelName}_#${fieldName}" TO "#$projectId"."#${newModelName}_#${newFieldName}""""
  }

  def renameTable(projectId: String, name: String, newName: String) = sqlu"""RENAME TABLE "#$projectId"."#$name" TO "#$projectId"."#$newName";"""

  def createColumn(projectId: String,
                   tableName: String,
                   columnName: String,
                   isRequired: Boolean,
                   isUnique: Boolean,
                   isList: Boolean,
                   typeIdentifier: TypeIdentifier.TypeIdentifier) = {

    val sqlType    = sqlTypeForScalarTypeIdentifier(typeIdentifier)
    val nullString = if (isRequired) "NOT NULL" else "NULL"
    val uniqueString =
      if (isUnique) {
        val indexSize = sqlType match {
          case "text" => "(191)"
          case _      => ""
        }

        s""""""
      } else { "" }

    sqlu"""ALTER TABLE "#$projectId"."#$tableName" ADD COLUMN "#$columnName"
         #$sqlType #$nullString #$uniqueString"""
  }

  def deleteColumn(projectId: String, tableName: String, columnName: String) = {
    sqlu"""ALTER TABLE "#$projectId"."#$tableName" DROP COLUMN "#$columnName", ALGORITHM = INPLACE"""
  }

  def updateColumn(projectId: String,
                   tableName: String,
                   oldColumnName: String,
                   newColumnName: String,
                   newIsRequired: Boolean,
                   newIsList: Boolean,
                   newTypeIdentifier: TypeIdentifier) = {
    val nulls   = if (newIsRequired) { "NOT NULL" } else { "NULL" }
    val sqlType = sqlTypeForScalarTypeIdentifier(newTypeIdentifier)

    sqlu"""ALTER TABLE "#$projectId"."#$tableName" CHANGE COLUMN "#$oldColumnName" "#$newColumnName" #$sqlType #$nulls"""
  }

  def addUniqueConstraint(projectId: String, tableName: String, columnName: String, typeIdentifier: TypeIdentifier, isList: Boolean) = {
    val sqlType = sqlTypeForScalarTypeIdentifier(typeIdentifier = typeIdentifier)

    val indexSize = sqlType match {
      case "text" | "mediumtext" => "(191)"
      case _                     => ""
    }

    sqlu"""ALTER TABLE  "#$projectId"."#$tableName" ADD UNIQUE INDEX "#${columnName}_UNIQUE" ("#$columnName"#$indexSize ASC)"""
  }

  def removeUniqueConstraint(projectId: String, tableName: String, columnName: String) = {
    sqlu"""ALTER TABLE  "#$projectId"."#$tableName" DROP INDEX "#${columnName}_UNIQUE""""
  }

  //todo indexes have to be added in a separate query
  def createRelationTable(projectId: String, tableName: String, aTableName: String, bTableName: String) = {
//    val idCharset = charsetTypeForScalarTypeIdentifier(isList = false, TypeIdentifier.GraphQLID)

    sqlu"""CREATE TABLE "#$projectId"."#$tableName" ("id" CHAR(25)  NOT NULL,
           PRIMARY KEY ("id"),
    "A" CHAR(25)  NOT NULL,
    "B" CHAR(25)  NOT NULL,
    FOREIGN KEY ("A") REFERENCES "#$projectId"."#$aTableName"("id") ON DELETE CASCADE,
    FOREIGN KEY ("B") REFERENCES "#$projectId"."#$bTableName"("id") ON DELETE CASCADE)
    ;"""
  }

  // note: utf8mb4 requires up to 4 bytes per character and includes full utf8 support, including emoticons
  // utf8 requires up to 3 bytes per character and does not have full utf8 support.
  // mysql indexes have a max size of 767 bytes or 191 utf8mb4 characters.
  // We limit enums to 191, and create text indexes over the first 191 characters of the string, but
  // allow the actual content to be much larger.
  // Key columns are utf8_general_ci as this collation is ~10% faster when sorting and requires less memory
  private def sqlTypeForScalarTypeIdentifier(typeIdentifier: TypeIdentifier): String = {
    typeIdentifier match {
      case TypeIdentifier.String    => "text"
      case TypeIdentifier.Boolean   => "boolean"
      case TypeIdentifier.Int       => "int"
      case TypeIdentifier.Float     => "Decimal(65,30)"
      case TypeIdentifier.GraphQLID => "char(25)"
      case TypeIdentifier.Enum      => "text"
      case TypeIdentifier.Json      => "text"
      case TypeIdentifier.DateTime  => "timestamp"
      case TypeIdentifier.Relation  => sys.error("Relation is not a scalar type. Are you trying to create a db column for a relation?")
    }
  }

}
