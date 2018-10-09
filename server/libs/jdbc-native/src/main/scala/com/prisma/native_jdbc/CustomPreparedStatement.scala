package com.prisma.native_jdbc

import java.io.{InputStream, Reader}
import java.net.URL
import java.sql
import java.sql.{Blob, Clob, Date, NClob, PreparedStatement, Ref, ResultSet, RowId, SQLException, SQLXML, Time, Timestamp}
import java.util.Calendar

import org.postgresql.core.Parser
import org.postgresql.util.PSQLState
import play.api.libs.json.{JsArray, JsNull, JsValue, Json}

import scala.collection.mutable

object CustomPreparedStatement {
  type Params = mutable.HashMap[Int, JsValue]
}

class CustomPreparedStatement(conn: RustConnection, query: String, binding: RustBinding[RustConnection, RustPreparedStatement]) extends PreparedStatement {
  import CustomPreparedStatement._

  val standardConformingStrings  = true
  val withParameters             = true
  val splitStatements            = true
  val isBatchedReWriteConfigured = false
  val rawSqlString               = Parser.parseJdbcSql(query, standardConformingStrings, withParameters, splitStatements, isBatchedReWriteConfigured).get(0).nativeSql
  val stmt = binding.prepareStatement(conn match {
    case x: RustConnectionJna => x
    case x                    => sys.error(s"Got: $x, required RustConnectionJna")
  }, rawSqlString)

  val currentParams = new Params
  val paramList     = mutable.ArrayBuffer.empty[Params]

  // WIP, just to make it work again for working tree
  def renderParams(ary: Boolean): String = {
//    if (paramList.nonEmpty) {
//      JsArray(paramList.map(x => JsArray(x.toSeq.sortBy(_._1).map(_._2)).toString()))
//    } else {
//      JsArray(currentParams.toSeq.sortBy(_._1).map(_._2)).toString()
//    }

    if (ary) {
      JsArray(Seq(JsArray(currentParams.toSeq.sortBy(_._1).map(_._2)))).toString
    } else {
      JsArray(currentParams.toSeq.sortBy(_._1).map(_._2)).toString()
    }

  }

  override def execute() = {
    val result = binding.executePreparedstatement(
      stmt,
      renderParams(true)
    )

    result.isResultSet
  }

  override def executeQuery(): ResultSet = {
    val result = binding.queryPreparedstatement(
      stmt,
      renderParams(false)
    )

    if (!result.isResultSet) {
      throw new SQLException("No results were returned by the query.", PSQLState.NO_DATA.toString)
    }

    result.toResultSet
  }

  override def executeUpdate() = {
    val result = binding.executePreparedstatement(
      stmt,
      renderParams(true)
    )

    if (!result.isCount) {
      throw new SQLException(s"No count was returned by the update. $result", PSQLState.TOO_MANY_RESULTS.toString)
    }

    result.count.get
  }

  override def addBatch() = {}

  override def getGeneratedKeys: ResultSet = JsonResultSet(RustResultSet.empty)

  override def setShort(parameterIndex: Int, x: Short) = ???

  override def setObject(parameterIndex: Int, x: scala.Any, targetSqlType: Int) = ???

  override def setObject(parameterIndex: Int, x: scala.Any) = {
    ???
  }

  override def setObject(parameterIndex: Int, x: scala.Any, targetSqlType: Int, scaleOrLength: Int) = ???

  override def setDouble(parameterIndex: Int, x: Double) = {
    currentParams.put(parameterIndex, Json.obj("discriminator" -> "Double", "value" -> x))
  }

  override def setNClob(parameterIndex: Int, value: NClob) = ???

  override def setNClob(parameterIndex: Int, reader: Reader, length: Long) = ???

  override def setNClob(parameterIndex: Int, reader: Reader) = ???

  override def getParameterMetaData = ???

  override def setTime(parameterIndex: Int, x: Time) = ???

  override def setTime(parameterIndex: Int, x: Time, cal: Calendar) = ???

  override def setUnicodeStream(parameterIndex: Int, x: InputStream, length: Int) = ???

  override def setArray(parameterIndex: Int, x: sql.Array) = ???

  override def setURL(parameterIndex: Int, x: URL) = ???

  override def setInt(parameterIndex: Int, x: Int): Unit = {
    currentParams.put(parameterIndex, Json.obj("discriminator" -> "Int", "value" -> x))
  }

  override def setString(parameterIndex: Int, x: String) = {
    currentParams.put(parameterIndex, Json.obj("discriminator" -> "String", "value" -> x))
  }

  override def setLong(parameterIndex: Int, x: Long) = {
    currentParams.put(parameterIndex, Json.obj("discriminator" -> "Long", "value" -> x))
  }

  override def setAsciiStream(parameterIndex: Int, x: InputStream, length: Int) = ???

  override def setAsciiStream(parameterIndex: Int, x: InputStream, length: Long) = ???

