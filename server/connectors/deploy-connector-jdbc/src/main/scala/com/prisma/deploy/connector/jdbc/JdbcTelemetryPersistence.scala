package com.prisma.deploy.connector.jdbc

import com.prisma.connector.shared.jdbc.SlickDatabase
import com.prisma.deploy.connector.TelemetryInfo
import com.prisma.deploy.connector.persistence.TelemetryPersistence
import org.joda.time.DateTime
import org.jooq.conf.Settings
import org.jooq.impl.DSL

import scala.concurrent.Future

case class JdbcTelemetryPersistence(slickDatabase: SlickDatabase) extends JdbcPersistenceBase with TelemetryPersistence {
  import slickDatabase.profile.api._

  val sql = DSL.using(slickDatabase.dialect, new Settings().withRenderFormatted(true))

  override def getOrCreateInfo(): Future[TelemetryInfo] = {

//    Tables.Telemetry.result.headOption.flatMap {
//      case Some(entry) =>
//        DBIO.successful(entry)
//
//      case None =>
//        val newInfo     = TelemetryInfo(java.util.UUID.randomUUID().toString, None)
//        val insertQuery = Tables.Telemetry += newInfo
//        insertQuery.map(_ => newInfo)
//    }
    ???
  }

  override def updateTelemetryInfo(lastPinged: DateTime): Future[Unit] = {
//    Tables.Telemetry.map(_.lastPinged).update(Some(lastPinged))
    ???
  }
}
