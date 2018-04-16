package com.prisma.deploy.connector.mysql.database

import com.prisma.shared.models.{Project, TypeIdentifier}
import com.prisma.shared.models.TypeIdentifier.TypeIdentifier
import slick.jdbc.MySQLProfile.api._

object DeployDatabaseMutationBuilder {
  def createClientDatabaseForProject(projectId: String) = {
    val idCharset = charsetTypeForScalarTypeIdentifier(isList = false, TypeIdentifier.GraphQLID)
    DBIO.seq(
      sqlu"""CREATE SCHEMA `#$projectId` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; """,
      sqlu"""CREATE TABLE `#$projectId`.`_RelayId` (`id` CHAR(25) #$idCharset NOT NULL, `stableModelIdentifier` CHAR(25) #$idCharset NOT NULL, PRIMARY KEY (`id`), UNIQUE INDEX `id_UNIQUE` (`id` ASC)) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"""
    )
  }

  def dropClientDatabaseForProject(projectId: String) = {
    println(s"Dropping $projectId")
    DBIO.seq(sqlu"""DROP SCHEMA IF EXISTS `#$projectId`;""")
  }

  def truncateProjectTables(project: Project) = {
    val listTableNames: List[String] =
      project.models.flatMap(model => model.fields.collect { case field if field.isScalar && field.isList => s"${model.name}_${field.name}" })

    val tables = Vector("_RelayId") ++ project.models.map(_.name) ++ project.relations.map(_.relationTableName) ++ listTableNames

    DBIO.seq(tables.map(name => sqlu"""TRUNCATE TABLE  #${project.id}.#$name CASCADE """): _*)
  }

  def deleteProjectDatabase(projectId: String) = sqlu"DROP DATABASE IF EXISTS `#$projectId`"

  def dropTable(projectId: String, tableName: String) = sqlu"DROP TABLE `#$projectId`.`#$tableName`"

  def createTable(projectId: String, name: String) = {
    val idCharset = charsetTypeForScalarTypeIdentifier(isList = false, TypeIdentifier.GraphQLID)

    sqlu"""CREATE TABLE `#$projectId`.`#$name`
    (`id` CHAR(25) #$idCharset NOT NULL,
    `createdAt` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updatedAt` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `id_UNIQUE` (`id` ASC))
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"""
  }

  def dropScalarListTable(projectId: String, modelName: String, fieldName: String) = sqlu"DROP TABLE `#$projectId`.`#${modelName}_#${fieldName}`"

  def createScalarListTable(projectId: String, modelName: String, fieldName: String, typeIdentifier: TypeIdentifier) = {
    val idCharset     = charsetTypeForScalarTypeIdentifier(isList = false, TypeIdentifier.GraphQLID)
    val sqlType       = sqlTypeForScalarTypeIdentifier(false, typeIdentifier)
    val charsetString = charsetTypeForScalarTypeIdentifier(false, typeIdentifier)
    val indexSize = sqlType match {
      case "text" | "mediumtext" => "(191)"
      case _                     => ""
    }

    sqlu"""CREATE TABLE `#$projectId`.`#${modelName}_#${fieldName}`
    (`nodeId` CHAR(25) #$idCharset NOT NULL,
    `position` INT(4) NOT NULL,
    `value` #$sqlType #$charsetString NOT NULL,
    PRIMARY KEY (`nodeId`, `position`),
    INDEX `value` (`value`#$indexSize ASC))
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"""
  }

  def updateScalarListType(projectId: String, modelName: String, fieldName: String, typeIdentifier: TypeIdentifier) = {
    val sqlType = sqlTypeForScalarTypeIdentifier(false, typeIdentifier)
//    val charsetString = charsetTypeForScalarTypeIdentifier(false, typeIdentifier)
    val indexSize = sqlType match {
      case "text" | "mediumtext" => "(191)"
      case _                     => ""
    }

    sqlu"ALTER TABLE `#$projectId`.`#${modelName}_#${fieldName}` DROP INDEX `value`, CHANGE COLUMN `value` `value` #$sqlType, ADD INDEX `value` (`value`#$indexSize ASC)"
  }

  def renameScalarListTable(projectId: String, modelName: String, fieldName: String, newModelName: String, newFieldName: String) = {
    sqlu"RENAME TABLE `#$projectId`.`#${modelName}_#${fieldName}` TO `#$projectId`.`#${newModelName}_#${newFieldName}`"
  }

  def renameTable(projectId: String, name: String, newName: String) = sqlu"""RENAME TABLE `#$projectId`.`#$name` TO `#$projectId`.`#$newName`;"""

