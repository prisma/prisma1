package com.prisma.api.connector.jdbc.extensions

import java.sql.{PreparedStatement, ResultSet, Timestamp}
import java.time.{LocalDateTime, ZoneOffset}
import java.util.{Calendar, Date, TimeZone}

import com.prisma.gc_values._
import com.prisma.shared.models.{Field, Model, RelationSide, TypeIdentifier}
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json.Json
import java.sql.Timestamp
import org.joda.time.LocalDateTime

trait JdbcExtensions {
  import JdbcExtensionsValueClasses._

  def currentSqlTimestampUTC: Timestamp = jodaDateTimeToSqlTimestampUTC(DateTime.now(DateTimeZone.UTC))

  implicit def preparedStatementExtensions(ps: PreparedStatement): PreparedStatementExtensions = new PreparedStatementExtensions(ps)
  implicit def resultSetExtensions(resultSet: ResultSet): ResultSetExtensions                  = new ResultSetExtensions(resultSet)

}

object JdbcExtensionsValueClasses {
  def jodaDateTimeToSqlTimestampUTC(dateTime: DateTime): Timestamp =
    Timestamp.valueOf(java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(dateTime.getMillis), ZoneOffset.UTC))

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

    def getParentId(side: RelationSide.Value, typeIdentifier: TypeIdentifier.Value): IdGCValue = {
      val gcValue = getGcValue("__Relation__" + side.toString, typeIdentifier)
      gcValue.asInstanceOf[IdGCValue]
    }

    def getGcValue(name: String, typeIdentifier: TypeIdentifier.Value): GCValue = {
      val calendar: java.util.Calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

      val gcValue: GCValue = typeIdentifier match {
        case TypeIdentifier.String  => StringGCValue(resultSet.getString(name))
        case TypeIdentifier.Cuid    => CuidGCValue(resultSet.getString(name))
        case TypeIdentifier.UUID    => UuidGCValue.parse_!(resultSet.getString(name))
        case TypeIdentifier.Enum    => EnumGCValue(resultSet.getString(name))
        case TypeIdentifier.Int     => IntGCValue(resultSet.getInt(name))
        case TypeIdentifier.Float   => FloatGCValue(resultSet.getDouble(name))
        case TypeIdentifier.Boolean => BooleanGCValue(resultSet.getBoolean(name))
        case TypeIdentifier.DateTime =>
          val sqlType = resultSet.getTimestamp(name, calendar)
          if (sqlType != null) {
            DateTimeGCValue(new DateTime(sqlType.getTime))
          } else {
            NullGCValue
          }
        case TypeIdentifier.Json =>
          val sqlType = resultSet.getString(name)
          if (sqlType != null) {
            JsonGCValue(Json.parse(sqlType))
          } else {
            NullGCValue
          }
        case TypeIdentifier.Relation => sys.error("TypeIdentifier.Relation is not supported here")
      }
      if (resultSet.wasNull) { // todo: should we throw here if the field is required but it is null?
        NullGCValue
      } else {
        gcValue
      }
    }
  }
}
