package cool.graph.deploy.database.tables

import play.api.libs.json.JsValue
import slick.jdbc.MySQLProfile.api._
import spray.json.{JsArray, JsString}

import scala.util.Success

object MappedColumns {
  import cool.graph.shared.util.json.JsonUtils._

  implicit val stringListMapper = MappedColumnType.base[Seq[String], String](
    list => JsArray(list.map(JsString.apply).toVector).toString,
    _.tryParseJson match {
      case Success(json: JsArray) => json.elements.collect { case x: JsString => x.value }
      case _                      => Seq.empty
    }
  )

  implicit val jsonMapper = MappedColumnType.base[JsValue, String](
    json => json.toString(),
    _.tryParseJson.getOrElse(sys.error("Invalid JSON was inserted into the database. Can't read it back."))
  )
}
