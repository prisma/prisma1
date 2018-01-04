package cool.graph.deploy.migration.migrator

import cool.graph.shared.models.{Migration, MigrationStep, Schema}

import scala.concurrent.Future

trait Migrator {
  def schedule(projectId: String, nextSchema: Schema, steps: Vector[MigrationStep]): Future[Migration]
}
