package com.prisma.native_jdbc

import java.sql.ResultSetMetaData

case class RustResultSetMetaData(rs: RustResultSet) extends ResultSetMetaData {
  override def getColumnCount: Int = rs.columns.length

  override def isAutoIncrement(column: Int): Boolean = ???

  override def isCaseSensitive(column: Int): Boolean = ???

  override def isSearchable(column: Int): Boolean = ???

  override def isCurrency(column: Int): Boolean = ???

  override def isNullable(column: Int): Int = ???

  override def isSigned(column: Int): Boolean = ???

  override def getColumnDisplaySize(column: Int): Int = ???

  override def getColumnLabel(column: Int): String = rs.columns(column - 1).name

  override def getColumnName(column: Int): String = rs.columns(column - 1).name

  override def getSchemaName(column: Int): String = ???

  override def getPrecision(column: Int): Int = ???

  override def getScale(column: Int): Int = ???

  override def getTableName(column: Int): String = ???

  override def getCatalogName(column: Int): String = ???

  override def getColumnType(column: Int): Int = ???

  override def getColumnTypeName(column: Int): String = ???

  override def isReadOnly(column: Int): Boolean = ???

  override def isWritable(column: Int): Boolean = ???

  override def isDefinitelyWritable(column: Int): Boolean = ???

  override def getColumnClassName(column: Int): String = ???

  override def unwrap[T](iface: Class[T]): T = ???

  override def isWrapperFor(iface: Class[_]): Boolean = ???
}
