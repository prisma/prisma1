package com.prisma.deploy.connector.jdbc.persistence

import com.prisma.connector.shared.jdbc.SlickDatabase
import com.prisma.deploy.connector.TelemetryInfo
import com.prisma.deploy.connector.jdbc.JdbcBase
import com.prisma.deploy.connector.persistence.TelemetryPersistence
import org.joda.time.DateTime
import org.jooq.impl.DSL
import org.jooq.impl.DSL._

import scala.concurrent.{ExecutionContext, Future}

object TelemetryTable {
  val telemetryTableName = "TelemetryInfo"
  val t                  = table(name(telemetryTableName))
  val id                 = field(name(telemetryTableName, "id"))
  val lastPinged         = field(name(telemetryTableName, "lastPinged"))
}

case class JdbcTelemetryPersistence(slickDatabase: SlickDatabase)(implicit ec: ExecutionContext) extends JdbcBase with TelemetryPersistence {
  override def getOrCreateInfo(): Future[TelemetryInfo] = {
    val query = sql
      .select(TelemetryTable.id, TelemetryTable.lastPinged)
      .from(TelemetryTable.t)
      .limit(DSL.inline(1))

    lazy val uuid = java.util.UUID.randomUUID().toString
    lazy val create = sql
      .insertInto(TelemetryTable.t)
      .columns(TelemetryTable.id, TelemetryTable.lastPinged)
      .values(placeHolder, DSL.inline(null.asInstanceOf[String]))

    database
      .run(
        queryToDBIO(query)(
          readResult = { rs =>
            if (rs.next()) {
              val ts = rs.getTimestamp(TelemetryTable.lastPinged.getName) match {
                case null => None
                case x    => Some(sqlTimestampToDateTime(x))
              }

              Some(TelemetryInfo(rs.getString(TelemetryTable.id.getName), ts))
            } else {
              None
            }
          }
        ))
      .flatMap {
        case Some(t) =>
          Future.successful(t)

        case None =>
          database
            .run(insertToDBIO(create)(
              setParams = { pp =>
                pp.setString(uuid)
              }
            ))
            .map { _ =>
              TelemetryInfo(uuid, None)
            }

      }
  }

  override def updateTelemetryInfo(lastPinged: DateTime): Future[Unit] = {
    val update = sql
      .update(TelemetryTable.t)
      .set(TelemetryTable.lastPinged, DSL.inline(jodaDateTimeToSqlTimestampUTC(lastPinged)).asInstanceOf[Object])

    database.run(updateToDBIO(update)()).map(_ => ())
  }
}
