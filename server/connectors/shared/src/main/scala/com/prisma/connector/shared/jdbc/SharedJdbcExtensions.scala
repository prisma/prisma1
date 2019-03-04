package com.prisma.connector.shared.jdbc

import java.sql.Timestamp
import org.joda.time.{DateTime, DateTimeZone}

trait SharedJdbcExtensions {
  def currentSqlTimestampUTC: Timestamp                            = jodaDateTimeToSqlTimestampUTC(DateTime.now(DateTimeZone.UTC))
  def jodaDateTimeToSqlTimestampUTC(dateTime: DateTime): Timestamp = new Timestamp(dateTime.getMillis)
  def sqlTimestampToDateTime(ts: Timestamp): DateTime              = new DateTime(ts, DateTimeZone.UTC)
}
