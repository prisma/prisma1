package com.prisma.deploy

import akka.actor.{ActorSystem, Props}
import com.prisma.deploy.connector.DeployConnector
import com.prisma.deploy.connector.persistence.ProjectPersistence
import com.prisma.deploy.server.DatabaseSizeReporter
import com.prisma.metrics.MetricsFacade

object DeployMetrics extends MetricsFacade {
  def init(projectPersistence: ProjectPersistence, deployConnector: DeployConnector, system: ActorSystem): Unit = {
    system.actorOf(Props(DatabaseSizeReporter(projectPersistence, deployConnector, this)))
  }
}
