package com.prisma.native_jdbc

import java.sql.{BatchUpdateException, SQLException}
import play.api.libs.json.{JsArray, Json}

import scala.util.Try

object RustCallResult {
  implicit val resultColumnFormat = Json.format[ResultColumn]
  implicit val resultSetFormat    = Json.format[RustResultSet]
  implicit val errorFormat        = Json.format[RustError]
  implicit val protocolFormat     = Json.format[RustCallResult]

  def fromString(str: String): RustCallResult = {
    (for {
      json     <- Try { Json.parse(str) }
      protocol <- Try { json.as[RustCallResult] }
    } yield {
      protocol.error.foreach { err =>
        throw new SQLException(err.message, err.code)
      }

      if (protocol.isCount && protocol.counts.contains(-3)) {
        throw new BatchUpdateException(protocol.counts.toArray)
      }

      protocol
    }).get
  }
}

case class RustCallResult(ty: String, counts: Vector[Int], rows: Option[RustResultSet], error: Option[RustError]) {
  def isResultSet = ty == "RESULT_SET"
  def isError     = ty == "ERROR"
  def isCount     = ty == "COUNT"
  def isEmpty     = ty == "EMPTY"
  def toResultSet = JsonResultSet(rows.get)
}

object RustResultSet {
  val empty = RustResultSet(Vector.empty, IndexedSeq.empty)
}

case class RustResultSet(columns: Vector[ResultColumn], data: IndexedSeq[JsArray])
case class ResultColumn(name: String, discriminator: String)
case class RustError(code: String, message: String)
