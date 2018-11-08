package com.prisma.connector.shared.jdbc

import java.sql.Timestamp
import java.time.ZoneOffset

import org.joda.time.{DateTime, DateTimeZone}

trait SharedJdbcExtensions {
  def currentSqlTimestampUTC: Timestamp = jodaDateTimeToSqlTimestampUTC(DateTime.now(DateTimeZone.UTC))

  def jodaDateTimeToSqlTimestampUTC(dateTime: DateTime): Timestamp = {
    val milis = dateTime.getMillis
    new Timestamp(milis)
  }

  def sqlTimestampToDateTime(ts: Timestamp): DateTime = {
    new DateTime(ts, DateTimeZone.UTC)
  }
}
