package com.prisma.native_jdbc.jna

import com.prisma.native_jdbc._
import com.sun.jna.{Native, Pointer}

class RustConnectionJna(val conn: Pointer)        extends RustConnection
class RustPreparedStatementJna(val stmt: Pointer) extends RustPreparedStatement

object RustBindingJna extends RustBinding {
  type Conn = RustConnectionJna
  type Stmt = RustPreparedStatementJna

  val currentDir = System.getProperty("user.dir")

  System.setProperty("jna.debug_load.jna", "true")
  System.setProperty("jna.debug_load", "true")

  val library = Native.loadLibrary("jdbc_native", classOf[JnaRustBridge])

  override def prepareStatement(connection: RustConnectionJna, query: String): RustPreparedStatementJna = {
    val ptrAndErr: PointerAndError = library.prepareStatement(connection.conn, query)
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

  override def closeStatement(stmt: RustPreparedStatementJna): RustCallResult = {
    RustCallResult.fromString(library.closeStatement(stmt.stmt))
  }
}
