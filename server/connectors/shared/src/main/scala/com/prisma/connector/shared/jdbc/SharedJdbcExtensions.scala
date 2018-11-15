package com.prisma.connector.shared.jdbc

import java.sql.Timestamp
import java.time.ZoneOffset

import org.joda.time.{DateTime, DateTimeZone}

trait SharedJdbcExtensions {
  def currentSqlTimestampUTC: Timestamp = jodaDateTimeToSqlTimestampUTC(DateTime.now(DateTimeZone.UTC))

  def jodaDateTimeToSqlTimestampUTC(dateTime: DateTime): Timestamp =
    Timestamp.valueOf(java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(dateTime.getMillis), ZoneOffset.UTC))

  def sqlTimestampToDateTime(ts: Timestamp): DateTime = {
    new DateTime(ts, DateTimeZone.UTC)
  }
}
