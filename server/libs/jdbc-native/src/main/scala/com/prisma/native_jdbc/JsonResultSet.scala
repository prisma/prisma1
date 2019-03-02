package com.prisma.native_jdbc

import java.io.{InputStream, Reader}
import java.sql.{Blob, Clob, Date, NClob, Ref, ResultSet, RowId, SQLXML, Time, Timestamp}
import java.util.{Calendar, TimeZone}
import java.{lang, sql, util}

import com.prisma.native_jdbc.CustomPreparedStatement._
import org.joda.time.DateTime
import play.api.libs.json._

case class JsonResultSet(rustResultSet: RustResultSet) extends ResultSet with DefaultReads {
  import DefaultValues._
  private var cursor: Int    = -1
  private var lastColWasNull = false

  override def next(): Boolean = {
    cursor += 1
    cursor < rustResultSet.data.size
  }

  /**
    * MISC METHODS
    */
  override def isFirst                           = ???
  override def isLast                            = ???
  override def wasNull()                         = lastColWasNull
  override def beforeFirst()                     = ???
  override def afterLast()                       = ???
  override def refreshRow()                      = ???
  override def getMetaData                       = RustResultSetMetaData(rustResultSet)
  override def getRow                            = ???
  override def getType                           = ???
  override def relative(rows: Int)               = ???
  override def getWarnings                       = ???
  override def close()                           = ???
  override def moveToCurrentRow()                = ???
  override def setFetchSize(rows: Int)           = ???
  override def clearWarnings()                   = ???
  override def insertRow()                       = ???
  override def isClosed                          = ???
  override def isAfterLast                       = ???
  override def rowDeleted()                      = ???
  override def absolute(row: Int)                = ???
  override def first()                           = ???
  override def getCursorName                     = ???
  override def getHoldability                    = ???
  override def getFetchSize                      = ???
  override def getConcurrency                    = ???
  override def setFetchDirection(direction: Int) = ???
  override def cancelRowUpdates()                = ???
  override def getStatement                      = ???
  override def getFetchDirection                 = ???
  override def last()                            = ???
  override def isBeforeFirst                     = ???
  override def deleteRow()                       = ???
  override def rowInserted()                     = ???
  override def previous()                        = ???
  override def rowUpdated()                      = ???
  override def moveToInsertRow()                 = ???
  override def unwrap[T](iface: Class[T])        = ???
  override def isWrapperFor(iface: Class[_])     = ???

  /**
    * DATA RETRIEVAL METHODS
    */
  override def getDouble(columnIndex: Int)              = readColumnAs[Double](columnIndex)
  override def getDouble(columnLabel: String)           = readColumnAs[Double](columnLabel)
  override def getString(columnIndex: Int)              = readColumnAs[String](columnIndex)
  override def getString(columnLabel: String)           = readColumnAs[String](columnLabel)
  override def getBoolean(columnIndex: Int): Boolean    = readColumnAs[Boolean](columnIndex)
  override def getBoolean(columnLabel: String): Boolean = readColumnAs[Boolean](columnLabel)
  override def getInt(columnIndex: Int)                 = readColumnAs[Int](columnIndex)
  override def getInt(columnLabel: String)              = readColumnAs[Int](columnLabel)
  override def getLong(columnIndex: Int)                = readColumnAs[Long](columnIndex)
  override def getLong(columnLabel: String)             = readColumnAs[Long](columnLabel)

  override def getTimestamp(columnLabel: String, cal: Calendar) = {
    val labelPos = rustResultSet.columns.indexWhere(c => c.name == columnLabel)
    if (labelPos <= -1) {
      sys.error("Column label not found")
    }

    getTimestamp(labelPos + 1, cal)
  }

  override def getTimestamp(columnIndex: Int, cal: Calendar) = {
    if (cal.getTimeZone != TimeZone.getTimeZone("UTC")) {
      sys.error("Can only handle UTC.")
    }

    getTimestamp(columnIndex)
  }

  override def getTimestamp(columnLabel: String) = {
    val labelPos = rustResultSet.columns.indexWhere(c => c.name == columnLabel)
    if (labelPos <= -1) {
      sys.error("Column label not found")
    }

    getTimestamp(labelPos + 1)
  }

  override def getTimestamp(columnIndex: Int) = {
    val readValue = readColumnAs[MagicDateTime](columnIndex)
    if (readValue == null) {
      null
    } else {
      val dt = new DateTime(
        readValue.year,
        readValue.month,
        readValue.day,
        readValue.hour,
        readValue.minute,
        readValue.seconds,
        readValue.millis
      )

      new Timestamp(dt.toInstant.getMillis)
    }
  }

  override def getObject(columnIndex: Int): java.lang.Object = {
    val column = rustResultSet.columns.lift(columnIndex - 1).getOrElse(sys.error(s"Column not found at $columnIndex"))

    column.discriminator match {
      case "String"      => getString(columnIndex)
      case "Int"         => new Integer(getInt(columnIndex))
      case "Long"        => new java.lang.Long(getLong(columnIndex))
      case "Double"      => new java.lang.Double(getDouble(columnIndex))
      case "DateTime"    => getTimestamp(columnIndex)
      case "Boolean"     => new lang.Boolean(getBoolean(columnIndex))
      case "Null"        => null
      case "VOID"        => null
      case "UUID"        => ??? //readColumnAs[UUID](columnIndex)
      case "StringArray" => getArray(columnIndex)
    }
  }

  override def getObject(columnLabel: String) = {
    ???
  }

  override def getArray(columnIndex: Int) = ???

  override def getArray(columnLabel: String) = {
    new sql.Array {
      override def getBaseTypeName: String = ???
      override def getBaseType: Int        = ???
      override def getArray: AnyRef = {
        // TODO: this only works for string arrays for now
        val jsArray = readColumnAs[JsArray](columnLabel)
        jsArray.value.map(_.toString()).toArray
      }
      override def getArray(map: util.Map[String, Class[_]]): AnyRef                                 = ???
      override def getArray(index: Long, count: Int): AnyRef                                         = ???
      override def getArray(index: Long, count: Int, map: util.Map[String, Class[_]]): AnyRef        = ???
      override def getResultSet: ResultSet                                                           = ???
      override def getResultSet(map: util.Map[String, Class[_]]): ResultSet                          = ???
      override def getResultSet(index: Long, count: Int): ResultSet                                  = ???
      override def getResultSet(index: Long, count: Int, map: util.Map[String, Class[_]]): ResultSet = ???
      override def free(): Unit                                                                      = ???
    }
  }

  private def readColumnAs[T](columnLabel: String)(implicit reads: Reads[T], default: DefaultValue[T]): T = {
    val labelPos = rustResultSet.columns.indexWhere(c => c.name == columnLabel)
    if (labelPos <= -1) {
      sys.error("Column label not found")
    }

    readColumnAs[T](labelPos + 1)
  }

  private def readColumnAs[T](index: Int)(implicit reads: Reads[T], default: DefaultValue[T]): T = {
    val row         = rustResultSet.data(cursor)
    val columnValue = row.value(index - 1)

    columnValue match {
      case JsNull => lastColWasNull = true; default.default
      case value  => lastColWasNull = false; value.validate[T].getOrElse(sys.error(s"received incompatible value $value"))
    }
  }

  override def getByte(columnIndex: Int) = ???

  override def getByte(columnLabel: String) = ???

  override def getBigDecimal(columnIndex: Int, scale: Int) = ???

  override def getBigDecimal(columnLabel: String, scale: Int) = ???

  override def getBigDecimal(columnIndex: Int) = ???

  override def getBigDecimal(columnLabel: String) = ???

  override def getClob(columnIndex: Int) = ???

  override def getClob(columnLabel: String) = ???

  override def getAsciiStream(columnIndex: Int) = ???

  override def getAsciiStream(columnLabel: String) = ???

  override def getNString(columnIndex: Int) = ???

  override def getNString(columnLabel: String) = ???

  override def getNClob(columnIndex: Int) = ???

  override def getNClob(columnLabel: String) = ???

  override def getObject(columnIndex: Int, map: java.util.Map[String, Class[_]]) = ???

  override def getObject(columnLabel: String, map: java.util.Map[String, Class[_]]) = ???

  override def getObject[T](columnIndex: Int, `type`: Class[T]) = ???

  override def getObject[T](columnLabel: String, `type`: Class[T]) = ???

  override def getNCharacterStream(columnIndex: Int) = ???

  override def getNCharacterStream(columnLabel: String) = ???

  override def getDate(columnIndex: Int) = ???

  override def getDate(columnLabel: String) = ???

  override def getDate(columnIndex: Int, cal: Calendar) = ???

  override def getDate(columnLabel: String, cal: Calendar) = ???

  override def getCharacterStream(columnIndex: Int) = ???

  override def getCharacterStream(columnLabel: String) = ???

  override def getBlob(columnIndex: Int) = ???

  override def getBlob(columnLabel: String) = ???

  override def getUnicodeStream(columnIndex: Int) = ???

  override def getUnicodeStream(columnLabel: String) = ???

  override def getBytes(columnIndex: Int) = ???

  override def getBytes(columnLabel: String) = ???

  override def getURL(columnIndex: Int) = ???

  override def getURL(columnLabel: String) = ???

  override def getSQLXML(columnIndex: Int) = ???

  override def getSQLXML(columnLabel: String) = ???

  override def getTime(columnIndex: Int) = ???

  override def getTime(columnLabel: String) = ???

  override def getTime(columnIndex: Int, cal: Calendar) = ???

  override def getTime(columnLabel: String, cal: Calendar) = ???

  override def getBinaryStream(columnIndex: Int) = ???

  override def getBinaryStream(columnLabel: String) = ???

  override def getRowId(columnIndex: Int) = ???

  override def getRowId(columnLabel: String) = ???

  override def findColumn(columnLabel: String) = ???

  override def getFloat(columnIndex: Int) = ???

  override def getFloat(columnLabel: String) = ???

  override def getRef(columnIndex: Int) = ???

  override def getRef(columnLabel: String) = ???

  override def getShort(columnIndex: Int) = ???

  override def getShort(columnLabel: String) = ???

  /**
    * Intentionally unimplemented because we don't need it
    */
  override def updateShort(columnIndex: Int, x: Short)                                   = ???
  override def updateShort(columnLabel: String, x: Short)                                = ???
  override def updateBytes(columnIndex: Int, x: Array[Byte])                             = ???
  override def updateBytes(columnLabel: String, x: Array[Byte])                          = ???
  override def updateNString(columnIndex: Int, nString: String)                          = ???
  override def updateNString(columnLabel: String, nString: String)                       = ???
  override def updateDouble(columnIndex: Int, x: Double)                                 = ???
  override def updateDouble(columnLabel: String, x: Double)                              = ???
  override def updateNCharacterStream(columnIndex: Int, x: Reader, length: Long)         = ???
  override def updateNCharacterStream(columnLabel: String, reader: Reader, length: Long) = ???
  override def updateNCharacterStream(columnIndex: Int, x: Reader)                       = ???
  override def updateNCharacterStream(columnLabel: String, reader: Reader)               = ???
  override def updateCharacterStream(columnIndex: Int, x: Reader, length: Int)           = ???
  override def updateCharacterStream(columnLabel: String, reader: Reader, length: Int)   = ???
  override def updateCharacterStream(columnIndex: Int, x: Reader, length: Long)          = ???
  override def updateCharacterStream(columnLabel: String, reader: Reader, length: Long)  = ???
  override def updateCharacterStream(columnIndex: Int, x: Reader)                        = ???
  override def updateCharacterStream(columnLabel: String, reader: Reader)                = ???
  override def updateBinaryStream(columnIndex: Int, x: InputStream, length: Int)         = ???
  override def updateBinaryStream(columnLabel: String, x: InputStream, length: Int)      = ???
  override def updateBinaryStream(columnIndex: Int, x: InputStream, length: Long)        = ???
  override def updateBinaryStream(columnLabel: String, x: InputStream, length: Long)     = ???
  override def updateBinaryStream(columnIndex: Int, x: InputStream)                      = ???
  override def updateBinaryStream(columnLabel: String, x: InputStream)                   = ???
  override def updateRef(columnIndex: Int, x: Ref)                                       = ???
  override def updateRef(columnLabel: String, x: Ref)                                    = ???
  override def updateString(columnIndex: Int, x: String)                                 = ???
  override def updateString(columnLabel: String, x: String)                              = ???
  override def updateInt(columnIndex: Int, x: Int)                                       = ???
  override def updateInt(columnLabel: String, x: Int)                                    = ???
  override def updateLong(columnIndex: Int, x: Long)                                     = ???
  override def updateLong(columnLabel: String, x: Long)                                  = ???
  override def updateAsciiStream(columnIndex: Int, x: InputStream, length: Int)          = ???
  override def updateAsciiStream(columnLabel: String, x: InputStream, length: Int)       = ???
  override def updateAsciiStream(columnIndex: Int, x: InputStream, length: Long)         = ???
  override def updateAsciiStream(columnLabel: String, x: InputStream, length: Long)      = ???
  override def updateAsciiStream(columnIndex: Int, x: InputStream)                       = ???
  override def updateAsciiStream(columnLabel: String, x: InputStream)                    = ???
  override def updateNull(columnIndex: Int)                                              = ???
  override def updateNull(columnLabel: String)                                           = ???
  override def updateBoolean(columnIndex: Int, x: Boolean)                               = ???
  override def updateBoolean(columnLabel: String, x: Boolean)                            = ???
  override def updateBigDecimal(columnIndex: Int, x: java.math.BigDecimal)               = ???
  override def updateBigDecimal(columnLabel: String, x: java.math.BigDecimal)            = ???
  override def updateObject(columnIndex: Int, x: scala.Any, scaleOrLength: Int)          = ???
  override def updateObject(columnIndex: Int, x: scala.Any)                              = ???
  override def updateObject(columnLabel: String, x: scala.Any, scaleOrLength: Int)       = ???
  override def updateObject(columnLabel: String, x: scala.Any)                           = ???
  override def updateBlob(columnIndex: Int, x: Blob)                                     = ???
  override def updateBlob(columnLabel: String, x: Blob)                                  = ???
  override def updateBlob(columnIndex: Int, inputStream: InputStream, length: Long)      = ???
  override def updateBlob(columnLabel: String, inputStream: InputStream, length: Long)   = ???
  override def updateBlob(columnIndex: Int, inputStream: InputStream)                    = ???
  override def updateBlob(columnLabel: String, inputStream: InputStream)                 = ???
  override def updateRowId(columnIndex: Int, x: RowId)                                   = ???
  override def updateRowId(columnLabel: String, x: RowId)                                = ???
  override def updateSQLXML(columnIndex: Int, xmlObject: SQLXML)                         = ???
  override def updateSQLXML(columnLabel: String, xmlObject: SQLXML)                      = ???
  override def updateTime(columnIndex: Int, x: Time)                                     = ???
  override def updateTime(columnLabel: String, x: Time)                                  = ???
  override def updateTimestamp(columnIndex: Int, x: Timestamp)                           = ???
  override def updateTimestamp(columnLabel: String, x: Timestamp)                        = ???
  override def updateByte(columnIndex: Int, x: Byte)                                     = ???
  override def updateByte(columnLabel: String, x: Byte)                                  = ???
  override def updateClob(columnIndex: Int, x: Clob)                                     = ???
  override def updateClob(columnLabel: String, x: Clob)                                  = ???
  override def updateClob(columnIndex: Int, reader: Reader, length: Long)                = ???
  override def updateClob(columnLabel: String, reader: Reader, length: Long)             = ???
  override def updateClob(columnIndex: Int, reader: Reader)                              = ???
  override def updateClob(columnLabel: String, reader: Reader)                           = ???
  override def updateArray(columnIndex: Int, x: sql.Array)                               = ???
  override def updateArray(columnLabel: String, x: sql.Array)                            = ???
  override def updateDate(columnIndex: Int, x: Date)                                     = ???
  override def updateDate(columnLabel: String, x: Date)                                  = ???
  override def updateFloat(columnIndex: Int, x: Float)                                   = ???
  override def updateFloat(columnLabel: String, x: Float)                                = ???
  override def updateRow()                                                               = ???
  override def updateNClob(columnIndex: Int, nClob: NClob)                               = ???
  override def updateNClob(columnLabel: String, nClob: NClob)                            = ???
  override def updateNClob(columnIndex: Int, reader: Reader, length: Long)               = ???
  override def updateNClob(columnLabel: String, reader: Reader, length: Long)            = ???
  override def updateNClob(columnIndex: Int, reader: Reader)                             = ???
  override def updateNClob(columnLabel: String, reader: Reader)                          = ???
}
