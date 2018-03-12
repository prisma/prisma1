package com.prisma.subscriptions.helpers

import akka.actor.ActorSystem
import com.prisma.shared.models.ProjectWithClientId
import com.prisma.subscriptions.SubscriptionDependencies

import scala.concurrent.{ExecutionContext, Future}

object ProjectHelper {
  def resolveProject(projectId: String)(implicit dependencies: SubscriptionDependencies, as: ActorSystem, ec: ExecutionContext): Future[ProjectWithClientId] = {
    dependencies.projectFetcher.fetch(projectId).map {
      case None =>
        sys.error(s"ProjectHelper: Could not resolve project with id: $projectId")

      case Some(project: ProjectWithClientId) =>
        project
    }
  }
}
