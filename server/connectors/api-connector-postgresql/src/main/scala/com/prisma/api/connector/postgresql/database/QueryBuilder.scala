package com.prisma.api.connector.postgresql.database

import com.prisma.api.connector.{Filter, NodeSelector}
import com.prisma.gc_values.GCValue
import com.prisma.shared.models.{Field, Model}
import slick.jdbc.SQLActionBuilder
import slick.jdbc.PostgresProfile.api._
import SlickExtensions._

object QueryDsl {
  def select(model: Model): QueryBuilder = QueryBuilder(model, Vector.empty)

}

case class QueryBuilderWhere(field: Field, value: GCValue)

case class QueryBuilder(model: Model, wheres: Vector[QueryBuilderWhere]) {

  def where(field: Field, value: GCValue): QueryBuilder = copy(wheres = wheres :+ QueryBuilderWhere(field, value))

  def build(schemaName: String): SQLActionBuilder = {
    val where = prefixIfNotNone("WHERE", combineByAnd(wheres.map { where =>
      sql""" "#${schemaName}"."#${model.dbName}"."#${where.field.dbName}" = ${where.value} """
    }))

    sql"""select * from "#${schemaName}"."#${model.dbName}" """ ++
      where
  }
}
