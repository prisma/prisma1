package com.prisma.native_jdbc

import java.sql.{BatchUpdateException, SQLException}

import com.prisma.native_jdbc.graalvm.CIntegration
import com.sun.jna.{Native, Pointer}
import org.graalvm.nativeimage.c.`type`.{CCharPointer, CTypeConversion}
import play.api.libs.json.{JsArray, Json}

import scala.util.Try

sealed trait RustConnection
class RustConnectionGraal(val conn: CIntegration.RustConnection) extends RustConnection
class RustConnectionJna(val conn: Pointer)                       extends RustConnection

sealed trait RustPreparedStatement
class RustPreparedStatementJna(val stmt: Pointer) extends RustPreparedStatement

trait RustBinding {
  type Conn <: RustConnection
  type Stmt <: RustPreparedStatement

  def newConnection(url: String): Conn
  def prepareStatement(connection: Conn, query: String): Stmt
  def closeStatement(stmt: Stmt): RustCallResult
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

object RustGraalBinding extends RustBinding {
  def toJavaString(str: CCharPointer) = CTypeConversion.toJavaString(str)
  def toCString(str: String)          = CTypeConversion.toCString(str).get()

  override type Conn = RustConnectionGraal
  override type Stmt = this.type

  override def newConnection(url: String): RustGraalImpl = ???

  override def prepareStatement(connection: RustGraalImpl, query: String): RustGraalImpl = ???

  override def closeStatement(stmt: RustGraalImpl): RustCallResult = ???

  override def startTransaction(connection: RustGraalImpl): RustCallResult = ???

  override def commitTransaction(connection: RustGraalImpl): RustCallResult = ???

  override def rollbackTransaction(connection: RustGraalImpl): RustCallResult = ???

  override def closeConnection(connection: RustGraalImpl): RustCallResult = ???

  override def sqlExecute(connection: RustGraalImpl, query: String, params: String): RustCallResult = ???

  override def sqlQuery(connection: RustGraalImpl, query: String, params: String): RustCallResult = ???

  override def executePreparedstatement(stmt: RustGraalImpl, params: String): RustCallResult = ???

  override def queryPreparedstatement(stmt: RustGraalImpl, params: String): RustCallResult = ???
}

object RustJnaBinding extends RustBinding {
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
    println("[Jna] Connecting...")
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

  override def closeStatement(stmt: RustPreparedStatementJna): RustCallResult = {
    RustCallResult.fromString(library.closeStatement(stmt.stmt))
  }
}
