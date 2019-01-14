package com.prisma.native_jdbc.graalvm

import com.prisma.native_jdbc.{RustBinding, RustCallResult, RustConnection, RustPreparedStatement}
import org.graalvm.nativeimage.c.`type`.{CCharPointer, CTypeConversion}

class RustConnectionGraal(val conn: CIntegration.RustConnection)       extends RustConnection
class RustPreparedStatementGraal(val stmt: CIntegration.RustStatement) extends RustPreparedStatement

object RustBindingGraal extends RustBinding {
  def toJavaString(str: CCharPointer): String                    = CTypeConversion.toJavaString(str)
  def toCString(str: String): CTypeConversion.CCharPointerHolder = CTypeConversion.toCString(str)

  override type Conn = RustConnectionGraal
  override type Stmt = RustPreparedStatementGraal

  def initialize() = {
    RustInterfaceGraal.jdbc_initialize()
    this
  }

  // todo Same todos as in JWT native apply - resources leak in error cases. Need to be solved with appropriate try-with-resources code, which doesn't compile at the moment.

  override def newConnection(url: String): RustConnectionGraal = {
    val _url       = toCString(url)
    val connection = new RustConnectionGraal(RustInterfaceGraal.newConnection(_url.get()))

    _url.close()
    connection
  }

  override def closeConnection(connection: RustConnectionGraal): RustCallResult = {
    val raw    = RustInterfaceGraal.closeConnection(connection.conn)
    val result = RustCallResult.fromString(toJavaString(raw))

    RustInterfaceGraal.destroy_string(raw)
    result
  }

  override def prepareStatement(connection: RustConnectionGraal, query: String): RustPreparedStatementGraal = {
    val _query: CTypeConversion.CCharPointerHolder = toCString(query)
    val ptrAndErr: CIntegration.PointerAndError    = RustInterfaceGraal.prepareStatement(connection.conn, _query.get())

    _query.close()
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
    val _query  = toCString(query)
    val _params = toCString(params)
    val raw     = RustInterfaceGraal.sqlExecute(connection.conn, _query.get(), _params.get())

    _query.close()
    _params.close()

    val result = RustCallResult.fromString(toJavaString(raw))

    RustInterfaceGraal.destroy_string(raw)
    result
  }

  override def sqlQuery(connection: RustConnectionGraal, query: String, params: String): RustCallResult = {
    val _query  = toCString(query)
    val _params = toCString(params)
    val raw     = RustInterfaceGraal.sqlQuery(connection.conn, _query.get(), _params.get())

    _query.close()
    _params.close()

    val result = RustCallResult.fromString(toJavaString(raw))

    RustInterfaceGraal.destroy_string(raw)
    result
  }

  override def executePreparedstatement(stmt: RustPreparedStatementGraal, params: String): RustCallResult = {
    val _params = toCString(params)
    val raw     = RustInterfaceGraal.executePreparedstatement(stmt.stmt, _params.get())

    _params.close()
    val result = RustCallResult.fromString(toJavaString(raw))

    RustInterfaceGraal.destroy_string(raw)
    result
  }

  override def queryPreparedstatement(stmt: RustPreparedStatementGraal, params: String): RustCallResult = {
    val _params = toCString(params)
    val raw     = RustInterfaceGraal.queryPreparedstatement(stmt.stmt, _params.get())

    _params.close()
    val result = RustCallResult.fromString(toJavaString(raw))

    RustInterfaceGraal.destroy_string(raw)
    result
  }
}
