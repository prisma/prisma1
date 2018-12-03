package com.prisma.deploy.connector.mysql.database

import com.prisma.connector.shared.jdbc.SlickDatabase
import com.prisma.deploy.connector.jdbc.database.{JdbcDeployDatabaseMutationBuilder, TypeMapper}
import com.prisma.shared.models.{Model, Project, Relation, TypeIdentifier}
import com.prisma.shared.models.TypeIdentifier.ScalarTypeIdentifier
import org.jooq.impl.DSL
import slick.dbio.{DBIOAction => DatabaseAction}

import scala.concurrent.ExecutionContext

case class MysqlJdbcDeployDatabaseMutationBuilder(
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
      changeDatabaseQueryToDBIO(sql.truncate(DSL.name(project.id, tableName)))()
    })

    DBIO.seq(sqlu"set foreign_key_checks=0" +: queries :+ sqlu"set foreign_key_checks=1": _*)
  }

  override def deleteProjectDatabase(projectId: String) = {
    sqlu"DROP DATABASE IF EXISTS #${qualify(projectId)}"
  }

  override def renameTable(projectId: String, currentName: String, newName: String): DBIOAction[Any, NoStream, Effect.All] = {
    sqlu"""RENAME TABLE #${qualify(projectId, currentName)} TO #${qualify(projectId, newName)};"""
  }

  override def createModelTable(projectId: String, model: Model): DatabaseAction[Any, NoStream, Effect.All] = {
    val idField    = model.idField_!
    val idFieldSQL = typeMapper.rawSQLForField(idField)

    sqlu"""CREATE TABLE #${qualify(projectId, model.dbName)} (
           #$idFieldSQL,
           PRIMARY KEY (#${qualify(idField.dbName)}),
           UNIQUE INDEX `id_UNIQUE` (#${qualify(idField.dbName)} ASC))
           DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"""
  }

  override def createScalarListTable(projectId: String,
                                     model: Model,
                                     fieldName: String,
                                     typeIdentifier: ScalarTypeIdentifier): DatabaseAction[Any, NoStream, Effect.All] = {
    val indexSize = indexSizeForSQLType(typeMapper.rawSqlTypeForScalarTypeIdentifier(isList = false, typeIdentifier))
    val nodeIdSql = typeMapper.rawSQLFromParts("nodeId", isRequired = true, isList = false, TypeIdentifier.Cuid)
    val valueSql  = typeMapper.rawSQLFromParts("value", isRequired = true, isList = false, typeIdentifier)

    sqlu"""CREATE TABLE #${qualify(projectId, s"${model.dbName}_$fieldName")} (
           #$nodeIdSql,
           `position` INT(4) NOT NULL,
           #$valueSql,
           PRIMARY KEY (`nodeId`, `position`),
           INDEX `value` (`value`#$indexSize ASC),
           FOREIGN KEY (`nodeId`) REFERENCES #${qualify(projectId, model.dbName)} (#${qualify(model.idField_!.dbName)}) ON DELETE CASCADE)
           DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"""
  }

  override def createRelationTable(projectId: String, relation: Relation): DatabaseAction[Any, NoStream, Effect.All] = {
    val modelA            = relation.modelA
    val modelB            = relation.modelB
    val relationTableName = relation.relationTableName
    val aColSql           = typeMapper.rawSQLFromParts("A", isRequired = true, isList = false, modelA.idField_!.typeIdentifier)
    val bColSql           = typeMapper.rawSQLFromParts("B", isRequired = true, isList = false, modelB.idField_!.typeIdentifier)
    val idSql             = typeMapper.rawSQLFromParts("id", isRequired = true, isList = false, TypeIdentifier.Cuid)

    sqlu"""
         CREATE TABLE #${qualify(projectId, relationTableName)} (
           #$idSql,
           PRIMARY KEY (`id`), UNIQUE INDEX `id_UNIQUE` (`id` ASC),
           #$aColSql, INDEX `A` (`A` ASC),
           #$bColSql, INDEX `B` (`B` ASC),
           UNIQUE INDEX `AB_unique` (`A` ASC, `B` ASC),
           FOREIGN KEY (A) REFERENCES #${qualify(projectId, modelA.dbName)}(#${qualify(modelA.dbNameOfIdField_!)}) ON DELETE CASCADE,
           FOREIGN KEY (B) REFERENCES #${qualify(projectId, modelB.dbName)}(#${qualify(modelB.dbNameOfIdField_!)}) ON DELETE CASCADE)
           DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"""
  }

  override def createRelationColumn(projectId: String, model: Model, references: Model, column: String): DatabaseAction[Any, NoStream, Effect.All] = {
    val colSql = typeMapper.rawSQLFromParts(column, isRequired = false, isList = model.idField_!.isList, references.idField_!.typeIdentifier)
    sqlu"""ALTER TABLE #${qualify(projectId, model.dbName)}
          ADD COLUMN #$colSql,
          ADD FOREIGN KEY (#${qualify(column)}) REFERENCES #${qualify(projectId, references.dbName)}(#${qualify(references.idField_!.dbName)}) ON DELETE CASCADE;
        """
  }

  override def createColumn(projectId: String,
                            tableName: String,
                            columnName: String,
                            isRequired: Boolean,
                            isUnique: Boolean,
                            isList: Boolean,
                            typeIdentifier: ScalarTypeIdentifier): DatabaseAction[Any, NoStream, Effect.All] = {
    val newColSql = typeMapper.rawSQLFromParts(columnName, isRequired = isRequired, isList = isList, typeIdentifier)
    val uniqueString =
      if (isUnique) {
        val indexSize = indexSizeForSQLType(typeMapper.rawSqlTypeForScalarTypeIdentifier(isList = isList, typeIdentifier))
        s", ADD UNIQUE INDEX ${qualify(s"${columnName}_UNIQUE")} (${qualify(columnName)}$indexSize ASC)"
      } else {
        ""
      }

    sqlu"""ALTER TABLE #${qualify(projectId, tableName)} ADD COLUMN #$newColSql #$uniqueString, ALGORITHM = INPLACE"""
  }

  override def updateScalarListType(projectId: String,
                                    modelName: String,
                                    fieldName: String,
                                    typeIdentifier: ScalarTypeIdentifier): DatabaseAction[Any, NoStream, Effect.All] = {
    val sqlType   = typeMapper.rawSqlTypeForScalarTypeIdentifier(isList = false, typeIdentifier)
    val indexSize = indexSizeForSQLType(sqlType)

    sqlu"ALTER TABLE #${qualify(projectId, s"${modelName}_$fieldName")} DROP INDEX `value`, CHANGE COLUMN `value` `value` #$sqlType, ADD INDEX `value` (`value`#$indexSize ASC)"
  }

  override def updateColumn(projectId: String,
                            tableName: String,
                            oldColumnName: String,
                            newColumnName: String,
                            newIsRequired: Boolean,
                            newIsList: Boolean,
                            newTypeIdentifier: ScalarTypeIdentifier): DatabaseAction[Any, NoStream, Effect.All] = {
    val newColSql = typeMapper.rawSQLFromParts(newColumnName, isRequired = newIsRequired, isList = newIsList, newTypeIdentifier)
    sqlu"ALTER TABLE #${qualify(projectId, tableName)} CHANGE COLUMN #${qualify(oldColumnName)} #$newColSql"
  }

  def indexSizeForSQLType(sql: String): String = sql match {
    case x if x.startsWith("text") | x.startsWith("mediumtext") => "(191)"
    case _                                                      => ""
  }

  override def addUniqueConstraint(projectId: String,
                                   tableName: String,
                                   columnName: String,
                                   typeIdentifier: ScalarTypeIdentifier): DatabaseAction[Any, NoStream, Effect.All] = {
    val sqlType   = typeMapper.rawSqlTypeForScalarTypeIdentifier(isList = false, typeIdentifier)
    val indexSize = indexSizeForSQLType(sqlType)

    sqlu"ALTER TABLE #${qualify(projectId, tableName)} ADD UNIQUE INDEX #${qualify(s"${columnName}_UNIQUE")}(#${qualify(columnName)}#$indexSize ASC)"
  }

  override def removeUniqueConstraint(projectId: String, tableName: String, columnName: String): DatabaseAction[Any, NoStream, Effect.All] = {
    sqlu"ALTER TABLE #${qualify(projectId, tableName)} DROP INDEX #${qualify(s"${columnName}_UNIQUE")}"
  }
}
