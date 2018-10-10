package com.prisma.native_jdbc

import java.sql.{BatchUpdateException, SQLException}

import com.sun.jna.{Native, Pointer}
import play.api.libs.json.{JsArray, Json}

import scala.util.Try

sealed trait RustConnection
//class RustConnectionGraal(val conn: CIntegration.RustConnection) extends RustConnection
class RustConnectionJna(val conn: Pointer) extends RustConnection

sealed trait RustPreparedStatement
class RustPreparedStatementJna(val stmt: Pointer) extends RustPreparedStatement

trait RustBinding {
  type Conn <: RustConnection
  type Stmt <: RustPreparedStatement

  def newConnection(url: String): Conn
  def prepareStatement(connection: Conn, query: String): Stmt
  def startTransaction(connection: Conn): RustCallResult
  def commitTransaction(connection: Conn): RustCallResult
  def rollbackTransaction(connection: Conn): RustCallResult
  def closeConnection(connection: Conn): RustCallResult
  def sqlExecute(connection: Conn, query: String, params: String): RustCallResult
  def sqlQuery(connection: Conn, query: String, params: String): RustCallResult
  def executePreparedstatement(stmt: Stmt, params: String): RustCallResult
  def queryPreparedstatement(stmt: Stmt, params: String): RustCallResult
}

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

case class RustError(code: String, message: String)

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

object RustJnaImpl extends RustBinding {
  type Conn = RustConnectionJna
  type Stmt = RustPreparedStatementJna

  val currentDir = System.getProperty("user.dir")

  System.setProperty("jna.debug_load.jna", "true")
  System.setProperty("jna.debug_load", "true")

  val library = Native.loadLibrary("jdbc_native", classOf[JnaRustBridge])

  override def prepareStatement(connection: RustConnectionJna, query: String): RustPreparedStatementJna = {
    val ptrAndErr = library.prepareStatement(connection.conn, query)
    println(s"[Jna] Prepare result: ${ptrAndErr.error}")
    RustCallResult.fromString(ptrAndErr.error)
    new RustPreparedStatementJna(ptrAndErr.pointer)
  }

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

  override def executePreparedstatement(stmt: RustPreparedStatementJna, params: String): RustCallResult = {
    println(s"[JNA] PreparedStatement: Executing with params: $params")
    val result = library.executePreparedstatement(stmt.stmt, params)
    println(s"[JNA] Result: $result")
    RustCallResult.fromString(result)
  }

  override def queryPreparedstatement(stmt: RustPreparedStatementJna, params: String): RustCallResult = {
    println(s"[JNA] PreparedStatement: Querying with params: $params")
    val result = library.queryPreparedstatement(stmt.stmt, params)
    println(s"[JNA] Result: $result")
    RustCallResult.fromString(result)
  }
}
