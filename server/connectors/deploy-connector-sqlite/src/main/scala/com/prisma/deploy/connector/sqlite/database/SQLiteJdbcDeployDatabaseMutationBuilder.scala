package com.prisma.deploy.connector.sqlite.database

import java.io.File

import com.prisma.connector.shared.jdbc.SlickDatabase
import com.prisma.deploy.connector.jdbc.database.{JdbcDeployDatabaseMutationBuilder, TypeMapper}
import com.prisma.gc_values.GCValue
import com.prisma.shared.models.TypeIdentifier.{ScalarTypeIdentifier, TypeIdentifier}
import com.prisma.shared.models.{Model, Project, Relation, TypeIdentifier}
import com.prisma.utils.boolean.BooleanUtils
import org.jooq.impl.DSL
import slick.dbio.{DBIOAction => DatabaseAction}

import scala.concurrent.ExecutionContext

case class SQLiteJdbcDeployDatabaseMutationBuilder(
    slickDatabase: SlickDatabase,
    typeMapper: TypeMapper
)(implicit val ec: ExecutionContext)
    extends JdbcDeployDatabaseMutationBuilder
    with BooleanUtils {

  import slickDatabase.profile.api._

  override def createSchema(projectId: String): DBIO[_] = {
    val list = sql"""PRAGMA database_list;""".as[(String, String, String)]
    val path = s"""'db/$projectId'"""
    val att  = sqlu"ATTACH DATABASE #${path} AS #$projectId;"

    for {
      attachedDbs <- list
      _ <- attachedDbs.map(_._2).contains(projectId) match {
            case true  => DBIO.successful(())
            case false => att
          }
    } yield ()
  }

  override def truncateProjectTables(project: Project): DBIO[_] = {
    val listTableNames: List[String] = project.models.flatMap { model =>
      model.fields.collect { case field if field.isScalar && field.isList => s"${model.dbName}_${field.dbName}" }
    }

    val tables = Vector("_RelayId") ++ project.models.map(_.dbName) ++ project.relations.map(_.relationTableName) ++ listTableNames
    val queries = tables.map(tableName => {
      changeDatabaseQueryToDBIO(sql.deleteFrom(DSL.table(DSL.name(project.id, tableName))))()
    })

    DBIO.seq(sqlu"PRAGMA foreign_keys=off" +: queries :+ sqlu"PRAGMA foreign_keys=on": _*)
  }

  override def deleteProjectDatabase(projectId: String) = {
    //check if db is attached
    //  yes ->  check if connected
    //          yes -> detach, delete
    //          no  -> delete
    //http://www.sqlitetutorial.net/sqlite-attach-database/
    val fileTemp = new File(s"""./db/$projectId""")

    if (fileTemp.exists) {
      //      val action = mutationBuilder.deleteProjectDatabase(projectId = id).map(_ => ())
      //      projectDatabase.run(action).map { x =>
      fileTemp.delete()
      //        ()
      //      }
    }
    DBIO.successful(())
  }

  override def createModelTable(projectId: String, model: Model): DBIO[_] = {
    val idField    = model.idField_!
    val idFieldSQL = typeMapper.rawSQLForField(idField)

    sqlu"""CREATE TABLE #${qualify(projectId, model.dbName)} (
           #$idFieldSQL,
           PRIMARY KEY (#${qualify(idField.dbName)})
           );"""
  }

  override def createScalarListTable(projectId: String, model: Model, fieldName: String, typeIdentifier: ScalarTypeIdentifier): DBIO[_] = {
    val nodeIdSql = typeMapper.rawSQLFromParts("nodeId", isRequired = true, isList = false, TypeIdentifier.Cuid)
    val valueSql  = typeMapper.rawSQLFromParts("value", isRequired = true, isList = false, typeIdentifier)

    val create = sqlu"""CREATE TABLE #${qualify(projectId, s"${model.dbName}_$fieldName")} (
           #$nodeIdSql,
           `position` INT(4) NOT NULL,
           #$valueSql,
           PRIMARY KEY (`nodeId`, `position`),
           FOREIGN KEY (`nodeId`) REFERENCES #${model.dbName} (#${qualify(model.idField_!.dbName)}) ON DELETE CASCADE)
           """
    val index  = sqlu"CREATE INDEX IF NOT EXISTS #${qualify(projectId, "value_Index")} ON #${qualify(s"${model.dbName}_$fieldName")} (#${qualify("value")} ASC)"

    DBIO.seq(create, index)
  }

  override def createRelationTable(projectId: String, relation: Relation): DBIO[_] = {
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
                            #$aColSql,
                            #$bColSql,
                            PRIMARY KEY ("id"),
                            FOREIGN KEY (#$modelAColumn) REFERENCES #${qualify(modelA.dbName)} (#${qualify(modelA.dbNameOfIdField_!)}) ON DELETE CASCADE,
                            FOREIGN KEY (#$modelBColumn) REFERENCES #${qualify(modelB.dbName)} (#${qualify(modelA.dbNameOfIdField_!)}) ON DELETE CASCADE
                        );"""

    val indexCreate =
      sqlu"""CREATE UNIQUE INDEX #${qualify(projectId, s"${relationTableName}_AB_unique")} on #$relationTableName ("#$modelAColumn" ASC, "#$modelBColumn" ASC)"""
    val indexB = sqlu"""CREATE INDEX #${qualify(projectId, s"${relationTableName}_B")} on #$relationTableName ("#$modelBColumn" ASC)"""

    DatabaseAction.seq(tableCreate, indexCreate, indexB)
  }

  override def updateRelationTable(projectId: String, previousRelation: Relation, nextRelation: Relation) = {
    val addOrRemoveId        = addOrRemoveIdColumn(projectId, previousRelation, nextRelation)
    val renameModelAColumn   = renameColumn(projectId, previousRelation.relationTableName, previousRelation.modelAColumn, nextRelation.modelAColumn)
    val renameModelBColumn   = renameColumn(projectId, previousRelation.relationTableName, previousRelation.modelBColumn, nextRelation.modelBColumn)
    val renameTableStatement = renameTable(projectId, previousRelation.relationTableName, nextRelation.relationTableName)

    val all = Vector(addOrRemoveId, renameModelAColumn, renameModelBColumn, renameTableStatement)
    DBIO.sequence(all)
  }

  override def createRelationColumn(projectId: String, model: Model, references: Model, column: String): DBIO[_] = {
    val colSql = typeMapper.rawSQLFromParts(column, isRequired = false, isList = model.idField_!.isList, references.idField_!.typeIdentifier)
    sqlu"""ALTER TABLE #${qualify(projectId, model.dbName)}
          ADD COLUMN #$colSql,
          ADD FOREIGN KEY (#${qualify(column)}) REFERENCES #${qualify(projectId, references.dbName)}(#${qualify(references.idField_!.dbName)}) ON DELETE CASCADE;
        """
  }

  override def deleteRelationColumn(projectId: String, model: Model, references: Model, column: String): DBIO[_] = {
    //Fixme see below
    for {
      namesOfForeignKey <- getNamesOfForeignKeyConstraints(projectId, model, column)
      _                 <- sqlu"""ALTER TABLE #${qualify(projectId, model.dbName)} DROP FOREIGN KEY `#${namesOfForeignKey.head}`;"""
      _                 <- sqlu"""ALTER TABLE #${qualify(projectId, model.dbName)} DROP COLUMN `#$column`;"""
    } yield ()
  }

  private def getNamesOfForeignKeyConstraints(projectId: String, model: Model, column: String): DatabaseAction[Vector[String], NoStream, Effect] = {
    //Fixme this probably does not work

    for {
      result <- sql"""
            SELECT
              CONSTRAINT_NAME
            FROM
              INFORMATION_SCHEMA.KEY_COLUMN_USAGE
            WHERE
              REFERENCED_TABLE_SCHEMA = '#$projectId' AND
              TABLE_NAME = '#${model.dbName}' AND
              COLUMN_NAME = '#$column';
          """.as[String]
    } yield result
  }

  override def createColumn(projectId: String,
                            tableName: String,
                            columnName: String,
                            isRequired: Boolean,
                            isUnique: Boolean,
                            isList: Boolean,
                            typeIdentifier: ScalarTypeIdentifier): DBIO[_] = {
    val newColSql = rawSQLFromParts(columnName, isRequired = isRequired, isList = isList, typeIdentifier)
    val unique    = if (isUnique) addUniqueConstraint(projectId, tableName, columnName, typeIdentifier) else DBIO.successful(())
    val add       = sqlu"""ALTER TABLE #${qualify(projectId, tableName)} ADD COLUMN #$newColSql"""
    DBIO.seq(add, unique)
  }

  override def updateScalarListType(projectId: String, modelName: String, fieldName: String, newType: ScalarTypeIdentifier): DBIO[_] = {
    val tableName    = s"${modelName}_$fieldName"
    val dropIndex    = removeIndex(projectId, tableName, "value_Index")
    val changeColumn = updateColumn(projectId, tableName, "value", "value", true, false, newType)
    val addIndex     = sqlu"CREATE INDEX IF NOT EXISTS #${qualify(projectId, "value_Index")} ON #${qualify(s"${modelName}_$fieldName")} (#${qualify("value")} ASC)"

    DBIO.seq(dropIndex, changeColumn, addIndex)
  }

  override def updateColumn(projectId: String,
                            tableName: String,
                            oldColumnName: String,
                            newColumnName: String,
                            newIsRequired: Boolean,
                            newIsList: Boolean,
                            newTypeIdentifier: ScalarTypeIdentifier): DBIO[_] = {

    val newColSql = rawSQLFromParts(newColumnName, isRequired = newIsRequired, isList = newIsList, newTypeIdentifier)
    sqlu"ALTER TABLE #${qualify(projectId, tableName)} CHANGE COLUMN #${qualify(oldColumnName)} #$newColSql"
    //Fixme -> Change does not work

  }

  def indexSizeForSQLType(sql: String): String = sql match {
    case x if x.startsWith("text") | x.startsWith("mediumtext") => "(191)"
    case _                                                      => ""
  }

  override def addUniqueConstraint(projectId: String, tableName: String, columnName: String, typeIdentifier: ScalarTypeIdentifier): DBIO[_] = {
    sqlu"CREATE UNIQUE INDEX IF NOT EXISTS #${qualify(projectId, s"${columnName}_UNIQUE")} ON #${qualify(tableName)} (#${qualify(columnName)} ASC)"
  }

  override def removeIndex(projectId: String, tableName: String, indexName: String): DBIO[_] = {
    sqlu"ALTER TABLE #${qualify(projectId, tableName)} DROP INDEX #${qualify(indexName)}"
  }

  override def renameTable(projectId: String, oldTableName: String, newTableName: String): DBIO[_] = {
    if (oldTableName != newTableName) {
      sqlu"""ALTER TABLE #${qualify(projectId, oldTableName)} RENAME TO #${qualify(newTableName)}"""
    } else {
      DatabaseAction.successful(())
    }
  }

  override def renameColumn(projectId: String, tableName: String, oldColumnName: String, newColumnName: String) = {
    if (oldColumnName != newColumnName) {
      sqlu"""ALTER TABLE #${qualify(projectId, tableName)} RENAME COLUMN #${qualify(oldColumnName)} TO #${qualify(newColumnName)}"""
    } else {
      DatabaseAction.successful(())
    }
  }

  def rawSQLFromParts(
      name: String,
      isRequired: Boolean,
      isList: Boolean,
      typeIdentifier: TypeIdentifier,
      isAutoGenerated: Boolean = false,
      defaultValue: Option[GCValue] = None
  ): String = {
    val n = typeMapper.esc(name)
    val defaultDefaultValue = typeIdentifier match {
      case TypeIdentifier.Cuid     => "defaultid"
      case TypeIdentifier.DateTime => "''"
      case TypeIdentifier.Boolean  => true
      case TypeIdentifier.UUID     => "defaultId"
      case TypeIdentifier.Enum     => "A"
      case TypeIdentifier.Float    => 0
      case TypeIdentifier.Int      => 0
      case TypeIdentifier.String   => "''"
      case TypeIdentifier.Json     => "''"
      case TypeIdentifier.Relation => "''"
    }
    val nullable  = if (isRequired) "NOT NULL" else "NULL" //Fixme
    val generated = if (isAutoGenerated) "AUTO_INCREMENT" else ""
    val ty        = typeMapper.rawSqlTypeForScalarTypeIdentifier(isList, typeIdentifier)
    val default = defaultValue match {
      case None if !isRequired    => ""
      case Some(d) if !isRequired => s"DEFAULT ${d.value}"
      case Some(d) if isRequired  => s"DEFAULT ${d.value}"
      case None if isRequired     => s"DEFAULT $defaultDefaultValue"
    }

    s"$n $ty $nullable $default $generated"
  }

}
