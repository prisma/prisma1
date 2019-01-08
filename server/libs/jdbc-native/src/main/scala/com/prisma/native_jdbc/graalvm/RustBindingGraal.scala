package com.prisma.native_jdbc.graalvm

import com.prisma.native_jdbc.{RustBinding, RustCallResult, RustConnection, RustPreparedStatement}
import org.graalvm.nativeimage.c.`type`.{CCharPointer, CTypeConversion}

class RustConnectionGraal(val conn: CIntegration.RustConnection)       extends RustConnection
class RustPreparedStatementGraal(val stmt: CIntegration.RustStatement) extends RustPreparedStatement

object RustBindingGraal extends RustBinding {
  def toJavaString(str: CCharPointer) = CTypeConversion.toJavaString(str)
  def toCString(str: String)          = CTypeConversion.toCString(str).get()

  override type Conn = RustConnectionGraal
  override type Stmt = RustPreparedStatementGraal

  def initialize() = {
    RustInterfaceGraal.jdbc_initialize()
    this
  }

  override def newConnection(url: String): RustConnectionGraal = {
    val conn = new RustConnectionGraal(RustInterfaceGraal.newConnection(toCString(url)))
    println(s"Connected: ${conn.hashCode()}")
    conn
  }

  override def prepareStatement(connection: RustConnectionGraal, query: String): RustPreparedStatementGraal = {
    val ptrAndErr: CIntegration.PointerAndError = RustInterfaceGraal.prepareStatement(connection.conn, toCString(query))
    if (ptrAndErr.error().isNonNull) println(s"[Graal] Prepare result: ${toJavaString(ptrAndErr.error)}")
    RustCallResult.fromString(toJavaString(ptrAndErr.error))
    val result = new RustPreparedStatementGraal(ptrAndErr.pointer.asInstanceOf[CIntegration.RustStatement])

    println(s"Prepared statement: ${result.hashCode()}")
    // The pointer will still be valid after dropping the envelope
    RustInterfaceGraal.destroy(ptrAndErr)
    result
  }

  override def closeStatement(stmt: RustPreparedStatementGraal): RustCallResult = {
    println(s"Closing statement: ${stmt.hashCode()}")
    val raw    = RustInterfaceGraal.closeStatement(stmt.stmt)
    val result = RustCallResult.fromString(toJavaString(raw))

    RustInterfaceGraal.destroy_string(raw)
    result
  }

  override def startTransaction(connection: RustConnectionGraal): RustCallResult = {
    println(s"Starting transaction")
    val raw    = RustInterfaceGraal.startTransaction(connection.conn)
    val result = RustCallResult.fromString(toJavaString(raw))

    RustInterfaceGraal.destroy_string(raw)
    result
  }

  override def commitTransaction(connection: RustConnectionGraal): RustCallResult = {
    println(s"Committing transaction")
    val raw    = RustInterfaceGraal.commitTransaction(connection.conn)
    val result = RustCallResult.fromString(toJavaString(raw))

    RustInterfaceGraal.destroy_string(raw)
    result
  }

  override def rollbackTransaction(connection: RustConnectionGraal): RustCallResult = {
    println(s"Rolling back transaction")
    val raw    = RustInterfaceGraal.rollbackTransaction(connection.conn)
    val result = RustCallResult.fromString(toJavaString(raw))

    RustInterfaceGraal.destroy_string(raw)
    result
  }

  override def closeConnection(connection: RustConnectionGraal): RustCallResult = {
    println(s"Closing connection ${connection.hashCode()}")
    val raw    = RustInterfaceGraal.closeConnection(connection.conn)
    val result = RustCallResult.fromString(toJavaString(raw))

    RustInterfaceGraal.destroy_string(raw)
    result
  }

  override def sqlExecute(connection: RustConnectionGraal, query: String, params: String): RustCallResult = {
    println(s"[Graal] Execute: '$query' with params: $params")
    val raw = RustInterfaceGraal.sqlExecute(connection.conn, toCString(query), toCString(params))
    println(s"[Graal] Result: $raw")
    val result = RustCallResult.fromString(toJavaString(raw))

    RustInterfaceGraal.destroy_string(raw)
    result
  }

  override def sqlQuery(connection: RustConnectionGraal, query: String, params: String): RustCallResult = {
    println(s"[Graal] Query: '$query' with params: $params")
    val raw = RustInterfaceGraal.sqlQuery(connection.conn, toCString(query), toCString(params))
    println(s"[Graal] Result: ${toJavaString(raw)}")
    val result = RustCallResult.fromString(toJavaString(raw))

    RustInterfaceGraal.destroy_string(raw)
    result
  }

  override def executePreparedstatement(stmt: RustPreparedStatementGraal, params: String): RustCallResult = {
    println(s"[Graal] PreparedStatement: Executing with params: $params")
    val raw = RustInterfaceGraal.executePreparedstatement(stmt.stmt, toCString(params))
    println(s"[Graal] Result: ${toJavaString(raw)}")
    val result = RustCallResult.fromString(toJavaString(raw))

    RustInterfaceGraal.destroy_string(raw)
    result
  }

  override def queryPreparedstatement(stmt: RustPreparedStatementGraal, params: String): RustCallResult = {
    println(s"[Graal] PreparedStatement: Executing with params: $params")
    val raw = RustInterfaceGraal.queryPreparedstatement(stmt.stmt, toCString(params))
    println(s"[Graal] Result: ${toJavaString(raw)}")
    val result = RustCallResult.fromString(toJavaString(raw))

    RustInterfaceGraal.destroy_string(raw)
    result
  }
}
