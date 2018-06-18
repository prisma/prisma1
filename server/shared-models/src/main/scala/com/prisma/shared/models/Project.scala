package com.prisma.shared.models

import com.prisma.shared.models.IdType.Id

case class Project(
    id: Id,
    ownerId: Id,
    revision: Int = 1,
    schema: Schema,
    webhookUrl: Option[String] = None,
    secrets: Vector[String] = Vector.empty,
    allowQueries: Boolean = true,
    allowMutations: Boolean = true,
    functions: List[Function] = List.empty
) {
  def models    = schema.models
  def relations = schema.relations
  def enums     = schema.enums

  val serverSideSubscriptionFunctions = functions.collect { case x: ServerSideSubscriptionFunction => x }
}

object ProjectWithClientId {
  def apply(project: Project): ProjectWithClientId = ProjectWithClientId(project, project.ownerId)
}
case class ProjectWithClientId(project: Project, clientId: Id)
