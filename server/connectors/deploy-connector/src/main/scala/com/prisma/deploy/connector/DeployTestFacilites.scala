package com.prisma.deploy.connector

import com.prisma.shared.models.TypeIdentifier.TypeIdentifier

import scala.concurrent.Future

case class DeployTestFacilites(inspector: DatabaseInspector)

trait DatabaseInspector {
  def inspect(schema: String): Future[DatabaseSchema]
}

object DatabaseInspector {
  val empty = new DatabaseInspector {
    override def inspect(schema: String) = Future.successful(DatabaseSchema(Vector.empty))
  }
}

case class DatabaseSchema(tables: Vector[Table]) {
  def table_!(name: String): Table       = table(name).getOrElse(sys.error(s"Table $name was not found."))
  def table(name: String): Option[Table] = tables.find(_.name == name)
}
object DatabaseSchema {
  val empty = DatabaseSchema(Vector.empty)
}

case class Table(name: String, columnFns: Vector[Table => Column], indexes: Vector[Index]) {
  val columns: Vector[Column]                         = columnFns.map(_.apply(this))
  def column_!(name: String): Column                  = column(name).getOrElse(sys.error(s"Column $name was not found in table $name."))
  def column(name: String): Option[Column]            = columns.find(_.name == name)
  def indexByColumns_!(columns: String*): Index       = indexByColumns(columns: _*).getOrElse(sys.error(s"No index in table $name for the columns: $columns"))
  def indexByColumns(columns: String*): Option[Index] = indexes.find(_.columns == columns.toVector)

  override def equals(obj: Any): Boolean = {
    obj match {
      case other: Table => name == other.name && columns == other.columns && indexes == other.indexes
      case _            => false
    }
  }
}

case class Index(name: String, columns: Vector[String], unique: Boolean)
case class Column(name: String, tpe: String, typeIdentifier: TypeIdentifier, isRequired: Boolean, foreignKey: Option[ForeignKey])(val table: Table)
case class ForeignKey(table: String, column: String)
