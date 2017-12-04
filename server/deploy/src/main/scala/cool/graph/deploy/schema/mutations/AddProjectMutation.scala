package cool.graph.deploy.schema.mutations

import cool.graph.deploy.database.persistence.{MigrationPersistence, ProjectPersistence}
import cool.graph.deploy.migration.mutactions.CreateClientDatabaseForProject
import cool.graph.deploy.schema.{InvalidServiceName, InvalidServiceStage}
import cool.graph.deploy.validation.NameConstraints
import cool.graph.shared.models._
import slick.jdbc.MySQLProfile.backend.DatabaseDef

import scala.concurrent.{ExecutionContext, Future}

case class AddProjectMutation(
    args: AddProjectInput,
    projectPersistence: ProjectPersistence,
    migrationPersistence: MigrationPersistence,
    clientDb: DatabaseDef
)(
    implicit ec: ExecutionContext
) extends Mutation[AddProjectMutationPayload] {

  override def execute: Future[MutationResult[AddProjectMutationPayload]] = {
    validate()

    val projectId = ProjectId.toEncodedString(name = args.name, stage = args.stage)
    val newProject = Project(
      id = projectId,
      ownerId = args.ownerId.getOrElse("")
    )

    val migration = Migration(
      projectId = newProject.id,
      revision = 0,
      hasBeenApplied = true,
      steps = Vector.empty
    )

    for {
      _    <- projectPersistence.create(newProject)
      stmt <- CreateClientDatabaseForProject(newProject.id).execute
      _    <- clientDb.run(stmt.sqlAction)
      _    <- migrationPersistence.create(newProject, migration)
    } yield MutationSuccess(AddProjectMutationPayload(args.clientMutationId, newProject))
  }

  private def validate(): Unit = {
    if (NameConstraints.isValidServiceName(args.name)) {
      throw InvalidServiceName(args.name)
    }
    if (NameConstraints.isValidServiceStage(args.stage)) {
      throw InvalidServiceStage(args.stage)
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
    stage: String
) extends sangria.relay.Mutation
