package com.prisma.native_jdbc

import java.sql.Driver
import java.util.Properties

object CustomJdbcDriver {
  def jna(): CustomJdbcDriver = new CustomJdbcDriver(RustJnaImpl.asInstanceOf[RustBinding[RustConnection, RustPreparedStatement]])
//  def graal
}

case class CustomJdbcDriver(binding: RustBinding[RustConnection, RustPreparedStatement]) extends Driver {
  override def getParentLogger = ???

  override def getMajorVersion                                = 1
  override def getMinorVersion                                = 0
  override def jdbcCompliant()                                = false
  override def acceptsURL(url: String)                        = true
  override def getPropertyInfo(url: String, info: Properties) = Array.empty

  override def connect(url: String, info: Properties) = {
    val props  = org.postgresql.Driver.parseURL(url, new Properties())
    val dbName = props.getProperty("PGDBNAME")
    val port   = props.getProperty("PGPORT").toInt
    val host   = props.getProperty("PGHOST")
    val user   = info.getProperty("user")
    val pass   = info.getProperty("password")

    new CustomJdbcConnection(s"postgres://$user:$pass@$host:$port/$dbName", binding)
  }
}
