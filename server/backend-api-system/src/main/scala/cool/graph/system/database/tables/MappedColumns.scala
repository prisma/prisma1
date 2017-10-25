package cool.graph.system.database.tables

import slick.jdbc.MySQLProfile.api._
import spray.json.{JsArray, JsString}

import scala.util.Success

object MappedColumns {
  import cool.graph.util.json.Json._

  implicit val stringListMapper = MappedColumnType.base[Seq[String], String](
    list => JsArray(list.map(JsString.apply _).toVector).toString,
    _.tryParseJson match {
      case Success(json: JsArray) => json.elements.collect { case x: JsString => x.value }
      case _                      => Seq.empty
    }
  )
}
