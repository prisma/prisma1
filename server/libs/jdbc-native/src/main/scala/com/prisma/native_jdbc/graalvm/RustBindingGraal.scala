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

  override def newConnection(url: String): RustConnectionGraal = {
    new RustConnectionGraal(RustInterfaceGraal.newConnection(toCString(url)))
  }

  override def prepareStatement(connection: RustConnectionGraal, query: String): RustPreparedStatementGraal = {
    val ptrAndErr: CIntegration.PointerAndError = RustInterfaceGraal.prepareStatement(connection.conn, toCString(query))
    println(s"[Graal] Prepare result: ${ptrAndErr.error}")
    RustCallResult.fromString(toJavaString(ptrAndErr.error))
    new RustPreparedStatementGraal(ptrAndErr.pointer.asInstanceOf[CIntegration.RustStatement])
  }

  override def closeStatement(stmt: RustPreparedStatementGraal): RustCallResult = {
    RustCallResult.fromString(toJavaString(RustInterfaceGraal.closeStatement(stmt.stmt)))
  }

  override def startTransaction(connection: RustConnectionGraal): RustCallResult = {
    RustCallResult.fromString(toJavaString(RustInterfaceGraal.startTransaction(connection.conn)))
  }

  override def commitTransaction(connection: RustConnectionGraal): RustCallResult = {
    RustCallResult.fromString(toJavaString(RustInterfaceGraal.commitTransaction(connection.conn)))
  }

  override def rollbackTransaction(connection: RustConnectionGraal): RustCallResult = {
    RustCallResult.fromString(toJavaString(RustInterfaceGraal.rollbackTransaction(connection.conn)))
  }

  override def closeConnection(connection: RustConnectionGraal): RustCallResult = {
    RustCallResult.fromString(toJavaString(RustInterfaceGraal.closeConnection(connection.conn)))
  }

  override def sqlExecute(connection: RustConnectionGraal, query: String, params: String): RustCallResult = {
    println(s"[Graal] Execute: '$query' with params: $params")
    val result = RustInterfaceGraal.sqlExecute(connection.conn, toCString(query), toCString(params))
    println(s"[Graal] Result: $result")
    RustCallResult.fromString(toJavaString(result))
  }

  override def sqlQuery(connection: RustConnectionGraal, query: String, params: String): RustCallResult = {
    println(s"[Graal] Query: '$query' with params: $params")
    val result = RustInterfaceGraal.sqlQuery(connection.conn, toCString(query), toCString(params))
    println(s"[Graal] Result: $result")
    RustCallResult.fromString(toJavaString(result))
  }

  override def executePreparedstatement(stmt: RustPreparedStatementGraal, params: String): RustCallResult = {
    println(s"[Graal] PreparedStatement: Executing with params: $params")
    val result = RustInterfaceGraal.executePreparedstatement(stmt.stmt, toCString(params))
    println(s"[Graal] Result: $result")
    RustCallResult.fromString(toJavaString(result))
  }

  override def queryPreparedstatement(stmt: RustPreparedStatementGraal, params: String): RustCallResult = {
    println(s"[Graal] PreparedStatement: Executing with params: $params")
    val result = RustInterfaceGraal.queryPreparedstatement(stmt.stmt, toCString(params))
    println(s"[Graal] Result: $result")
    RustCallResult.fromString(toJavaString(result))
  }
}
