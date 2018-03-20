package com.prisma.api.connector.mysql.database

import java.sql.{PreparedStatement, Timestamp}

import com.prisma.gc_values._
import org.joda.time.format.DateTimeFormat

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
}
