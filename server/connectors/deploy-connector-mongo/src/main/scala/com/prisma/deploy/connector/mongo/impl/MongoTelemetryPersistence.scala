package com.prisma.deploy.connector.mongo.impl

import com.prisma.deploy.connector.TelemetryInfo
import com.prisma.deploy.connector.persistence.TelemetryPersistence
import org.joda.time.DateTime

import scala.concurrent.Future

case class MongoTelemetryPersistence() extends TelemetryPersistence {
  override def getOrCreateInfo(): Future[TelemetryInfo]                = Future.successful(TelemetryInfo("not Implemented", None))
  override def updateTelemetryInfo(lastPinged: DateTime): Future[Unit] = Future.unit
}
