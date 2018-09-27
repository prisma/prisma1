package com.prisma.native_jdbc

import com.sun.jna.{Native, Pointer}

sealed trait RustConnection
//class RustConnectionGraal(val conn: CIntegration.RustConnection) extends RustConnection
class RustConnectionJna(val conn: Pointer) extends RustConnection

trait RustBinding[T <: RustConnection] {
  def newConnection(url: String): T
  def startTransaction(connection: T): Unit
  def commitTransaction(connection: T): Unit
  def rollbackTransaction(connection: T): Unit
  def closeConnection(connection: T): Unit
  def sqlExecute(connection: T, query: String, params: String): Unit
  def sqlQuery(connection: T, query: String, params: String): String
}

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
//  System.setProperty("jna.boot.library.path", s"$currentDir/jnalib/")
  System.setProperty("jna.debug_load", "true")
//  System.setProperty("jna.library.path", s"$currentDir/hello-rs/target/debug")
  val library = Native.loadLibrary("jdbc_native", classOf[JnaRustBridge])
  override def newConnection(url: String): RustConnectionJna = {
    new RustConnectionJna(library.newConnection(url))
  }

  override def startTransaction(connection: RustConnectionJna): Unit    = library.startTransaction(connection.conn)
  override def commitTransaction(connection: RustConnectionJna): Unit   = library.commitTransaction(connection.conn)
  override def rollbackTransaction(connection: RustConnectionJna): Unit = library.rollbackTransaction(connection.conn)
  override def closeConnection(connection: RustConnectionJna): Unit     = library.closeConnection(connection.conn)
  override def sqlExecute(connection: RustConnectionJna, query: String, params: String): Unit = {
    println(s"[JNA] Execute: '$query' with params: $params")
    library.sqlExecute(connection.conn, query, params)
  }

  override def sqlQuery(connection: RustConnectionJna, query: String, params: String): String = {
    println(s"[JNA] Query: '$query' with params: $params")
    val result = library.sqlQuery(connection.conn, query, params)
    println(s"[JNA] Result: $result")
    result
  }
}
