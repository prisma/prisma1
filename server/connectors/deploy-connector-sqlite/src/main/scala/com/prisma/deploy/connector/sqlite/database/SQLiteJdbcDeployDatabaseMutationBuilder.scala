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

  override def deleteProjectDatabase(project: Project) = {
    //check if db is attached
    //  yes ->  check if connected
    //          yes -> detach, delete
    //          no  -> delete
    //http://www.sqlitetutorial.net/sqlite-attach-database/
    val fileTemp = new File(s"""./db/${project.dbName}""")

    if (fileTemp.exists) {
      //      val action = mutationBuilder.deleteProjectDatabase(projectId = id).map(_ => ())
      //      projectDatabase.run(action).map { x =>
      fileTemp.delete()
      //        ()
      //      }
    }
    DBIO.successful(())
  }

  override def createModelTable(project: Project, model: Model): DBIO[_] = {
    val idField    = model.idField_!
    val idFieldSQL = typeMapper.rawSQLForField(idField)

    sqlu"""CREATE TABLE #${qualify(project.dbName, model.dbName)} (
           #$idFieldSQL,
           PRIMARY KEY (#${qualify(idField.dbName)})
           );"""
  }

  override def createScalarListTable(project: Project, model: Model, fieldName: String, typeIdentifier: ScalarTypeIdentifier): DBIO[_] = {
    val nodeIdSql = typeMapper.rawSQLFromParts("nodeId", isRequired = true, isList = false, TypeIdentifier.Cuid)
    val valueSql  = typeMapper.rawSQLFromParts("value", isRequired = true, isList = false, typeIdentifier)

    val create = sqlu"""CREATE TABLE #${qualify(project.dbName, s"${model.dbName}_$fieldName")} (
           #$nodeIdSql,
           `position` INT(4) NOT NULL,
           #$valueSql,
           PRIMARY KEY (`nodeId`, `position`),
           FOREIGN KEY (`nodeId`) REFERENCES #${model.dbName} (#${qualify(model.idField_!.dbName)}) ON DELETE CASCADE)
           """
    val index =
      sqlu"CREATE INDEX IF NOT EXISTS #${qualify(project.dbName, "value_Index")} ON #${qualify(s"${model.dbName}_$fieldName")} (#${qualify("value")} ASC)"

    DBIO.seq(create, index)
  }

  override def createRelationTable(project: Project, relation: Relation): DBIO[_] = {
    val relationTableName = relation.relationTableName
    val modelA            = relation.modelA
    val modelB            = relation.modelB
    val modelAColumn      = relation.modelAColumn
    val modelBColumn      = relation.modelBColumn
    val aColSql           = typeMapper.rawSQLFromParts(modelAColumn, isRequired = true, isList = false, modelA.idField_!.typeIdentifier)
    val bColSql           = typeMapper.rawSQLFromParts(modelBColumn, isRequired = true, isList = false, modelB.idField_!.typeIdentifier)
    val tableCreate       = sqlu"""
                        CREATE TABLE #${qualify(project.dbName, relationTableName)} (
                            "id" CHAR(25) NOT NULL,
                            #$aColSql,
                            #$bColSql,
                            PRIMARY KEY ("id"),
                            FOREIGN KEY (#$modelAColumn) REFERENCES #${qualify(modelA.dbName)} (#${qualify(modelA.dbNameOfIdField_!)}) ON DELETE CASCADE,
                            FOREIGN KEY (#$modelBColumn) REFERENCES #${qualify(modelB.dbName)} (#${qualify(modelA.dbNameOfIdField_!)}) ON DELETE CASCADE
                        );"""

    val indexCreate =
      sqlu"""CREATE UNIQUE INDEX #${qualify(project.dbName, s"${relationTableName}_AB_unique")} on #$relationTableName ("#$modelAColumn" ASC, "#$modelBColumn" ASC)"""
    val indexB = sqlu"""CREATE INDEX #${qualify(project.dbName, s"${relationTableName}_B")} on #$relationTableName ("#$modelBColumn" ASC)"""

    DatabaseAction.seq(tableCreate, indexCreate, indexB)
  }

  override def createRelationColumn(project: Project, model: Model, references: Model, column: String): DBIO[_] = {
    val colSql = typeMapper.rawSQLFromParts(column, isRequired = false, isList = model.idField_!.isList, references.idField_!.typeIdentifier)
    sqlu"""ALTER TABLE #${qualify(project.dbName, model.dbName)}
          ADD COLUMN #$colSql,
          ADD FOREIGN KEY (#${qualify(column)}) REFERENCES #${qualify(project.dbName, references.dbName)}(#${qualify(references.idField_!.dbName)}) ON DELETE CASCADE;
        """
  }

  override def deleteRelationColumn(project: Project, model: Model, references: Model, column: String): DBIO[_] = {
    //Fixme see below
    for {
      namesOfForeignKey <- getNamesOfForeignKeyConstraints(project.dbName, model, column)
      _                 <- sqlu"""ALTER TABLE #${qualify(project.dbName, model.dbName)} DROP FOREIGN KEY `#${namesOfForeignKey.head}`;"""
      _                 <- sqlu"""ALTER TABLE #${qualify(project.dbName, model.dbName)} DROP COLUMN `#$column`;"""
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

  override def createColumn(project: Project,
                            tableName: String,
                            columnName: String,
                            isRequired: Boolean,
                            isUnique: Boolean,
                            isList: Boolean,
                            typeIdentifier: ScalarTypeIdentifier): DBIO[_] = {
    val newColSql = rawSQLFromParts(columnName, isRequired = isRequired, isList = isList, typeIdentifier)
    val unique    = if (isUnique) addUniqueConstraint(project.dbName, tableName, columnName, typeIdentifier) else DBIO.successful(())
    val add       = sqlu"""ALTER TABLE #${qualify(project.dbName, tableName)} ADD COLUMN #$newColSql"""
    DBIO.seq(add, unique)
  }

  override def updateColumn(project: Project,
                            model: Model,
                            oldColumnName: String,
                            newColumnName: String,
                            newIsRequired: Boolean,
                            newIsList: Boolean,
                            newTypeIdentifier: ScalarTypeIdentifier): DBIO[_] = {

//    https://www.sqlite.org/lang_altertable.html
//    If foreign key constraints are enabled, disable them using PRAGMA foreign_keys=OFF.
    val foreignKeysOff = sqlu"Pragma foreign_keys=OFF"

//    Start a transaction.
    val beginTransaction = sqlu"BEGIN TRANSACTION"
//    Remember the format of all indexes and triggers associated with table X. This information will be needed in step 8 below
//    One way to do this is to run a query like the following: SELECT type, sql FROM sqlite_master WHERE tbl_name='X'.

//    Use CREATE TABLE to construct a new table "new_X" that is in the desired revised format of table X.
//    Make sure that the name "new_X" does not collide with any existing table name, of course.
    val idField    = model.idField_!
    val idFieldSQL = typeMapper.rawSQLForField(idField)

    val createNewTable = sqlu"""CREATE TABLE #${qualify(project.dbName, s"${model.dbName}_TEMP_TABLE")} (
           #$idFieldSQL,
           PRIMARY KEY (#${qualify(idField.dbName)})
           );"""

    val addAllScalarNonListFields = DBIO.seq(model.scalarNonListFields.map { field =>
      createColumn(project, s"${model.dbName}_TEMP_TABLE", field.dbName, field.isRequired, field.isUnique, field.isList, field.typeIdentifier)
    }: _*)

//    Transfer content from X into new_X using a statement like: INSERT INTO new_X SELECT ... FROM X.
    val columnNames = model.scalarNonListFields.map(_.dbName).mkString(",")
    val transferContent =
      sqlu"""INSERT INTO #${qualify(project.dbName, s"${model.dbName}_TEMP_TABLE")} (#$columnNames)
             SELECT #$columnNames
             FROM #${qualify(project.dbName, s"${model.dbName}")}
          """

//    Drop the old table X: DROP TABLE X.
    val dropOldTable = sqlu"DROP TABLE #${qualify(project.dbName, model.dbName)} "
//    Change the name of new_X to X using: ALTER TABLE new_X RENAME TO X.

    val renameNewTable = sqlu"ALTER TABLE #${qualify(project.dbName, s"${model.dbName}_TEMP_TABLE")} RENAME TO #${qualify(project.dbName, model.dbName)}"
//    Use CREATE INDEX and CREATE TRIGGER to reconstruct indexes and triggers associated with table X. Perhaps use the old format of the triggers and indexes saved from step 3 above as a guide, making changes as appropriate for the alteration.
    val createIndexes = sqlu"" //Fixme
//    If any views refer to table X in a way that is affected by the schema change, then drop those views using DROP VIEW and recreate them with whatever changes are necessary to accommodate the schema change using CREATE VIEW.

//    If foreign key constraints were originally enabled then run PRAGMA foreign_key_check to verify that the schema change did not break any foreign key constraints.
    val foreignKeyCheck = sqlu"Pragma foreign_key_check"
//    Commit the transaction started in step 2.

    val commit = sqlu"COMMIT"
//    If foreign keys constraints were originally enabled, reenable them now.
    val foreignKeysOn = sqlu"Pragma foreign_keys=ON"

    DBIO.seq(
      foreignKeysOff,
      beginTransaction,
      createNewTable,
      addAllScalarNonListFields,
      transferContent,
      dropOldTable,
      renameNewTable,
      createIndexes,
      foreignKeyCheck,
      commit,
      foreignKeysOn
    )
  }

  override def deleteColumn(project: Project, tableName: String, columnName: String, model: Option[Model]) = {
    //if no model is provided, this concerns the relation table

    sqlu"""ALTER TABLE #${qualify(project.dbName, tableName)} DROP COLUMN #${qualify(columnName)}"""
  }

  override def addUniqueConstraint(project: Project, tableName: String, columnName: String, typeIdentifier: ScalarTypeIdentifier): DBIO[_] = {
    sqlu"CREATE UNIQUE INDEX IF NOT EXISTS #${qualify(project.dbName, s"${columnName}_UNIQUE")} ON #${qualify(tableName)} (#${qualify(columnName)} ASC)"
  }

  override def removeIndex(project: Project, tableName: String, indexName: String): DBIO[_] = {
    sqlu"ALTER TABLE #${qualify(project.dbName, tableName)} DROP INDEX #${qualify(indexName)}"
  }

  override def renameTable(project: Project, oldTableName: String, newTableName: String): DBIO[_] = {
    if (oldTableName != newTableName) {
      sqlu"""ALTER TABLE #${qualify(project.dbName, oldTableName)} RENAME TO #${qualify(newTableName)}"""
    } else {
      DatabaseAction.successful(())
    }
  }

  override def renameColumn(project: Project, tableName: String, oldColumnName: String, newColumnName: String) = {
    if (oldColumnName != newColumnName) {
      sqlu"""ALTER TABLE #${qualify(project.dbName, tableName)} RENAME COLUMN #${qualify(oldColumnName)} TO #${qualify(newColumnName)}"""
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
    val nullable  = if (isRequired) "NOT NULL" else "NULL"
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
