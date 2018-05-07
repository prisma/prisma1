package com.prisma.deploy.connector.postgresql

import com.prisma.deploy.connector._
import slick.jdbc.meta.{MColumn, MForeignKey, MTable}

import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.PostgresProfile.backend.DatabaseDef

case class DatabaseIntrospectionInferrerImpl(db: DatabaseDef, schema: String)(implicit ec: ExecutionContext) extends DatabaseIntrospectionInferrer {

  def infer(): Future[InferredTables] = db.run(action)

  def action: DBIO[InferredTables] = {
    for {
      tables         <- MTable.getTables(cat = None, schemaPattern = Some(schema), namePattern = None, types = None)
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
//      println(s"inferred table ${mTable.name.name}")
//      val columnNames = columns.map(_.name)
//      println(s"found the columns: $columnNames")

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
