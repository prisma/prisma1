package com.prisma.image

import com.prisma.api.project.RefreshableProjectFetcher
import com.prisma.deploy.connector.persistence.ProjectPersistence
import com.prisma.shared.models.Project

import scala.concurrent.{ExecutionContext, Future}

case class SingleServerProjectFetcher(projectPersistence: ProjectPersistence)(implicit ec: ExecutionContext) extends RefreshableProjectFetcher {
  override def fetch(projectIdOrAlias: String): Future[Option[Project]] = {
    fetchRefreshed(projectIdOrAlias)
  }

  override def fetchRefreshed(projectIdOrAlias: String) = projectPersistence.load(projectIdOrAlias)
}
