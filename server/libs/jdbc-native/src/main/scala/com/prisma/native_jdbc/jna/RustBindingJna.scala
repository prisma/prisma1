package com.prisma.native_jdbc.jna

import com.prisma.native_jdbc._
import com.sun.jna.{Native, Pointer}

import scala.util.Try

class RustConnectionJna(val conn: Pointer)        extends RustConnection
class RustPreparedStatementJna(val stmt: Pointer) extends RustPreparedStatement

object RustBindingJna extends RustBinding {
  type Conn = RustConnectionJna
  type Stmt = RustPreparedStatementJna

  val currentDir = System.getProperty("user.dir")

  System.setProperty("jna.debug_load.jna", "true")
  System.setProperty("jna.debug_load", "true")

  val library = Native.loadLibrary("jdbc_native", classOf[JnaRustBridge])

  override def newConnection(url: String): RustConnectionJna = {
    new RustConnectionJna(library.newConnection(url))
  }

  override def prepareStatement(connection: RustConnectionJna, query: String): RustPreparedStatementJna = {
    val ptrAndErr: PointerAndError = library.prepareStatement(connection.conn, query)
    println(s"[Jna] Prepare result: ${ptrAndErr.error}")
    RustCallResult.fromString(ptrAndErr.error)
    val result = new RustPreparedStatementJna(ptrAndErr.pointer)

    // The pointer will still be valid after dropping the envelope
    library.destroy(ptrAndErr)
    result
  }

  override def startTransaction(connection: RustConnectionJna): RustCallResult = {
    val raw = library.startTransaction(connection.conn)
    processCallResult(raw)
  }

  override def commitTransaction(connection: RustConnectionJna): RustCallResult = {
    val raw = library.commitTransaction(connection.conn)
    processCallResult(raw)
  }

  override def rollbackTransaction(connection: RustConnectionJna): RustCallResult = {
    val raw = library.rollbackTransaction(connection.conn)
    processCallResult(raw)
  }

  override def closeConnection(connection: RustConnectionJna): RustCallResult = {
    val raw = library.closeConnection(connection.conn)
    processCallResult(raw)
  }

  override def sqlExecute(connection: RustConnectionJna, query: String, params: String): RustCallResult = {
    println(s"[JNA] Execute: '$query' with params: $params")
    val raw = library.sqlExecute(connection.conn, query, params)
    println(s"[JNA] Result: $raw")
    processCallResult(raw)
  }

  override def sqlQuery(connection: RustConnectionJna, query: String, params: String): RustCallResult = {
    println(s"[JNA] Query: '$query' with params: $params")
    val raw = library.sqlQuery(connection.conn, query, params)
    println(s"[JNA] Result: $raw")
    processCallResult(raw)
  }

  override def executePreparedstatement(stmt: RustPreparedStatementJna, params: String): RustCallResult = {
    println(s"[JNA] PreparedStatement: Executing with params: $params")
    val raw = library.executePreparedstatement(stmt.stmt, params)
    println(s"[JNA] Result: $raw")
    processCallResult(raw)
  }

  override def queryPreparedstatement(stmt: RustPreparedStatementJna, params: String): RustCallResult = {
    println(s"[JNA] PreparedStatement: Querying with params: $params")
    val raw = library.queryPreparedstatement(stmt.stmt, params)
    println(s"[JNA] Result: $raw")
    processCallResult(raw)
  }

  override def closeStatement(stmt: RustPreparedStatementJna): RustCallResult = {
    val raw = library.closeStatement(stmt.stmt)
    processCallResult(raw)
  }

  def processCallResult(raw: String) =
    (Try { RustCallResult.fromString(raw) } match {
      case x @ _ =>
        library.destroy_string(raw)
        x
    }).get
}
