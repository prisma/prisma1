package com.prisma.deploy.database.tables

import play.api.libs.json.{JsArray, JsString, JsValue}
import slick.jdbc.MySQLProfile.api._

import scala.util.Success

object MappedColumns {
  import com.prisma.utils.json.JsonUtils._

  implicit val stringListMapper = MappedColumnType.base[Seq[String], String](
    list => JsArray(list.map(JsString.apply).toVector).toString,
    _.tryParseJson match {
      case Success(json: JsArray) => json.value.collect { case x: JsString => x.value }
      case _                      => Seq.empty
    }
  )

  implicit val jsonMapper = MappedColumnType.base[JsValue, String](
    json => json.toString(),
    _.tryParseJson.getOrElse(sys.error("Invalid JSON was inserted into the database. Can't read it back."))
  )
}
