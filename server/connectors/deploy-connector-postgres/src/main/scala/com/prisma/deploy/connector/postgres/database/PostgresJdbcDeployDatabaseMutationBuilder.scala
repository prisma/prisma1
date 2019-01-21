package com.prisma.deploy.connector.postgres.database

import com.prisma.connector.shared.jdbc.SlickDatabase
import com.prisma.deploy.connector.jdbc.database.{JdbcDeployDatabaseMutationBuilder, TypeMapper}
import com.prisma.gc_values.StringGCValue
import com.prisma.shared.models.FieldBehaviour.IdBehaviour
import com.prisma.shared.models.Manifestations.RelationTable
import com.prisma.shared.models.TypeIdentifier.ScalarTypeIdentifier
import com.prisma.shared.models.{Model, Project, Relation}
import com.prisma.utils.boolean.BooleanUtils
import org.jooq.impl.DSL
import slick.dbio.{DBIOAction => DatabaseAction}

import scala.concurrent.ExecutionContext

case class PostgresJdbcDeployDatabaseMutationBuilder(
    slickDatabase: SlickDatabase,
    typeMapper: TypeMapper
)(implicit val ec: ExecutionContext)
    extends JdbcDeployDatabaseMutationBuilder
    with BooleanUtils {

  import slickDatabase.profile.api._

  override def createSchema(projectId: String): DBIO[_] = {
    sqlu"CREATE SCHEMA #${qualify(projectId)}"
  }

  override def truncateProjectTables(project: Project): DBIO[_] = {
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

  override def createModelTable(projectId: String, model: Model): DBIO[_] = {
    val idField = model.idField_!
    val sequence = idField.behaviour.flatMap {
      case IdBehaviour(_, seq) => seq
      case _                   => None
    }

    val idFieldSQL = sequence match {
      case Some(seq) =>
        typeMapper.rawSQLFromParts(
          name = idField.dbName,
          isRequired = idField.isRequired,
          isList = false,
          typeIdentifier = idField.typeIdentifier,
          defaultValue = Some(StringGCValue(s"""nextval('"$projectId"."${seq.name}"'::regclass)"""))
        )
      case None =>
        typeMapper.rawSQLForField(idField)
    }

    val createSequenceIfRequired = sequence match {
      case Some(sequence) =>
        sqlu"""
              CREATE SEQUENCE "#$projectId"."#${sequence.name}" START #${sequence.initialValue}
            """
      case _ =>
        DBIO.successful(())
    }

    val createTable = sqlu"""
         CREATE TABLE #${qualify(projectId, model.dbName)} (
            #$idFieldSQL,
            PRIMARY KEY (#${qualify(idField.dbName)})
         )
      """

    DBIO.seq(createSequenceIfRequired, createTable)
  }

  override def createScalarListTable(projectId: String, model: Model, fieldName: String, typeIdentifier: ScalarTypeIdentifier): DBIO[_] = {
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
  ): DBIO[_] = {
    val relationTableName                   = relation.relationTableName
    val modelA                              = relation.modelA
    val modelB                              = relation.modelB
    val modelAColumn                        = relation.modelAColumn
    val modelBColumn                        = relation.modelBColumn
    val aColSql                             = typeMapper.rawSQLFromParts(modelAColumn, isRequired = true, isList = false, modelA.idField_!.typeIdentifier)
    val bColSql                             = typeMapper.rawSQLFromParts(modelBColumn, isRequired = true, isList = false, modelB.idField_!.typeIdentifier)
    def legacyTableCreate(idColumn: String) = sqlu"""
                        CREATE TABLE #${qualify(projectId, relationTableName)} (
                            "#$idColumn" CHAR(25) NOT NULL,
                            PRIMARY KEY ("#$idColumn"),
                            #$aColSql,
                            #$bColSql,
                            FOREIGN KEY ("#$modelAColumn") REFERENCES #${qualify(projectId, modelA.dbName)} (#${qualify(modelA.dbNameOfIdField_!)}) ON DELETE CASCADE,
                            FOREIGN KEY ("#$modelBColumn") REFERENCES #${qualify(projectId, modelB.dbName)} (#${qualify(modelA.dbNameOfIdField_!)}) ON DELETE CASCADE
                        );"""

    val modernTableCreate = sqlu"""
                        CREATE TABLE #${qualify(projectId, relationTableName)} (
                            #$aColSql,
                            #$bColSql,
                            FOREIGN KEY ("#$modelAColumn") REFERENCES #${qualify(projectId, modelA.dbName)} (#${qualify(modelA.dbNameOfIdField_!)}) ON DELETE CASCADE,
                            FOREIGN KEY ("#$modelBColumn") REFERENCES #${qualify(projectId, modelB.dbName)} (#${qualify(modelA.dbNameOfIdField_!)}) ON DELETE CASCADE
                        );"""

    val tableCreate = relation.manifestation match {
      case None                                         => legacyTableCreate("id")
      case Some(RelationTable(_, _, _, Some(idColumn))) => legacyTableCreate(idColumn)
      case _                                            => modernTableCreate
    }

    // we do not create an index on A because queries for the A column can be satisfied with the combined index as well
    val indexCreate =
      sqlu"""CREATE UNIQUE INDEX "#${relationTableName}_AB_unique" on #${qualify(projectId, relationTableName)} ("#$modelAColumn" ASC, "#$modelBColumn" ASC)"""
    val indexB = sqlu"""CREATE INDEX #${qualify(s"${relationTableName}_B")} on #${qualify(projectId, relationTableName)} ("#$modelBColumn" ASC)"""

    DatabaseAction.seq(tableCreate, indexCreate, indexB)
  }

  override def createRelationColumn(projectId: String, model: Model, references: Model, column: String): DBIO[_] = {
    val colSql = typeMapper.rawSQLFromParts(column, isRequired = false, isList = model.idField_!.isList, references.idField_!.typeIdentifier)

    sqlu"""ALTER TABLE #${qualify(projectId, model.dbName)} ADD COLUMN #$colSql
           REFERENCES #${qualify(projectId, references.dbName)} (#${qualify(references.dbNameOfIdField_!)}) ON DELETE SET NULL;"""
  }

  override def deleteRelationColumn(projectId: String, model: Model, references: Model, column: String): DBIO[_] = {
    deleteColumn(projectId, model.dbName, column)
  }

  override def createColumn(
      projectId: String,
      tableName: String,
      columnName: String,
      isRequired: Boolean,
      isUnique: Boolean,
      isList: Boolean,
      typeIdentifier: ScalarTypeIdentifier
  ): DBIO[_] = {
    val fieldSQL = typeMapper.rawSQLFromParts(columnName, isRequired, isList, typeIdentifier)
    val uniqueAction = isUnique match {
      case true  => addUniqueConstraint(projectId, tableName, columnName, typeIdentifier)
      case false => DatabaseAction.successful(())
    }

    val addColumn = sqlu"""ALTER TABLE #${qualify(projectId, tableName)} ADD COLUMN #$fieldSQL"""
    DatabaseAction.seq(addColumn, uniqueAction)
  }

  override def updateScalarListType(projectId: String, modelName: String, fieldName: String, newType: ScalarTypeIdentifier) = {
    val sqlType = typeMapper.rawSqlTypeForScalarTypeIdentifier(isList = false, newType)
    sqlu"""ALTER TABLE #${qualify(projectId, s"${modelName}_$fieldName")} DROP INDEX "value",
           CHANGE COLUMN "value" "value" #$sqlType,
           ADD INDEX "value" ("value" ASC)"""
  }

  override def updateColumn(projectId: String,
                            tableName: String,
                            oldColumnName: String,
                            newColumnName: String,
                            newIsRequired: Boolean,
                            newIsList: Boolean,
                            newTypeIdentifier: ScalarTypeIdentifier): DBIO[_] = {
    val nulls             = if (newIsRequired) { "SET NOT NULL" } else { "DROP NOT NULL" }
    val sqlType           = typeMapper.rawSqlTypeForScalarTypeIdentifier(newIsList, newTypeIdentifier)
    val renameIfNecessary = renameColumn(projectId, tableName, oldColumnName, newColumnName)

    DatabaseAction.seq(
      sqlu"""ALTER TABLE #${qualify(projectId, tableName)} ALTER COLUMN #${qualify(oldColumnName)} TYPE #$sqlType""",
      sqlu"""ALTER TABLE #${qualify(projectId, tableName)} ALTER COLUMN #${qualify(oldColumnName)} #$nulls""",
      renameIfNecessary
    )
  }

  override def addUniqueConstraint(projectId: String, tableName: String, columnName: String, typeIdentifier: ScalarTypeIdentifier): DBIO[_] = {
    sqlu"""CREATE UNIQUE INDEX #${qualify(s"$projectId.$tableName.$columnName._UNIQUE")} ON #${qualify(projectId, tableName)}(#${qualify(columnName)} ASC);"""
  }

  override def removeIndex(projectId: String, tableName: String, indexName: String): DBIO[_] = {
    sqlu"""DROP INDEX #${qualify(projectId, indexName)}"""
  }

  override def renameTable(projectId: String, oldTableName: String, newTableName: String) = {
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
}
