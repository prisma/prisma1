package cool.graph.deploy.schema.mutations

import cool.graph.deploy.database.persistence.{MigrationPersistence, ProjectPersistence}
import cool.graph.deploy.migration.mutactions.CreateClientDatabaseForProject
import cool.graph.shared.models._
import cool.graph.shared.project_dsl.TestProject
import slick.jdbc.MySQLProfile.backend.DatabaseDef

import scala.concurrent.{ExecutionContext, Future}

case class AddProjectMutation(
    args: AddProjectInput,
    client: Client,
    projectPersistence: ProjectPersistence,
    migrationPersistence: MigrationPersistence,
    clientDb: DatabaseDef
)(
    implicit ec: ExecutionContext
) extends Mutation[AddProjectMutationPayload] {

  override def execute: Future[MutationResult[AddProjectMutationPayload]] = {
    val newProject = Project(
      id = args.projectId,
      ownerId = client.id
    )

    val migration = Migration(
      projectId = newProject.id,
      revision = 0,
      hasBeenApplied = true,
      steps = Vector()
    )

    for {
      _    <- projectPersistence.create(newProject)
      stmt <- CreateClientDatabaseForProject(newProject.id).execute
      _    <- clientDb.run(stmt.sqlAction)
      _    <- migrationPersistence.create(newProject, migration)
    } yield MutationSuccess(AddProjectMutationPayload(args.clientMutationId, newProject))
  }
}

case class AddProjectMutationPayload(
    clientMutationId: Option[String],
    project: Project
) extends sangria.relay.Mutation

case class AddProjectInput(
    clientMutationId: Option[String],
    projectId: String
) extends sangria.relay.Mutation
