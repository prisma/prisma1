package com.prisma.deploy.schema.mutations

import com.prisma.deploy.database.persistence.{MigrationPersistence, ProjectPersistence}
import com.prisma.deploy.migration.mutactions.DeleteClientDatabaseForProject
import com.prisma.deploy.schema.InvalidServiceName
import com.prisma.messagebus.PubSubPublisher
import com.prisma.messagebus.pubsub.Only
import com.prisma.shared.models._
import slick.jdbc.MySQLProfile.backend.DatabaseDef

import scala.concurrent.{ExecutionContext, Future}

case class DeleteProjectMutation(
    args: DeleteProjectInput,
    projectPersistence: ProjectPersistence,
    clientDb: DatabaseDef,
    invalidationPubSub: PubSubPublisher[String]
)(
    implicit ec: ExecutionContext
) extends Mutation[DeleteProjectMutationPayload] {

  override def execute: Future[MutationResult[DeleteProjectMutationPayload]] = {

    val projectId = ProjectId.toEncodedString(name = args.name, stage = args.stage)

    for {
      projectOpt <- projectPersistence.load(projectId)
      project    = validate(projectOpt)
      _          <- projectPersistence.delete(projectId)
      stmt       <- DeleteClientDatabaseForProject(projectId).execute
      _          <- clientDb.run(stmt.sqlAction)
      _          = invalidationPubSub.publish(Only(projectId), projectId)
    } yield MutationSuccess(DeleteProjectMutationPayload(args.clientMutationId, project))
  }

  private def validate(project: Option[Project]): Project = {
    project match {
      case None    => throw InvalidServiceName(args.name)
      case Some(p) => p
    }
  }
}

case class DeleteProjectMutationPayload(
    clientMutationId: Option[String],
    project: Project
) extends sangria.relay.Mutation

case class DeleteProjectInput(
    clientMutationId: Option[String],
    name: String,
    stage: String
) extends sangria.relay.Mutation
