package com.prisma.api

import com.prisma.api.connector.mysql.database.DatabaseMutationBuilder
import com.prisma.shared.models.TypeIdentifier.TypeIdentifier
import com.prisma.shared.models.{Model, TypeIdentifier}
import slick.dbio.DBIOAction
import slick.jdbc.MySQLProfile.api._

object DatabaseApiTestDatabaseMutationBuilder {

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
      sqlu"""CREATE SCHEMA `#$projectId` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; """,
      sqlu"""CREATE TABLE `#$projectId`.`_RelayId` (`id` CHAR(25) #$idCharset NOT NULL, `stableModelIdentifier` CHAR(25) #$idCharset NOT NULL, PRIMARY KEY (`id`), UNIQUE INDEX `id_UNIQUE` (`id` ASC)) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"""
    )
  }

  def dropDatabaseIfExists(database: String) =
    sqlu"DROP DATABASE IF EXISTS `#$database`"

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

  def createScalarListTable(projectId: String, modelName: String, fieldName: String, typeIdentifier: TypeIdentifier) = {
    val idCharset     = charsetTypeForScalarTypeIdentifier(isList = false, TypeIdentifier.GraphQLID)
    val sqlType       = DatabaseMutationBuilder.sqlTypeForScalarTypeIdentifier(false, typeIdentifier)
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
    INDEX `value` (`value`#$indexSize ASC),
    FOREIGN KEY (`nodeId`) REFERENCES `#$projectId`.`#$modelName`(id) ON DELETE CASCADE)
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"""
  }

  def dangerouslyTruncateTable(tableNames: Vector[String]): DBIOAction[Unit, NoStream, Effect] = {
    DBIO.seq(
      List(sqlu"""SET FOREIGN_KEY_CHECKS=0""") ++
        tableNames.map(name => sqlu"TRUNCATE TABLE `#$name`") ++
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

    val sqlType       = DatabaseMutationBuilder.sqlTypeForScalarTypeIdentifier(isList, typeIdentifier)
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
}
