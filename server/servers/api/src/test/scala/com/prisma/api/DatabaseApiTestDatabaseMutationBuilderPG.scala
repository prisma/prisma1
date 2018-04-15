package com.prisma.api

import com.prisma.api.connector.mysql.database.DatabaseMutationBuilder
import com.prisma.shared.models.TypeIdentifier.TypeIdentifier
import com.prisma.shared.models.{Model, TypeIdentifier}
import slick.dbio.DBIOAction
import slick.jdbc.MySQLProfile.api._

object DatabaseApiTestDatabaseMutationBuilderPG {

  def charsetTypeForScalarTypeIdentifier(isList: Boolean, typeIdentifier: TypeIdentifier): String = {
    if (isList) return "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"

    typeIdentifier match {
      case TypeIdentifier.String    => "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
      case TypeIdentifier.Boolean   => ""
      case TypeIdentifier.Int       => ""
      case TypeIdentifier.Float     => ""
      case TypeIdentifier.GraphQLID => "CHARACTER SET utf8 COLLATE utf8_general_ci"
      case TypeIdentifier.Enum      => "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
      case TypeIdentifier.Json      => "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
      case TypeIdentifier.DateTime  => ""
    }
  }

  def createClientDatabaseForProject(projectId: String) = {
    val idCharset = charsetTypeForScalarTypeIdentifier(isList = false, TypeIdentifier.GraphQLID)

    DBIO.seq(
      sqlu"""CREATE SCHEMA #$projectId; """,
      sqlu"""CREATE TABLE #$projectId._RelayId (
                    id CHAR(25) NOT NULL,
                    stableModelIdentifier CHAR(25) NOT NULL,
                    PRIMARY KEY (id)
                    )"""
    )
  }

  def dropDatabaseIfExists(database: String) =
    sqlu"DROP SCHEMA IF EXISTS #$database cascade"

  def createTable(projectId: String, name: String) = {
    val idCharset = charsetTypeForScalarTypeIdentifier(isList = false, TypeIdentifier.GraphQLID)

    sqlu"""CREATE TABLE #$projectId.#$name
    (id CHAR(25) NOT NULL,
    createdAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
    )
    """
  }

  def createScalarListTable(projectId: String, modelName: String, fieldName: String, typeIdentifier: TypeIdentifier) = {
//    val idCharset     = charsetTypeForScalarTypeIdentifier(isList = false, TypeIdentifier.GraphQLID)
    val sqlType = sqlTypeForScalarTypeIdentifier(false, typeIdentifier)
//    val charsetString = charsetTypeForScalarTypeIdentifier(false, typeIdentifier)
    val indexSize = sqlType match {
      case "text" | "mediumtext" => "(191)"
      case _                     => ""
    }

    sqlu"""CREATE TABLE #$projectId.#${modelName}_#${fieldName}
    (nodeId CHAR(25)  NOT NULL,
    position INT(4) NOT NULL,
    value #$sqlType NOT NULL,
    PRIMARY KEY (nodeId, position),
    INDEX value (value#$indexSize ASC),
    FOREIGN KEY (nodeId) REFERENCES #$projectId.#$modelName(id) ON DELETE CASCADE)
    """
  }

  def dangerouslyTruncateTable(tableNames: Vector[String]): DBIOAction[Unit, NoStream, Effect] = {
    DBIO.seq(
      List(sqlu"""SET FOREIGN_KEY_CHECKS=0""") ++
        tableNames.map(name => sqlu"TRUNCATE TABLE #$name") ++
        List(sqlu"""SET FOREIGN_KEY_CHECKS=1"""): _*
    )
  }

  def createColumn(projectId: String,
                   tableName: String,
                   columnName: String,
                   isRequired: Boolean,
                   isUnique: Boolean,
                   isList: Boolean,
                   typeIdentifier: TypeIdentifier.TypeIdentifier) = {

    val sqlType = sqlTypeForScalarTypeIdentifier(isList, typeIdentifier)
//    val charsetString = charsetTypeForScalarTypeIdentifier(isList, typeIdentifier)
    val nullString = if (isRequired) "NOT NULL" else "NULL"
    val uniqueString =
      if (isUnique) {
        val indexSize = sqlType match {
          case "text" | "mediumtext" => "(191)"
          case _                     => ""
        }
        s""
//        s", ADD UNIQUE INDEX ${columnName}_UNIQUE ($columnName$indexSize ASC)"
      } else { "" }

    sqlu"""ALTER TABLE #$projectId.#$tableName ADD COLUMN "#$columnName"
         #$sqlType #$nullString #$uniqueString"""
  }

  def createTableForModel(projectId: String, model: Model) = {
    DBIO.seq(
      DBIO.seq(createTable(projectId, model.name)),
      DBIO.seq(
        model.scalarNonListFields
          .filter(f => !DatabaseMutationBuilder.implicitlyCreatedColumns.contains(f.name))
          .map { (field) =>
            createColumn(
              projectId = projectId,
              tableName = model.name,
              columnName = field.name,
              isRequired = field.isRequired,
              isUnique = field.isUnique,
              isList = field.isList,
              typeIdentifier = field.typeIdentifier
            )
          }: _*),
      DBIO.seq(model.scalarListFields.map { (field) =>
        createScalarListTable(projectId, model.name, field.name, field.typeIdentifier)
      }: _*)
    )
  }

  // note: utf8mb4 requires up to 4 bytes per character and includes full utf8 support, including emoticons
  // utf8 requires up to 3 bytes per character and does not have full utf8 support.
  // mysql indexes have a max size of 767 bytes or 191 utf8mb4 characters.
  // We limit enums to 191, and create text indexes over the first 191 characters of the string, but
  // allow the actual content to be much larger.
  // Key columns are utf8_general_ci as this collation is ~10% faster when sorting and requires less memory
  def sqlTypeForScalarTypeIdentifier(isList: Boolean, typeIdentifier: TypeIdentifier): String = {
    if (isList) return "mediumtext"

    typeIdentifier match {
      case TypeIdentifier.String    => "Text"
      case TypeIdentifier.Boolean   => "boolean"
      case TypeIdentifier.Int       => "int"
      case TypeIdentifier.Float     => "Decimal(65,30)"
      case TypeIdentifier.GraphQLID => "Char(25)"
      case TypeIdentifier.Enum      => "Varchar(191)"
      case TypeIdentifier.Json      => "Text"
      case TypeIdentifier.DateTime  => "Timestamp"
      case TypeIdentifier.Relation  => sys.error("Relation is not a scalar type. Are you trying to create a db column for a relation?")
    }
  }
}
