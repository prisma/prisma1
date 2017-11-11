package cool.graph.client.server

import cool.graph.client.UserContext
import cool.graph.shared.models.Project
import sangria.schema.Schema

trait ProjectSchemaBuilder {
  def build(project: Project): Schema[UserContext, Unit]
}

object ProjectSchemaBuilder {
  def apply(fn: Project => Schema[UserContext, Unit]): ProjectSchemaBuilder = new ProjectSchemaBuilder {
    override def build(project: Project) = fn(project)
  }
}
