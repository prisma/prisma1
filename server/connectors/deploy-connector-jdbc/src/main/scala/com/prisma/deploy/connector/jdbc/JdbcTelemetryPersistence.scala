package com.prisma.deploy.connector.jdbc

import com.prisma.connector.shared.jdbc.SlickDatabase
import com.prisma.deploy.connector.TelemetryInfo
import com.prisma.deploy.connector.persistence.TelemetryPersistence
import org.joda.time.DateTime
import org.jooq.conf.Settings
import org.jooq.impl.DSL
import org.jooq.impl.DSL._

import scala.concurrent.{ExecutionContext, Future}

case class JdbcTelemetryPersistence(slickDatabase: SlickDatabase)(implicit ec: ExecutionContext) extends JdbcPersistenceBase with TelemetryPersistence {
  val sql                = DSL.using(slickDatabase.dialect, new Settings().withRenderFormatted(true))
  val telemetryTableName = "TelemetryInfo"
  val database           = slickDatabase.database

  override def getOrCreateInfo(): Future[TelemetryInfo] = {
    val query = sql
      .select(field(name(telemetryTableName, "id")), field(name(telemetryTableName, "lastPinged")))
      .from(table(name(telemetryTableName)))
      .limit(DSL.inline(1))

    lazy val create = sql
      .insertInto(table(name(telemetryTableName)))
      .columns(field(name(telemetryTableName, "id")), field(name(telemetryTableName, "lastPinged")))
      .values(DSL.inline(java.util.UUID.randomUUID().toString), DSL.inline(null.asInstanceOf[String]))

    val wat = database
      .run(
        queryToDBIO(query)(
          setParams = (_) => (),
          readResult = { rs =>
            if (rs.next()) {
              rs.getTimestamp("lastPinged") match {
                case null => None
                case x    => Some(TelemetryInfo(rs.getString("id"), Some(sqlTimestampToDateTime(x))))
              }
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
              setParams = (_) => (),
              readResult = { rs =>
                if (rs.next()) {
                  TelemetryInfo(rs.getString("id"), None)
                } else {
                  sys.error("[Telemetry] Did not receive result after inserting")
                }
              }
            ))
      }
    wat.onComplete(println(_))
    wat
  }

  override def updateTelemetryInfo(lastPinged: DateTime): Future[Unit] = {
    val update = sql
      .update(table(name(telemetryTableName)))
      .set(field(name(telemetryTableName, "lastPinged")), jodaDateTimeToSqlTimestampUTC(lastPinged))

    database.run(updateToDBIO(update)(setParams = (_) => ()))
  }
}
