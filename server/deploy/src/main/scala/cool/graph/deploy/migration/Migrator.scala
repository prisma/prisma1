package cool.graph.deploy.migration

import cool.graph.shared.models.Migration

trait Migrator {
  def schedule(migration: Migration): Unit
}