  def charsetTypeForScalarTypeIdentifier(isList: Boolean, typeIdentifier: TypeIdentifier): String = {
    if (isList) {
      return "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
    }

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

  def createColumn(projectId: String,
                   tableName: String,
                   columnName: String,
                   isRequired: Boolean,
                   isUnique: Boolean,
                   isList: Boolean,
                   typeIdentifier: TypeIdentifier.TypeIdentifier) = {

    val sqlType       = sqlTypeForScalarTypeIdentifier(isList, typeIdentifier)
    val charsetString = charsetTypeForScalarTypeIdentifier(isList, typeIdentifier)
    val nullString    = if (isRequired) "NOT NULL" else "NULL"
    val uniqueString =
      if (isUnique) {
        val indexSize = sqlType match {
          case "text" | "mediumtext" => "(191)"
          case _                     => ""
        }

        s", ADD UNIQUE INDEX `${columnName}_UNIQUE` (`$columnName`$indexSize ASC)"
      } else { "" }

    sqlu"""ALTER TABLE `#$projectId`.`#$tableName` ADD COLUMN `#$columnName`
         #$sqlType #$charsetString #$nullString #$uniqueString, ALGORITHM = INPLACE"""
  }

  def deleteColumn(projectId: String, tableName: String, columnName: String) = {
    sqlu"ALTER TABLE `#$projectId`.`#$tableName` DROP COLUMN `#$columnName`, ALGORITHM = INPLACE"
  }

  def updateColumn(projectId: String,
                   tableName: String,
                   oldColumnName: String,
                   newColumnName: String,
                   newIsRequired: Boolean,
                   newIsList: Boolean,
                   newTypeIdentifier: TypeIdentifier) = {
    val nulls   = if (newIsRequired) { "NOT NULL" } else { "NULL" }
    val sqlType = sqlTypeForScalarTypeIdentifier(newIsList, newTypeIdentifier)

    sqlu"ALTER TABLE `#$projectId`.`#$tableName` CHANGE COLUMN `#$oldColumnName` `#$newColumnName` #$sqlType #$nulls"
  }

  def addUniqueConstraint(projectId: String, tableName: String, columnName: String, typeIdentifier: TypeIdentifier, isList: Boolean) = {
    val sqlType = sqlTypeForScalarTypeIdentifier(isList = isList, typeIdentifier = typeIdentifier)

    val indexSize = sqlType match {
      case "text" | "mediumtext" => "(191)"
      case _                     => ""
    }

    sqlu"ALTER TABLE  `#$projectId`.`#$tableName` ADD UNIQUE INDEX `#${columnName}_UNIQUE` (`#$columnName`#$indexSize ASC)"
  }

  def removeUniqueConstraint(projectId: String, tableName: String, columnName: String) = {
    sqlu"ALTER TABLE  `#$projectId`.`#$tableName` DROP INDEX `#${columnName}_UNIQUE`"
  }

  def createRelationTable(projectId: String, tableName: String, aTableName: String, bTableName: String) = {
    val idCharset = charsetTypeForScalarTypeIdentifier(isList = false, TypeIdentifier.GraphQLID)

    sqlu"""CREATE TABLE `#$projectId`.`#$tableName` (`id` CHAR(25) #$idCharset NOT NULL,
           PRIMARY KEY (`id`), UNIQUE INDEX `id_UNIQUE` (`id` ASC),
    `A` CHAR(25) #$idCharset NOT NULL, INDEX `A` (`A` ASC),
    `B` CHAR(25) #$idCharset NOT NULL, INDEX `B` (`B` ASC),
    UNIQUE INDEX `AB_unique` (`A` ASC, `B` ASC),
    FOREIGN KEY (A) REFERENCES `#$projectId`.`#$aTableName`(id) ON DELETE CASCADE,
    FOREIGN KEY (B) REFERENCES `#$projectId`.`#$bTableName`(id) ON DELETE CASCADE)
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"""
  }

  // note: utf8mb4 requires up to 4 bytes per character and includes full utf8 support, including emoticons
  // utf8 requires up to 3 bytes per character and does not have full utf8 support.
  // mysql indexes have a max size of 767 bytes or 191 utf8mb4 characters.
  // We limit enums to 191, and create text indexes over the first 191 characters of the string, but
  // allow the actual content to be much larger.
  // Key columns are utf8_general_ci as this collation is ~10% faster when sorting and requires less memory
  private def sqlTypeForScalarTypeIdentifier(isList: Boolean, typeIdentifier: TypeIdentifier): String = {
    if (isList) {
      return "mediumtext"
    }

    typeIdentifier match {
      case TypeIdentifier.String    => "mediumtext"
      case TypeIdentifier.Boolean   => "boolean"
      case TypeIdentifier.Int       => "int"
      case TypeIdentifier.Float     => "Decimal(65,30)"
      case TypeIdentifier.GraphQLID => "char(25)"
      case TypeIdentifier.Enum      => "varchar(191)"
      case TypeIdentifier.Json      => "mediumtext"
      case TypeIdentifier.DateTime  => "datetime(3)"
      case TypeIdentifier.Relation  => sys.error("Relation is not a scalar type. Are you trying to create a db column for a relation?")
    }
  }

}
