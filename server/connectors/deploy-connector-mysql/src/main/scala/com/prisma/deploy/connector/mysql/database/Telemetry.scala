package com.prisma.deploy.connector.mysql.database

import com.github.tototoshi.slick.MySQLJodaSupport
import com.prisma.deploy.connector.TelemetryInfo
import org.joda.time.DateTime
import slick.dbio.Effect.{Read, Write}
import slick.jdbc.MySQLProfile.api._
import slick.sql.FixedSqlAction

class TelemetryTable(tag: Tag) extends Table[TelemetryInfo](tag, "Migration") {
  implicit val jodaMapper = MySQLJodaSupport.datetimeTypeMapper

  def id         = column[String]("id")
  def lastPinged = column[Option[DateTime]]("lastPinged")
  def *          = (id, lastPinged) <> (TelemetryInfo.tupled, TelemetryInfo.unapply)
}

object TelemetryTable {
  implicit val jodaMapper = MySQLJodaSupport.datetimeTypeMapper

  def getOrCreateInfo(): DBIOAction[TelemetryInfo, NoStream, Read with Write] = {
    Tables.Telemetry.result.headOption.flatMap {
      case Some(entry) =>
        DBIO.successful(entry)

      case None =>
        val newInfo     = TelemetryInfo(java.util.UUID.randomUUID().toString, None)
        val insertQuery = Tables.Telemetry += newInfo
        insertQuery.map(_ => newInfo)
    }
  }

  def updateInfo(lastPinged: DateTime): FixedSqlAction[Int, NoStream, Write] = {
    Tables.Telemetry.map(_.lastPinged).update(Some(lastPinged))
  }
}
