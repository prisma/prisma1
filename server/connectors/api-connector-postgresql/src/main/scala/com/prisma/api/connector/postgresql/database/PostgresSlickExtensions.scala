package com.prisma.api.connector.postgresql.database

import com.prisma.api.connector.postgresql.database.JdbcExtensions._
import com.prisma.gc_values._
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.{PositionedParameters, SQLActionBuilder, SetParameter}

object PostgresSlickExtensions {

  implicit class PositionedParameterExtensions(val pp: PositionedParameters) extends AnyVal {
    def setGcValue(value: GCValue): Unit = {
      val npos = pp.pos + 1
      pp.ps.setGcValue(npos, value)
      pp.pos = npos
    }
  }

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

  def escapeKey(key: String) = sql""""#$key""""

  def combineByComma(actions: Iterable[SQLActionBuilder]) = combineBy(actions, ",")

  // Use this with caution, since combinator is not escaped!
  private def combineBy(actions: Iterable[SQLActionBuilder], combinator: String): Option[SQLActionBuilder] = actions.toList match {
    case Nil         => None
    case head :: Nil => Some(head)
    case _           => Some(actions.reduceLeft((a, b) => a ++ sql"#$combinator" ++ b))
  }
}
