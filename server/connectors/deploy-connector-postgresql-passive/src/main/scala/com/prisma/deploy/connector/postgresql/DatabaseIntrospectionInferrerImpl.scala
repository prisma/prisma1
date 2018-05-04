package com.prisma.deploy.connector.postgresql

import com.prisma.deploy.connector._
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.JdbcProfile
import slick.jdbc.meta.{MColumn, MForeignKey, MTable}

import scala.concurrent.Future

case class DatabaseIntrospectionInferrerImpl(jdbcProfile: JdbcProfile, db: Database, schema: String) extends DatabaseIntrospectionInferrer {
  import jdbcProfile.api._

  def infer(): Future[InferredTables] = db.run(action)

  def action: DBIO[InferredTables] = {
    for {
      tables         <- MTable.getTables(cat = Some(schema), schemaPattern = None, namePattern = None, types = None)
      inferredTables <- DBIO.sequence(tables.map(mTableToInferredTable))
    } yield {
      InferredTables(
        relationTables = inferredTables.collect { case x: InferredRelationTable => x },
        modelTables = inferredTables.collect { case x: InferredModelTable       => x }
      )
    }
  }

  def mTableToInferredTable(mTable: MTable): DBIO[InferredTable] = {
    for {
      columns      <- mTable.getColumns
      importedKeys <- mTable.getImportedKeys
    } yield {
      if (isRelationTable(columns, importedKeys)) {
        mTableToRelationTable(mTable, importedKeys)
      } else {
        mTableToModelTable(mTable, importedKeys)
      }
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

  def isRelationTable(columns: Vector[MColumn], importedKeys: Vector[MForeignKey]): Boolean = importedKeys.size == 2 && columns.size == 2

  def slickForeignKeyToOurForeignKeyColumn(fk: MForeignKey): InferredForeignKeyColumn = {
    InferredForeignKeyColumn(
      name = fk.fkColumn,
      referencesTable = fk.pkTable.name,
      referencesColumn = fk.pkColumn
    )
  }
}
