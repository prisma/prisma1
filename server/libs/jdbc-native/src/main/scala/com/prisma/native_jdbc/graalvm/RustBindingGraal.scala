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
    new RustConnectionGraal(RustInterfaceGraal.newConnection(toCString(url)))
  }

  override def closeConnection(connection: RustConnectionGraal): RustCallResult = {
    val raw    = RustInterfaceGraal.closeConnection(connection.conn)
    val result = RustCallResult.fromString(toJavaString(raw))

    RustInterfaceGraal.destroy_string(raw)
    result
  }

  override def prepareStatement(connection: RustConnectionGraal, query: String): RustPreparedStatementGraal = {
    val ptrAndErr: CIntegration.PointerAndError = RustInterfaceGraal.prepareStatement(connection.conn, toCString(query))
    RustCallResult.fromString(toJavaString(ptrAndErr.error))

    val result = new RustPreparedStatementGraal(ptrAndErr.pointer.asInstanceOf[CIntegration.RustStatement])
    RustInterfaceGraal.destroy(ptrAndErr) // The pointer will still be valid after dropping the envelope
    result
  }

  override def closeStatement(stmt: RustPreparedStatementGraal): RustCallResult = {
    val raw    = RustInterfaceGraal.closeStatement(stmt.stmt)
    val result = RustCallResult.fromString(toJavaString(raw))

    RustInterfaceGraal.destroy_string(raw)
    result
  }

  override def startTransaction(connection: RustConnectionGraal): RustCallResult = {
    val raw    = RustInterfaceGraal.startTransaction(connection.conn)
    val result = RustCallResult.fromString(toJavaString(raw))

    RustInterfaceGraal.destroy_string(raw)
    result
  }

  override def commitTransaction(connection: RustConnectionGraal): RustCallResult = {
    val raw    = RustInterfaceGraal.commitTransaction(connection.conn)
    val result = RustCallResult.fromString(toJavaString(raw))

    RustInterfaceGraal.destroy_string(raw)
    result
  }

  override def rollbackTransaction(connection: RustConnectionGraal): RustCallResult = {
    val raw    = RustInterfaceGraal.rollbackTransaction(connection.conn)
    val result = RustCallResult.fromString(toJavaString(raw))

    RustInterfaceGraal.destroy_string(raw)
    result
  }

  override def sqlExecute(connection: RustConnectionGraal, query: String, params: String): RustCallResult = {
    val raw    = RustInterfaceGraal.sqlExecute(connection.conn, toCString(query), toCString(params))
    val result = RustCallResult.fromString(toJavaString(raw))

    RustInterfaceGraal.destroy_string(raw)
    result
  }

  override def sqlQuery(connection: RustConnectionGraal, query: String, params: String): RustCallResult = {
    val raw    = RustInterfaceGraal.sqlQuery(connection.conn, toCString(query), toCString(params))
    val result = RustCallResult.fromString(toJavaString(raw))

    RustInterfaceGraal.destroy_string(raw)
    result
  }

  override def executePreparedstatement(stmt: RustPreparedStatementGraal, params: String): RustCallResult = {
    val raw    = RustInterfaceGraal.executePreparedstatement(stmt.stmt, toCString(params))
    val result = RustCallResult.fromString(toJavaString(raw))

    RustInterfaceGraal.destroy_string(raw)
    result
  }

  override def queryPreparedstatement(stmt: RustPreparedStatementGraal, params: String): RustCallResult = {
    val raw    = RustInterfaceGraal.queryPreparedstatement(stmt.stmt, toCString(params))
    val result = RustCallResult.fromString(toJavaString(raw))

    RustInterfaceGraal.destroy_string(raw)
    result
  }
}
