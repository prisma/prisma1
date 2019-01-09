package com.prisma.api.project

import com.prisma.api.schema.APIErrors.ProjectNotFound
import com.prisma.shared.models.Project

import scala.concurrent.{ExecutionContext, Future}

trait ProjectFetcher {
  def fetch_!(projectIdOrAlias: String)(implicit ec: ExecutionContext): Future[Project] = {
    fetch(projectIdOrAlias = projectIdOrAlias) map {
      case None          => throw ProjectNotFound(projectIdOrAlias)
      case Some(project) => project
    }
  }

  def fetch(projectIdOrAlias: String): Future[Option[Project]]
}

trait RefreshableProjectFetcher extends ProjectFetcher {
  def fetchRefreshed(projectIdOrAlias: String): Future[Option[Project]]
}
