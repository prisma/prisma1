package cool.graph.client.database

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.{PositionedParameters, SQLActionBuilder, SetParameter}
import spray.json.DefaultJsonProtocol._
import spray.json._

object SlickExtensions {

  implicit class SQLActionBuilderConcat(a: SQLActionBuilder) {
    def concat(b: SQLActionBuilder): SQLActionBuilder = {
      SQLActionBuilder(a.queryParts ++ " " ++ b.queryParts, new SetParameter[Unit] {
        def apply(p: Unit, pp: PositionedParameters): Unit = {
          a.unitPConv.apply(p, pp)
          b.unitPConv.apply(p, pp)
        }
      })
    }
    def concat(b: Option[SQLActionBuilder]): SQLActionBuilder = b match {
      case Some(b) => a concat b
      case None    => a
    }
  }

  def listToJson(param: List[Any]): String = {
    param
      .map(_ match {
        case v: String     => v.toJson
        case v: JsValue    => v.toJson
        case v: Boolean    => v.toJson
        case v: Int        => v.toJson
        case v: Long       => v.toJson
        case v: Float      => v.toJson
        case v: Double     => v.toJson
        case v: BigInt     => v.toJson
        case v: BigDecimal => v.toJson
        case v: DateTime   => v.toString.toJson
      })
      .toJson
      .toString
  }

  def escapeUnsafeParam(param: Any) = {
    def unwrapSome(x: Any): Any = {
      x match {
        case Some(x) => x
        case x       => x
      }
    }
    unwrapSome(param) match {
      case param: String     => sql"$param"
      case param: JsValue    => sql"${param.compactPrint}"
      case param: Boolean    => sql"$param"
      case param: Int        => sql"$param"
      case param: Long       => sql"$param"
      case param: Float      => sql"$param"
      case param: Double     => sql"$param"
      case param: BigInt     => sql"#${param.toString}"
      case param: BigDecimal => sql"#${param.toString}"
      case param: DateTime =>
        sql"${param.toString(DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS").withZoneUTC())}"
      case param: Vector[_] => sql"${listToJson(param.toList)}"
      case None             => sql"NULL"
      case null             => sql"NULL"
      case _ =>
        throw new IllegalArgumentException("Unsupported scalar value in SlickExtensions: " + param.toString)
    }
  }

  def escapeKey(key: String) = sql"`#$key`"

  def combineByAnd(actions: Iterable[SQLActionBuilder]) =
    generateParentheses(combineBy(actions, "and"))
  def combineByOr(actions: Iterable[SQLActionBuilder]) =
    generateParentheses(combineBy(actions, "or"))
  def combineByComma(actions: Iterable[SQLActionBuilder]) =
    combineBy(actions, ",")

  def generateParentheses(sql: Option[SQLActionBuilder]) = {
    sql match {
      case None => None
      case Some(sql) =>
        Some(
          sql"(" concat sql concat sql")"
        )
    }
  }

  // Use this with caution, since combinator is not escaped!
  def combineBy(actions: Iterable[SQLActionBuilder], combinator: String): Option[SQLActionBuilder] =
    actions.toList match {
      case Nil         => None
      case head :: Nil => Some(head)
      case _ =>
        Some(actions.reduceLeft((a, b) => a concat sql"#$combinator" concat b))
    }

  def prefixIfNotNone(prefix: String, action: Option[SQLActionBuilder]): Option[SQLActionBuilder] = {
    if (action.isEmpty) None else Some(sql"#$prefix " concat action.get)
  }
}
