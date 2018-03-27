package com.prisma.api.connector.mysql.database

import java.sql.{PreparedStatement, ResultSet, Timestamp}

import com.prisma.gc_values._
import com.prisma.shared.models.{Field, TypeIdentifier}
import com.prisma.shared.models.TypeIdentifier.TypeIdentifier
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.Json

object JdbcExtensions {
  val dateTimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS").withZoneUTC()

  implicit class PreparedStatementExtensions(val ps: PreparedStatement) extends AnyVal {
    def setGcValue(index: Int, value: GCValue): Unit = value match {
      case gcValue: StringGCValue    => ps.setString(index, gcValue.value)
      case gcValue: BooleanGCValue   => ps.setBoolean(index, gcValue.value)
      case gcValue: IntGCValue       => ps.setInt(index, gcValue.value)
      case gcValue: FloatGCValue     => ps.setDouble(index, gcValue.value)
      case gcValue: GraphQLIdGCValue => ps.setString(index, gcValue.value)
      case gcValue: DateTimeGCValue  => ps.setTimestamp(index, new Timestamp(gcValue.value.getMillis))
      case gcValue: EnumGCValue      => ps.setString(index, gcValue.value)
      case gcValue: JsonGCValue      => ps.setString(index, gcValue.value.toString)
      case NullGCValue               => ps.setNull(index, java.sql.Types.NULL)
      case x                         => sys.error(s"This method must only be called with LeafGCValues. Was called with: ${x.getClass}")
    }
  }

  implicit class ResultSetExtensions(val resultSet: ResultSet) extends AnyVal {

    def getGcValue(name: String, typeIdentifier: TypeIdentifier.Value): GCValue = {
      val gcValue: GCValue = typeIdentifier match {
        case TypeIdentifier.String    => StringGCValue(resultSet.getString(name))
        case TypeIdentifier.GraphQLID => GraphQLIdGCValue(resultSet.getString(name))
        case TypeIdentifier.Enum      => EnumGCValue(resultSet.getString(name))
        case TypeIdentifier.Int       => IntGCValue(resultSet.getInt(name))
        case TypeIdentifier.Float     => FloatGCValue(resultSet.getDouble(name))
        case TypeIdentifier.Boolean   => BooleanGCValue(resultSet.getBoolean(name))
        case TypeIdentifier.DateTime =>
          val sqlType = resultSet.getTimestamp(name)
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
