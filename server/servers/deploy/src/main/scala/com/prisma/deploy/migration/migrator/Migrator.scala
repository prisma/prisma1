package com.prisma.deploy.migration.migrator

import com.prisma.shared.models.{Function, Migration, MigrationStep, Schema}

import scala.concurrent.Future

trait Migrator {
  def initialize: Unit
  def schedule(projectId: String, nextSchema: Schema, steps: Vector[MigrationStep], functions: Vector[Function], rawDataModel: String): Future[Migration]
}
