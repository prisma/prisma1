package com.prisma.api.connector.postgresql

import com.prisma.shared.models.Schema

object Sketch {
  // possible names? Manifestation
  sealed trait Table
  sealed trait Column
  case class ScalarColumn(name: String, `type`: String)                                      extends Column
  case class ForeignKeyColumn(name: String, tableReference: String, columnReference: String) extends Column

  sealed trait ModelTable
  case class PrismaModelTable(id: Column, scalars: Vector[ScalarColumn])                                       extends ModelTable
  case class PlainModelTable(id: Column, scalars: Vector[ScalarColumn], foreignKeys: Vector[ForeignKeyColumn]) extends ModelTable // has an id and potentially many scalar fields

  sealed trait RelationTable
  case class PrismaRelationTable(id: Column, a: ForeignKeyColumn, b: ForeignKeyColumn) extends RelationTable // represents the default flavour
  case class PlainRelationTable(a: ForeignKeyColumn, b: ForeignKeyColumn)              extends RelationTable // a table with only 2 foreign keys

  sealed trait ScalarListTable // what is the condition to differentiate it from a model? maybe number of columns? we could inline all models that have only one column

  // inferer
  trait ManifestationInferer {
//    def infer(schema: String): Vector[Table]
    def infer(schema: String): Schema
  }
}
