package com.prisma.native_jdbc

import java.sql.SQLException

import com.sun.jna.{Native, Pointer}
import play.api.libs.json.{JsArray, JsObject, Json}

import scala.util.Try

sealed trait RustConnection
//class RustConnectionGraal(val conn: CIntegration.RustConnection) extends RustConnection
class RustConnectionJna(val conn: Pointer) extends RustConnection

trait RustBinding[T <: RustConnection] {
  def newConnection(url: String): T
  def startTransaction(connection: T): RustCallResult
  def commitTransaction(connection: T): RustCallResult
  def rollbackTransaction(connection: T): RustCallResult
  def closeConnection(connection: T): RustCallResult
  def sqlExecute(connection: T, query: String, params: String): RustCallResult
  def sqlQuery(connection: T, query: String, params: String): RustCallResult
}

object RustCallResult {
  implicit val resultSetFormat = Json.format[RustResultSet]
  implicit val errorFormat     = Json.format[RustError]
  implicit val protocolFormat  = Json.format[RustCallResult]

  def fromString(str: String): RustCallResult = {
    (for {
      json     <- Try { Json.parse(str) }
      protocol <- Try { json.as[RustCallResult] }
    } yield {
      protocol.error.foreach(err => throw new SQLException(err.message, err.code))
      protocol
    }).get
  }
}

case class RustError(code: String, message: String)

case class RustCallResult(ty: String, count: Option[Int], rows: Option[RustResultSet], error: Option[RustError]) {
  def isResultSet = ty == "RESULT_SET"
  def isError     = ty == "ERROR"
  def isCount     = ty == "COUNT"
  def isEmpty     = ty == "EMPTY"
  def toResultSet = JsonResultSet(rows.get)
}

object RustResultSet {
  val empty = RustResultSet(Vector.empty, IndexedSeq.empty)
}

case class RustResultSet(columns: Vector[String], data: IndexedSeq[JsArray])

//object RustGraalImpl extends RustBinding[RustConnectionGraal] {
//  def toJavaString(str: CCharPointer) = CTypeConversion.toJavaString(str)
//  def toCString(str: String)          = CTypeConversion.toCString(str).get()
//
//  override def newConnection(url: String): RustConnectionGraal            = new RustConnectionGraal(RustInterfaceGraal.newConnection(toCString(url)))
//  override def startTransaction(connection: RustConnectionGraal): Unit    = RustInterfaceGraal.startTransaction(connection.conn)
//  override def commitTransaction(connection: RustConnectionGraal): Unit   = RustInterfaceGraal.commitTransaction(connection.conn)
//  override def rollbackTransaction(connection: RustConnectionGraal): Unit = RustInterfaceGraal.rollbackTransaction(connection.conn)
//  override def closeConnection(connection: RustConnectionGraal): Unit     = RustInterfaceGraal.closeConnection(connection.conn)
//  override def sqlExecute(connection: RustConnectionGraal, query: String, params: String): Unit =
//    RustInterfaceGraal.sqlExecute(connection.conn, toCString(query), toCString(params))
//  override def sqlQuery(connection: RustConnectionGraal, query: String, params: String): String =
//    toJavaString(RustInterfaceGraal.sqlQuery(connection.conn, toCString(query), toCString(params)))
//}

object RustJnaImpl extends RustBinding[RustConnectionJna] {
  val currentDir = System.getProperty("user.dir")

  System.setProperty("jna.debug_load.jna", "true")
  System.setProperty("jna.debug_load", "true")

  val library = Native.loadLibrary("jdbc_native", classOf[JnaRustBridge])

  override def newConnection(url: String): RustConnectionJna = {
    new RustConnectionJna(library.newConnection(url))
  }

  override def startTransaction(connection: RustConnectionJna): RustCallResult = {
    RustCallResult.fromString(library.startTransaction(connection.conn))
  }

  override def commitTransaction(connection: RustConnectionJna): RustCallResult = {
    RustCallResult.fromString(library.commitTransaction(connection.conn))
  }

  override def rollbackTransaction(connection: RustConnectionJna): RustCallResult = {
    RustCallResult.fromString(library.rollbackTransaction(connection.conn))
  }

  override def closeConnection(connection: RustConnectionJna): RustCallResult = {
    RustCallResult.fromString(library.closeConnection(connection.conn))
  }

  override def sqlExecute(connection: RustConnectionJna, query: String, params: String): RustCallResult = {
    println(s"[JNA] Execute: '$query' with params: $params")
    val result = library.sqlExecute(connection.conn, query, params)
    println(s"[JNA] Result: $result")
    RustCallResult.fromString(result)
  }

  override def sqlQuery(connection: RustConnectionJna, query: String, params: String): RustCallResult = {
    println(s"[JNA] Query: '$query' with params: $params")
    val result = library.sqlQuery(connection.conn, query, params)
    println(s"[JNA] Result: $result")
    RustCallResult.fromString(result)
  }
}
