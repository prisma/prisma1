package com.prisma.deploy.connector.jdbc

import com.prisma.connector.shared.jdbc.SlickDatabase
import com.prisma.deploy.connector.TelemetryInfo
import com.prisma.deploy.connector.persistence.TelemetryPersistence
import org.joda.time.DateTime
import org.jooq.conf.Settings
import org.jooq.impl.DSL
import org.jooq.impl.DSL._

import scala.concurrent.{ExecutionContext, Future}

object TelemetryTable {
  val telemetryTableName = "TelemetryInfo"
  val t                  = table(name(telemetryTableName))
  val id                 = field(name(telemetryTableName, "id"))
  val lastPinged         = field(name(telemetryTableName, "lastPinged"))
}

case class JdbcTelemetryPersistence(slickDatabase: SlickDatabase)(implicit ec: ExecutionContext) extends JdbcPersistenceBase with TelemetryPersistence {
  val sql                = DSL.using(slickDatabase.dialect, new Settings().withRenderFormatted(true))
  val telemetryTableName = "TelemetryInfo"
  val database           = slickDatabase.database

  override def getOrCreateInfo(): Future[TelemetryInfo] = {
    val query = sql
      .select(TelemetryTable.id, TelemetryTable.lastPinged)
      .from(TelemetryTable.t)
      .limit(DSL.inline(1))

    lazy val create = sql
      .insertInto(TelemetryTable.t)
      .columns(TelemetryTable.id, TelemetryTable.lastPinged)
      .values(DSL.inline(java.util.UUID.randomUUID().toString), DSL.inline(null.asInstanceOf[String]))
      .returning(TelemetryTable.id, TelemetryTable.lastPinged)

    database
      .run(
        queryToDBIO(query)(
          setParams = (_) => (),
          readResult = { rs =>
            if (rs.next()) {
              val ts = rs.getTimestamp("lastPinged") match {
                case null => None
                case x    => Some(sqlTimestampToDateTime(x))
              }

              Some(TelemetryInfo(rs.getString("id"), ts))
            } else {
              None
            }
          }
        ))
      .flatMap {
        case Some(t) =>
          Future.successful(t)

        case None =>
          database.run(
            insertIntoReturning(create)(
              readResult = { rs =>
                if (rs.next()) {
                  TelemetryInfo(rs.getString("id"), None)
                } else {
                  sys.error("[Telemetry] Did not receive result after inserting")
                }
              }
            ))
      }
  }

  override def updateTelemetryInfo(lastPinged: DateTime): Future[Unit] = {
    val update = sql
      .update(TelemetryTable.t)
      .set(TelemetryTable.lastPinged, DSL.inline(jodaDateTimeToSqlTimestampUTC(lastPinged)).asInstanceOf[Object])

    database.run(updateToDBIO(update)())
  }
}
