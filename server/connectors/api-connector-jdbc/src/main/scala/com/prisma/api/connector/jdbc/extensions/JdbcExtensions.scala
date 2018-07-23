package com.prisma.api.connector.jdbc.extensions

import java.sql.{PreparedStatement, ResultSet, Timestamp}
import java.time.{LocalDateTime, ZoneOffset}
import java.util.{Calendar, Date, TimeZone}

import com.prisma.gc_values._
import com.prisma.shared.models.{Field, Model, TypeIdentifier}
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json.Json

trait JdbcExtensions {
  import JdbcExtensionsValueClasses._

  def exactlyNow = {
    val today = new Date()
    new DateTime(today).withZone(DateTimeZone.UTC)
  }

  def currentSqlTimestampUTC: Timestamp = jodaDateTimeToSqlTimestampUTC(exactlyNow)
  def currentDateTimeGCValue            = DateTimeGCValue(exactlyNow)

  implicit def preparedStatementExtensions(ps: PreparedStatement): PreparedStatementExtensions = new PreparedStatementExtensions(ps)
  implicit def resultSetExtensions(resultSet: ResultSet): ResultSetExtensions                  = new ResultSetExtensions(resultSet)

}

object JdbcExtensionsValueClasses {
  val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

  def jodaDateTimeToSqlTimestampUTC(dateTime: DateTime): Timestamp = {
    val millis     = dateTime.getMillis
    val seconds    = millis / 1000
    val difference = millis - seconds * 1000
    val nanos      = difference * 1000000

    Timestamp.valueOf(LocalDateTime.ofEpochSecond(seconds, nanos.toInt, ZoneOffset.UTC))
  }

  class PreparedStatementExtensions(val ps: PreparedStatement) extends AnyVal {

    def setGcValue(index: Int, value: GCValue): Unit = {
      value match {
        case v: LeafGCValue => setLeafValue(index, v)
        case x              => sys.error(s"This method must only be called with LeafGCValues. Was called with: ${x.getClass}")
      }
    }

    private def setLeafValue(index: Int, value: LeafGCValue): Unit = {
      value match {
        case StringGCValue(string)     => ps.setString(index, string)
        case BooleanGCValue(boolean)   => ps.setBoolean(index, boolean)
        case IntGCValue(int)           => ps.setInt(index, int)
        case FloatGCValue(float)       => ps.setDouble(index, float)
        case CuidGCValue(id)           => ps.setString(index, id)
        case UuidGCValue(uuid)         => ps.setObject(index, uuid)
        case DateTimeGCValue(dateTime) => ps.setTimestamp(index, jodaDateTimeToSqlTimestampUTC(dateTime))
        case EnumGCValue(enum)         => ps.setString(index, enum)
        case JsonGCValue(json)         => ps.setString(index, json.toString)
        case NullGCValue               => ps.setNull(index, java.sql.Types.NULL)
      }
    }
  }

  class ResultSetExtensions(val resultSet: ResultSet) extends AnyVal {

    def getId(model: Model): IdGCValue                 = getAsID(model.idField_!)
    def getId(model: Model, column: String): IdGCValue = getAsID(column, model.idField_!.typeIdentifier)

    def getAsID(field: Field): IdGCValue = getAsID(field.dbName, field.typeIdentifier)

    def getAsID(column: String, typeIdentifier: TypeIdentifier.Value): IdGCValue = {
      val gcValue = getGcValue(column, typeIdentifier)
      gcValue.asInstanceOf[IdGCValue]
    }

    def getParentId(sideString: String, typeIdentifier: TypeIdentifier.Value): IdGCValue = {
      val gcValue = getGcValue(sideString, typeIdentifier)
      gcValue.asInstanceOf[IdGCValue]
    }

    def getGcValue(name: String, typeIdentifier: TypeIdentifier.Value): GCValue = {
      val gcValue = typeIdentifier match {
        case TypeIdentifier.String   => StringGCValue(resultSet.getString(name))
        case TypeIdentifier.Cuid     => CuidGCValue(resultSet.getString(name))
        case TypeIdentifier.UUID     => UuidGCValue.parse_!(resultSet.getString(name))
        case TypeIdentifier.Enum     => EnumGCValue(resultSet.getString(name))
        case TypeIdentifier.Int      => IntGCValue(resultSet.getInt(name))
        case TypeIdentifier.Float    => FloatGCValue(resultSet.getDouble(name))
        case TypeIdentifier.Boolean  => BooleanGCValue(resultSet.getBoolean(name))
        case TypeIdentifier.DateTime => getDateTimeGCValue(name)
        case TypeIdentifier.Json     => getJsonGCValue(name)
        case TypeIdentifier.Relation => sys.error("TypeIdentifier.Relation is not supported here")
      }

      if (resultSet.wasNull) NullGCValue else gcValue
    }

    private def getDateTimeGCValue(name: String) = {
      val sqlType = resultSet.getTimestamp(name, calendar)
      if (sqlType != null) DateTimeGCValue(new DateTime(sqlType, DateTimeZone.UTC)) else NullGCValue
    }

    private def getJsonGCValue(name: String) = {
      val sqlType = resultSet.getString(name)
      if (sqlType != null) JsonGCValue(Json.parse(sqlType)) else NullGCValue
    }
  }
}
