package cool.graph.deploy.migration.migrator

import cool.graph.shared.models.{Migration, MigrationStep, Schema, Function}

import scala.concurrent.Future

trait Migrator {
  def schedule(projectId: String, nextSchema: Schema, steps: Vector[MigrationStep], functions: Vector[Function]): Future[Migration]
}
