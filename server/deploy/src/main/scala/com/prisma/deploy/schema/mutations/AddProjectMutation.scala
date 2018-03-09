package com.prisma.deploy.schema.mutations

import com.prisma.deploy.database.persistence.{MigrationPersistence, ProjectPersistence}
import com.prisma.deploy.connector.DeployConnector
import com.prisma.deploy.schema.{InvalidServiceName, InvalidServiceStage, ProjectAlreadyExists}
import com.prisma.deploy.validation.NameConstraints
import com.prisma.shared.models._
import com.prisma.utils.await.AwaitUtils

import scala.concurrent.{ExecutionContext, Future}

case class AddProjectMutation(
    args: AddProjectInput,
    projectPersistence: ProjectPersistence,
    migrationPersistence: MigrationPersistence,
    persistencePlugin: DeployConnector
)(
    implicit ec: ExecutionContext
) extends Mutation[AddProjectMutationPayload]
    with AwaitUtils {
  val projectId = ProjectId.toEncodedString(name = args.name, stage = args.stage)

  override def execute: Future[MutationResult[AddProjectMutationPayload]] = {
    validate()

    val newProject = Project(
      id = projectId,
      ownerId = args.ownerId.getOrElse(""),
      secrets = args.secrets,
      schema = Schema()
    )

    val migration = Migration(
      projectId = newProject.id,
      revision = 0,
      applied = 0,
      rolledBack = 0,
      status = MigrationStatus.Success,
      steps = Vector.empty,
      errors = Vector.empty,
      schema = Schema(),
      functions = Vector.empty
    )

    for {
      _ <- projectPersistence.create(newProject)
//      stmt <- CreateClientDatabaseForProject(newProject.id).execute
//      _    <- clientDb.run(stmt.sqlAction)
      _ <- persistencePlugin.createProjectDatabase(newProject.id)
      _ <- migrationPersistence.create(migration)
    } yield MutationSuccess(AddProjectMutationPayload(args.clientMutationId, newProject))
  }

  private def validate(): Unit = {
    if (!NameConstraints.isValidServiceName(args.name)) {
      throw InvalidServiceName(args.name)
    }
    if (!NameConstraints.isValidServiceStage(args.stage)) {
      throw InvalidServiceStage(args.stage)
    }
    val projectForGivenId = projectPersistence.load(projectId).await()
    if (projectForGivenId.isDefined) {
      throw ProjectAlreadyExists(name = args.name, stage = args.stage)
    }
  }
}

case class AddProjectMutationPayload(
    clientMutationId: Option[String],
    project: Project
) extends sangria.relay.Mutation

case class AddProjectInput(
    clientMutationId: Option[String],
    ownerId: Option[String],
    name: String,
    stage: String,
    secrets: Vector[String]
) extends sangria.relay.Mutation
