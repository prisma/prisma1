package com.prisma.native_jdbc

trait RustConnection
trait RustPreparedStatement

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
