package com.prisma.deploy.connector.persistence

import com.prisma.deploy.connector.TelemetryInfo
import org.joda.time.DateTime

import scala.concurrent.Future

trait TelemetryPersistence {
  def getOrCreateInfo(): Future[TelemetryInfo]
  def updateTelemetryInfo(lastPinged: DateTime): Future[Unit]
}
