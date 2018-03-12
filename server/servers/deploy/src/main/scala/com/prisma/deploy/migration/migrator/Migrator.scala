package com.prisma.deploy.migration.migrator

import com.prisma.shared.models.{Migration, MigrationStep, Schema, Function}

import scala.concurrent.Future

trait Migrator {
  def schedule(projectId: String, nextSchema: Schema, steps: Vector[MigrationStep], functions: Vector[Function]): Future[Migration]
}
