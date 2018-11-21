package com.prisma.deploy.connector

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

case class Tables(tables: Vector[Table])

case class Table(columns: Vector[Column], indexes: Vector[Index])

case class Index(name: String)
case class Column(name: String, tpe: String, foreignKey: Option[ForeignKey])
case class ForeignKey(table: String, column: String)
