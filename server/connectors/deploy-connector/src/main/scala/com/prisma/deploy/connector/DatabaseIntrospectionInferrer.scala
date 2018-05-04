package com.prisma.deploy.connector

import scala.concurrent.Future

trait DatabaseIntrospectionInferrer {
  def infer(): Future[InferredTables]
}

case class InferredTables(
    relationTables: Vector[InferredRelationTable],
    modelTables: Vector[InferredModelTable]
)

sealed trait InferredTable

case class InferredModelTable(name: String, foreignKeys: Vector[InferredForeignKeyColumn]) extends InferredTable {
  def columnNameForReferencedTable(table: String): Option[String] = {
    foreignKeys.find(_.referencesTable == table).map(_.name)
  }
}

case class InferredRelationTable(name: String, foreignKey1: InferredForeignKeyColumn, foreignKey2: InferredForeignKeyColumn) extends InferredTable {
  def columnForTable(table: String): Option[String] = {
    if (foreignKey1.referencesTable == table) Some(foreignKey1.name)
    else if (foreignKey2.referencesTable == table) Some(foreignKey2.name)
    else None
  }
}
case class InferredForeignKeyColumn(name: String, referencesTable: String, referencesColumn: String)

object EmptyDatabaseIntrospectionInferrer extends DatabaseIntrospectionInferrer {
  override def infer() = Future.successful(InferredTables(Vector.empty, Vector.empty))
}
