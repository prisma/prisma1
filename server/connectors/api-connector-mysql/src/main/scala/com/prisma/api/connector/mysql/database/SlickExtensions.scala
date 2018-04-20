package com.prisma.api.connector.mysql.database

import com.prisma.api.connector.Types.DataItemFilterCollection
import com.prisma.api.connector.mysql.database.JdbcExtensions._
import com.prisma.gc_values._
import com.prisma.shared.models.Model
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.{JsValue => PlayJsValue}
import slick.jdbc.MySQLProfile.api._
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

  def escapeUnsafeParam(param: Any): SQLActionBuilder = {
    def unwrapSome(x: Any): Any = x match {
      case Some(x) => x
      case x       => x
    }

    unwrapSome(param) match {
      case param: String      => sql"$param"
      case param: PlayJsValue => sql"${param.toString}"
      case param: Boolean     => sql"$param"
      case param: Int         => sql"$param"
      case param: Long        => sql"$param"
      case param: Float       => sql"$param"
      case param: Double      => sql"$param"
      case param: BigInt      => sql"#${param.toString}"
      case param: BigDecimal  => sql"#${param.toString}"
      case param: DateTime    => sql"${param.toString(DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS").withZoneUTC())}"
      case None               => sql"NULL"
      case null               => sql"NULL"
      case _                  => throw new IllegalArgumentException("Unsupported scalar value in SlickExtensions: " + param.toString)
    }
  }

  def gcValueToSQLBuilder(gcValue: GCValue): SQLActionBuilder = {
    val dateTimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS").withZoneUTC()
    gcValue match {
      case NullGCValue            => sql"NULL"
      case StringGCValue(value)   => sql"$value"
      case EnumGCValue(value)     => sql"$value"
      case IdGCValue(value)       => sql"$value"
      case DateTimeGCValue(value) => sql"${dateTimeFormat.print(value)}"
      case IntGCValue(value)      => sql"$value"
      case FloatGCValue(value)    => sql"$value"
      case BooleanGCValue(value)  => sql"$value"
      case JsonGCValue(value)     => sql"${value.toString}"
      case ListGCValue(_)         => sys.error("ListGCValue not implemented here yet.")
      case RootGCValue(_)         => sys.error("RootGCValues not implemented here yet.")
    }
  }

  def escapeKey(key: String) = sql"`#$key`"

  def combineByAnd(actions: Iterable[SQLActionBuilder]) = generateParentheses(combineBy(actions, "and"))

  def combineByOr(actions: Iterable[SQLActionBuilder]) = generateParentheses(combineBy(actions, "or"))

  def combineByNot(actions: Iterable[SQLActionBuilder]) = generateParentheses(combinedBy(actions, "not"))

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

  def whereFilterAppendix(projectId: String, model: Model, filter: Option[DataItemFilterCollection]) = {
    val whereSql = filter.flatMap(where => QueryArgumentsHelpers.generateFilterConditions(projectId, model.name, where))
    prefixIfNotNone("WHERE", whereSql)
  }
}
