package com.prisma.deploy.connector.postgres.database

import com.prisma.deploy.connector._
import com.prisma.utils.boolean.BooleanUtils._
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.meta.{MColumn, MForeignKey, MTable}

import scala.concurrent.{ExecutionContext, Future}

case class DatabaseIntrospectionInferrerImpl(db: JdbcProfile#Backend#Database, schema: String)(implicit ec: ExecutionContext)
    extends DatabaseIntrospectionInferrer {

  def infer(): Future[InferredTables] = db.run(action)

  def action: DBIO[InferredTables] = {
    for {
      // the line below does not work perfectly on postgres. E.g. it will return tables for schemas "passive_test" and "passive$test" when param is "passive_test"
      // we therefore have one additional filter step
      potentialTables <- MTable.getTables(cat = Some(schema), schemaPattern = Some(schema), namePattern = None, types = None)
      tables          = potentialTables.filter(table => table.name.schema.contains(schema))
      inferredTables  <- DBIO.sequence(tables.map(mTableToInferredTables))
    } yield {
      InferredTables(
        relationTables = inferredTables.collect { case (_, Some(x)) => x },
        modelTables = inferredTables.collect { case (x, _)          => x }
      )
    }
  }

  def mTableToInferredTables(mTable: MTable): DBIO[(InferredModelTable, Option[InferredRelationTable])] = {
    for {
      columns      <- mTable.getColumns
      importedKeys <- mTable.getImportedKeys
    } yield {
      val modelTable    = mTableToModelTable(mTable, importedKeys)
      val relationTable = isRelationTable(columns, importedKeys).toOption(mTableToRelationTable(mTable, importedKeys))
      (modelTable, relationTable)
    }
  }

  def mTableToRelationTable(mTable: MTable, importedKeys: Vector[MForeignKey]): InferredRelationTable = {
    val foreignKey1 = slickForeignKeyToOurForeignKeyColumn(importedKeys(0))
    val foreignKey2 = slickForeignKeyToOurForeignKeyColumn(importedKeys(1))
    InferredRelationTable(name = mTable.name.name, foreignKey1 = foreignKey1, foreignKey2 = foreignKey2)
  }

  def mTableToModelTable(mTable: MTable, importedKeys: Vector[MForeignKey]): InferredModelTable = {
    InferredModelTable(name = mTable.name.name, foreignKeys = importedKeys.map(slickForeignKeyToOurForeignKeyColumn))
  }

  def isRelationTable(columns: Vector[MColumn], importedKeys: Vector[MForeignKey]): Boolean = importedKeys.size == 2

  def slickForeignKeyToOurForeignKeyColumn(fk: MForeignKey): InferredForeignKeyColumn = {
    InferredForeignKeyColumn(
      name = fk.fkColumn,
      referencesTable = fk.pkTable.name,
      referencesColumn = fk.pkColumn
    )
  }
}
