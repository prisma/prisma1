package com.prisma.deploy.schema.mutations

import com.prisma.deploy.DeployDependencies
import com.prisma.deploy.connector.persistence.{MigrationPersistence, ProjectPersistence}
import com.prisma.deploy.connector.DeployConnector
import com.prisma.deploy.schema._
import com.prisma.deploy.validation.NameConstraints
import com.prisma.shared.models._
import com.prisma.utils.await.AwaitUtils

import scala.concurrent.{ExecutionContext, Future}

case class AddProjectMutation(
    args: AddProjectInput,
    projectPersistence: ProjectPersistence,
    migrationPersistence: MigrationPersistence,
    deployConnector: DeployConnector,
    connectorCapabilities: ConnectorCapabilities
)(
    implicit ec: ExecutionContext,
    dependencies: DeployDependencies
) extends Mutation[AddProjectMutationPayload]
    with AwaitUtils {
  val projectIdEncoder = dependencies.projectIdEncoder
  val projectId        = projectIdEncoder.toEncodedString(name = args.name, stage = args.stage)

  override def execute: Future[MutationResult[AddProjectMutationPayload]] = {
    validate()

    val schema = if (connectorCapabilities.isDataModelV11) {
      Schema.empty.copy(version = Some(Schema.version.v11))
    } else {
      Schema.empty
    }

    val newProject = Project(
      id = projectId,
      secrets = args.secrets,
      schema = schema
    )

    val migration = Migration(
      projectId = newProject.id,
      revision = 0,
      applied = 0,
      rolledBack = 0,
      status = MigrationStatus.Success,
      steps = Vector.empty,
      errors = Vector.empty,
      schema = schema,
      functions = Vector.empty,
      previousSchema = schema,
      rawDataModel = ""
    )

    for {
      _             <- projectPersistence.create(newProject)
      _             <- migrationPersistence.create(migration)
      loadedProject <- projectPersistence.load(newProject.id)
      _             <- deployConnector.createProjectDatabase(loadedProject.get.dbName)
    } yield MutationSuccess(AddProjectMutationPayload(args.clientMutationId, loadedProject.get))
  }

  private def validate(): Unit = {
    if (projectIdEncoder.reservedServiceAndStageNames.contains(args.name)) throw ReservedServiceName(args.name)
    if (projectIdEncoder.reservedServiceAndStageNames.contains(args.stage)) throw ReservedStageName(args.stage)
    if (!NameConstraints.isValidServiceName(args.name)) throw InvalidServiceName(args.name)
    if (!NameConstraints.isValidServiceStage(args.stage)) throw InvalidServiceStage(args.stage)

    val projectForGivenId = projectPersistence.load(projectId).await()
    if (projectForGivenId.isDefined) throw ProjectAlreadyExists(name = args.name, stage = args.stage)
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
