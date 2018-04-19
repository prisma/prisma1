package com.prisma.api.connector.postgresql.database

import com.prisma.api.connector.Types.DataItemFilterCollection
import com.prisma.api.connector.postgresql.database.JdbcExtensions._
import com.prisma.gc_values._
import com.prisma.shared.models.Model
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.{PositionedParameters, SQLActionBuilder, SetParameter}

object SlickExtensions {

  implicit object SetGcValueParam extends SetParameter[GCValue] {
    override def apply(gcValue: GCValue, pp: PositionedParameters): Unit = {
      val npos = pp.pos + 1
      pp.ps.setGcValue(npos, gcValue)
      pp.pos = npos
    }
  }

  implicit class SQLActionBuilderConcat(val a: SQLActionBuilder) extends AnyVal {
    def concat(b: SQLActionBuilder): SQLActionBuilder = {
      SQLActionBuilder(a.queryParts ++ " " ++ b.queryParts, (p: Unit, pp: PositionedParameters) => {
        a.unitPConv.apply(p, pp)
        b.unitPConv.apply(p, pp)
      })
    }
    def concat(b: Option[SQLActionBuilder]): SQLActionBuilder = b match {
      case Some(b) => a concat b
      case None    => a
    }

    def ++(b: SQLActionBuilder): SQLActionBuilder         = concat(b)
    def ++(b: Option[SQLActionBuilder]): SQLActionBuilder = concat(b)
  }

  def escapeUnsafeParam(string: String): SQLActionBuilder = sql"$string"

  def escapeKey(key: String) = sql""""#$key""""

  def combineByAnd(actions: Iterable[SQLActionBuilder]) = generateParentheses(combineBy(actions, "and"))

  def combineByOr(actions: Iterable[SQLActionBuilder]) = generateParentheses(combineBy(actions, "or"))

  def combineByComma(actions: Iterable[SQLActionBuilder]) = combineBy(actions, ",")

  def generateParentheses(sql: Option[SQLActionBuilder]) = sql match {
    case None      => None
    case Some(sql) => Some(sql"(" ++ sql ++ sql")")
  }

  // Use this with caution, since combinator is not escaped!
  def combineBy(actions: Iterable[SQLActionBuilder], combinator: String): Option[SQLActionBuilder] = actions.toList match {
    case Nil         => None
    case head :: Nil => Some(head)
    case _           => Some(actions.reduceLeft((a, b) => a ++ sql"#$combinator" ++ b))
  }

  def prefixIfNotNone(prefix: String, action: Option[SQLActionBuilder]): Option[SQLActionBuilder] = {
    if (action.isEmpty) None else Some(sql"#$prefix " ++ action.get)
  }

  def whereFilterAppendix(projectId: String, table: String, filter: Option[DataItemFilterCollection]) = {
    val whereSql = filter.flatMap(where => QueryArgumentsHelpers.generateFilterConditions(projectId, table, where))
    prefixIfNotNone("WHERE", whereSql)
  }
}
