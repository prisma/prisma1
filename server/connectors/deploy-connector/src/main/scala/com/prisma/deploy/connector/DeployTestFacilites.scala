package com.prisma.deploy.connector

import com.prisma.shared.models.TypeIdentifier.TypeIdentifier

import scala.concurrent.Future

case class DeployTestFacilites(inspector: DatabaseInspector)

trait DatabaseInspector {
  def inspect(schema: String): Future[Tables]
}

object DatabaseInspector {
  val empty = new DatabaseInspector {
    override def inspect(schema: String) = Future.successful(Tables(Vector.empty))
  }
}

case class Tables(tables: Vector[Table]) {
  def table_!(name: String): Table       = table(name).getOrElse(sys.error(s"Table $name was not found."))
  def table(name: String): Option[Table] = tables.find(_.name == name)
}

case class Table(name: String, columns: Vector[Column], indexes: Vector[Index]) {
  def column_!(name: String): Column       = column(name).getOrElse(sys.error(s"Column $name was not found."))
  def column(name: String): Option[Column] = columns.find(_.name == name)
}

case class Index(name: String, columns: Vector[String], unique: Boolean)
case class Column(name: String, tpe: String, typeIdentifier: TypeIdentifier, isRequired: Boolean, foreignKey: Option[ForeignKey])
case class ForeignKey(table: String, column: String)