  override def setAsciiStream(parameterIndex: Int, x: InputStream) = ???

  override def setClob(parameterIndex: Int, x: Clob) = ???

  override def setClob(parameterIndex: Int, reader: Reader, length: Long) = ???

  override def setClob(parameterIndex: Int, reader: Reader) = ???

  override def setBinaryStream(parameterIndex: Int, x: InputStream, length: Int) = ???

  override def setBinaryStream(parameterIndex: Int, x: InputStream, length: Long) = ???

  override def setBinaryStream(parameterIndex: Int, x: InputStream) = ???

  override def setNString(parameterIndex: Int, value: String) = ???

  override def getMetaData = ???

  override def setByte(parameterIndex: Int, x: Byte) = ???

  override def setNull(parameterIndex: Int, sqlType: Int) = {
    currentParams.put(parameterIndex, Json.obj("discriminator" -> "Null", "value" -> JsNull))
  }

  override def setNull(parameterIndex: Int, sqlType: Int, typeName: String) = ???

  override def setSQLXML(parameterIndex: Int, xmlObject: SQLXML) = ???

  override def setNCharacterStream(parameterIndex: Int, value: Reader, length: Long) = ???

  override def setNCharacterStream(parameterIndex: Int, value: Reader) = ???

  override def setCharacterStream(parameterIndex: Int, reader: Reader, length: Int) = ???

  override def setCharacterStream(parameterIndex: Int, reader: Reader, length: Long) = ???

  override def setCharacterStream(parameterIndex: Int, reader: Reader) = ???

  override def setRef(parameterIndex: Int, x: Ref) = ???

  override def setBlob(parameterIndex: Int, x: Blob) = ???

  override def setBlob(parameterIndex: Int, inputStream: InputStream, length: Long) = ???

  override def setBlob(parameterIndex: Int, inputStream: InputStream) = ???

  override def setTimestamp(parameterIndex: Int, x: Timestamp) = {
    currentParams.put(parameterIndex, Json.obj("discriminator" -> "DateTime", "value" -> x.toInstant.toEpochMilli))
  }

  override def setTimestamp(parameterIndex: Int, x: Timestamp, cal: Calendar) = {
    currentParams.put(parameterIndex, Json.obj("discriminator" -> "DateTime", "value" -> x.toInstant.toEpochMilli))
  }

  override def setBytes(parameterIndex: Int, x: Array[Byte]) = ???

  override def setBigDecimal(parameterIndex: Int, x: java.math.BigDecimal) = ???

  override def setFloat(parameterIndex: Int, x: Float) = ???

  override def setRowId(parameterIndex: Int, x: RowId) = ???

  override def setDate(parameterIndex: Int, x: Date) = ???

  override def setDate(parameterIndex: Int, x: Date, cal: Calendar) = ???

  override def clearParameters() = ???

  override def setBoolean(parameterIndex: Int, x: Boolean) = {
    currentParams.put(parameterIndex, Json.obj("discriminator" -> "Boolean", "value" -> x))
  }

  override def cancel() = ???

  override def getResultSetHoldability = ???

  override def getMaxFieldSize = ???

  override def getUpdateCount = ???

  override def setPoolable(poolable: Boolean) = ???

  override def getFetchSize = ???

  override def setQueryTimeout(seconds: Int) = ???

  override def setFetchDirection(direction: Int) = ???

  override def setMaxRows(max: Int) = ???

  override def setCursorName(name: String) = ???

  override def getFetchDirection = ???

  override def getResultSetType = ???

  override def getMoreResults = ???

  override def getMoreResults(current: Int) = ???

  override def addBatch(sql: String) = ???

  override def execute(sql: String) = ???

  override def execute(sql: String, autoGeneratedKeys: Int) = ???

  override def execute(sql: String, columnIndexes: Array[Int]) = ???

  override def execute(sql: String, columnNames: Array[String]) = ???

  override def executeQuery(sql: String) = ???

  override def isCloseOnCompletion = ???

  override def getResultSet = ???

  override def getMaxRows = ???

  override def setEscapeProcessing(enable: Boolean) = ???

  override def executeUpdate(sql: String) = ???

  override def executeUpdate(sql: String, autoGeneratedKeys: Int) = ???

  override def executeUpdate(sql: String, columnIndexes: Array[Int]) = ???

  override def executeUpdate(sql: String, columnNames: Array[String]) = ???

  override def getQueryTimeout = ???

  override def getWarnings = ???

  override def getConnection = ???

  override def setMaxFieldSize(max: Int) = ???

  override def isPoolable = ???

  override def clearBatch() = ???

  override def close() = ???

  override def closeOnCompletion() = ???

  override def executeBatch() = ???

  override def setFetchSize(rows: Int) = ???

  override def clearWarnings() = ???

  override def getResultSetConcurrency = ???

  override def isClosed = ???

  override def unwrap[T](iface: Class[T]) = ???

  override def isWrapperFor(iface: Class[_]) = ???
}
