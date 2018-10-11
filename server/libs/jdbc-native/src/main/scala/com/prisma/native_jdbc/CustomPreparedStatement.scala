package com.prisma.native_jdbc

import java.io.{InputStream, Reader}
import java.net.URL
import java.sql
import java.sql.{Blob, Clob, Date, NClob, PreparedStatement, Ref, ResultSet, RowId, SQLException, SQLXML, Time, Timestamp}
import java.util.{Calendar, UUID}

import org.joda.time.DateTime
import org.postgresql.core.Parser
import org.postgresql.util.PSQLState
import play.api.libs.json.{JsArray, JsNull, JsValue, Json}

import scala.collection.mutable

object CustomPreparedStatement {
  type Params = mutable.HashMap[Int, JsValue]

  implicit val magicDateTimeFormat = Json.format[MagicDateTime]
  case class MagicDateTime(year: Int, month: Int, day: Int, hour: Int, minute: Int, seconds: Int, millis: Int)
}

abstract class BindingAndConnection {
  val binding: RustBinding
  val connection: binding.Conn
}

class CustomPreparedStatement(query: String, val bindingAndConnection: BindingAndConnection) extends PreparedStatement {
  import CustomPreparedStatement._
  import bindingAndConnection._

  val standardConformingStrings      = true
  val withParameters                 = true
  val splitStatements                = true
  val isBatchedReWriteConfigured     = false
  val parsedQuery                    = Parser.parseJdbcSql(query, standardConformingStrings, withParameters, splitStatements, isBatchedReWriteConfigured).get(0)
  val rawSqlString                   = parsedQuery.nativeSql
  val stmt                           = binding.prepareStatement(connection, rawSqlString)
  var currentParams                  = new Params
  val paramList                      = mutable.ArrayBuffer.empty[Params]
  var lastCallResult: RustCallResult = null

  def renderParams(asArray: Boolean): String = {
    if (paramList.nonEmpty) {
      JsArray(
        paramList.toVector.map(x => JsArray(x.toSeq.sortBy(_._1).map(_._2)))
      ).toString()
    } else if (asArray) {
      JsArray(Seq(JsArray(currentParams.toSeq.sortBy(_._1).map(_._2)))).toString()
    } else {
      JsArray(currentParams.toSeq.sortBy(_._1).map(_._2)).toString()
    }
  }

  def clearParams() = {
    currentParams = new Params
    paramList.clear()
  }

  override def execute() = {
    val result = if (parsedQuery.command.returnsRows()) {
      val params = renderParams(asArray = false)
      clearParams()

      binding.queryPreparedstatement(
        stmt,
        params
      )
    } else {
      val params = renderParams(asArray = true)
      clearParams()

      binding.executePreparedstatement(
        stmt,
        params
      )
    }

    lastCallResult = result
    result.isResultSet
  }

  override def executeQuery(): ResultSet = {
    val params = renderParams(asArray = false)
    clearParams()

    val result = binding.queryPreparedstatement(
      stmt,
      params
    )

    if (!result.isResultSet) {
      throw new SQLException("No results were returned by the query.", PSQLState.NO_DATA.toString)
    }

    lastCallResult = result
    result.toResultSet
  }

  override def executeUpdate() = {
    val params = renderParams(asArray = true)
    clearParams()

    val result = binding.executePreparedstatement(
      stmt,
      params
    )

    if (!result.isCount) {
      throw new SQLException(s"No count was returned by the update. $result", PSQLState.TOO_MANY_RESULTS.toString)
    }

    lastCallResult = result
    result.counts.head
  }

  override def executeBatch(): Array[Int] = {
    if (paramList.isEmpty) {
      return Array(0)
    }

    val params = renderParams(asArray = true)
    clearParams()

    val result = binding.executePreparedstatement(
      stmt,
      params
    )

    if (!result.isCount) {
      throw new SQLException(s"No count was returned by the update. $result", PSQLState.TOO_MANY_RESULTS.toString)
    }

    lastCallResult = result
    result.counts.toArray
  }

  override def addBatch() = {
    println("Adding batch")
    paramList += currentParams
    currentParams = new Params
  }

  override def getGeneratedKeys: ResultSet = JsonResultSet(RustResultSet.empty)

  override def setShort(parameterIndex: Int, x: Short) = ???

  override def setObject(parameterIndex: Int, x: scala.Any, targetSqlType: Int) = ???

  override def setObject(parameterIndex: Int, x: scala.Any) = {
    x match {
      case uuid: UUID => currentParams.put(parameterIndex, Json.obj("discriminator" -> "UUID", "value" -> uuid.toString))
      case _          => sys.error(s"SetObject not implemented for $x")
    }
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
    val isoDate = new DateTime(x.getTime)
    val date = MagicDateTime(
      isoDate.year().get(),
      isoDate.monthOfYear().get(),
      isoDate.dayOfMonth().get(),
      isoDate.hourOfDay().get(),
      isoDate.minuteOfHour().get(),
      isoDate.secondOfMinute().get(),
      isoDate.millisOfSecond().get()
    )

    currentParams.put(parameterIndex, Json.obj("discriminator" -> "DateTime", "value" -> Json.toJson(date)))
  }

  override def setTimestamp(parameterIndex: Int, x: Timestamp, cal: Calendar) = {
    setTimestamp(parameterIndex, x)
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

  override def getUpdateCount = lastCallResult.counts.head

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

  override def getResultSet = lastCallResult.toResultSet

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

  override def setFetchSize(rows: Int) = ???

  override def clearWarnings() = ???

  override def getResultSetConcurrency = ???

  override def isClosed = ???

  override def unwrap[T](iface: Class[T]) = ???

  override def isWrapperFor(iface: Class[_]) = ???
}
