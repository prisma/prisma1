package cool.graph.deploy.database

import cool.graph.shared.models.TypeIdentifier
import cool.graph.shared.models.TypeIdentifier.TypeIdentifier
import slick.jdbc.MySQLProfile.api._

object DatabaseMutationBuilder {
  def createClientDatabaseForProject(projectId: String) = {
    val idCharset =
      charsetTypeForScalarTypeIdentifier(isList = false, TypeIdentifier.GraphQLID)

    DBIO.seq(
      sqlu"""CREATE SCHEMA `#$projectId` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; """,
      sqlu"""CREATE TABLE `#$projectId`.`_RelayId` (`id` CHAR(25) #$idCharset NOT NULL, `modelId` CHAR(25) #$idCharset NOT NULL, PRIMARY KEY (`id`), UNIQUE INDEX `id_UNIQUE` (`id` ASC)) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"""
    )
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
      case TypeIdentifier.Password  => "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
      case TypeIdentifier.Enum      => "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
      case TypeIdentifier.Json      => "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
      case TypeIdentifier.DateTime  => ""
    }
  }

}
