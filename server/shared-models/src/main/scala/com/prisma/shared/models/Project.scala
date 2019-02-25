package com.prisma.shared.models

import com.prisma.shared.models.IdType.Id

case class Project(
    id: Id,
    revision: Int = 1,
    schema: Schema,
    secrets: Vector[String] = Vector.empty,
    allowQueries: Boolean = true,
    allowMutations: Boolean = true,
    functions: List[Function] = List.empty,
    manifestation: ProjectManifestation = ProjectManifestation.empty
) {
  def models            = schema.models
  def relations         = schema.relations
  def enums             = schema.enums
  def nonEmbeddedModels = schema.models.filterNot(_.isEmbedded)

  val serverSideSubscriptionFunctions = functions.collect { case x: ServerSideSubscriptionFunction => x }

  val dbName: String = manifestation match {
    case ProjectManifestation(Some(_), Some(schema), "postgres")                  => schema
    case ProjectManifestation(Some(_), Some(schema), _)                           => sys.error("Only Postgres allows schema + database.")
    case ProjectManifestation(Some(_), None, "postgres")                          => id
    case ProjectManifestation(Some(database), None, "mongo" | "mysql" | "sqlite") => database
    case ProjectManifestation(Some(database), None, _)                            => sys.error("We only have four connectors atm.")
    case ProjectManifestation(None, Some(_), _)                                   => sys.error("You cannot provide a schema only.")
    case ProjectManifestation(None, None, _)                                      => id
  }
}

object ProjectManifestation {
  val empty = ProjectManifestation(None, None, "")
}

case class ProjectManifestation(database: Option[String], schema: Option[String], connector: String)
