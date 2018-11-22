package com.prisma.deploy.migration

import com.prisma.deploy.connector.{EmptyDatabaseIntrospectionInferrer, FieldRequirementsInterface, Tables}
import com.prisma.deploy.connector.postgres.database.DatabaseIntrospectionInferrerImpl
import com.prisma.deploy.migration.inference.{MigrationStepsInferrer, SchemaInferrer}
import com.prisma.deploy.schema.mutations.{DeployMutation, DeployMutationInput, MutationError, MutationSuccess}
import com.prisma.deploy.specutils.DeploySpecBase
import com.prisma.shared.models.{ConnectorCapabilities, Project, Schema}
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{Matchers, WordSpecLike}

class MigrationsSpec extends WordSpecLike with Matchers with DeploySpecBase {

  val name      = this.getClass.getSimpleName
  val stage     = "default"
  val serviceId = testDependencies.projectIdEncoder.toEncodedString(name, stage)
  val initialDataModel =
    """
      |type A {
      |  id: ID! @id
      |}
    """.stripMargin
  val inspector        = deployConnector.testFacilities.inspector
  var project: Project = Project(id = serviceId, schema = Schema.empty)

  import system.dispatcher

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    setup()
  }

  "adding a scalar field should work" in {
    val dataModel =
      """
        |type A {
        |  id: ID! @id
        |  field: String
        |}
      """.stripMargin

    deploy(dataModel, ConnectorCapabilities.empty)

    val result = inspect
    val column = result.table_!("A").column_!("field")
    column.tpe should be("text")
  }

  def setup() = {
    val idAsString = testDependencies.projectIdEncoder.toEncodedString(name, stage)
    deployConnector.deleteProjectDatabase(idAsString).await()
    server.addProject(name, stage)
    deploy(initialDataModel, ConnectorCapabilities.empty)
    project = testDependencies.projectPersistence.load(serviceId).await.get
  }

  def deploy(dataModel: String, capabilities: ConnectorCapabilities): Unit = {
    val input = DeployMutationInput(
      clientMutationId = None,
      name = name,
      stage = stage,
      types = dataModel,
      dryRun = None,
      force = None,
      secrets = Vector.empty,
      functions = Vector.empty
    )
    val mutation = DeployMutation(
      args = input,
      project = project,
      schemaInferrer = SchemaInferrer(capabilities),
      migrationStepsInferrer = MigrationStepsInferrer(),
      schemaMapper = SchemaMapper,
      migrationPersistence = testDependencies.migrationPersistence,
      projectPersistence = testDependencies.projectPersistence,
      migrator = testDependencies.migrator,
      functionValidator = testDependencies.functionValidator,
      invalidationPublisher = testDependencies.invalidationPublisher,
      capabilities = capabilities,
      clientDbQueries = deployConnector.clientDBQueries(project),
      databaseIntrospectionInferrer = EmptyDatabaseIntrospectionInferrer,
      fieldRequirements = FieldRequirementsInterface.empty,
      isActive = true
    )

    val result = mutation.execute.await
    result match {
      case MutationSuccess(result) =>
        if (result.errors.nonEmpty) {
          sys.error(s"Deploy returned unexpected errors: ${result.errors}")
        }
      case MutationError =>
        sys.error("Deploy returned an unexpected error")
    }
  }

  def inspect: Tables = {
    deployConnector.testFacilities.inspector.inspect(serviceId).await()
  }
}
