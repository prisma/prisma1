package com.prisma.deploy.connector
import com.prisma.deploy.connector.persistence.InternalMigration
import com.prisma.shared.models.Project

import scala.concurrent.Future

trait InternalMigrationApplier {
  def apply(internalMigration: InternalMigration, project: Project): Future[Unit]
}

object EmptyInternalMigrationApplier extends InternalMigrationApplier {
  override def apply(internalMigration: InternalMigration, project: Project): Future[Unit] = {
    Future.successful(())
  }
}
