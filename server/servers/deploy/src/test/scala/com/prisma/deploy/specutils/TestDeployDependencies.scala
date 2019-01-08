package com.prisma.deploy.specutils

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.TestProbe
import com.prisma.auth.AuthImpl
import com.prisma.config.ConfigLoader
import com.prisma.connectors.utils.ConnectorLoader
import com.prisma.deploy.DeployDependencies
import com.prisma.deploy.migration.validation.DeployError
import com.prisma.deploy.schema.mutations.{FunctionInput, FunctionValidator}
import com.prisma.deploy.server.auth.DummyManagementAuth
import com.prisma.errors.{BugsnagErrorReporter, ErrorReporter}
import com.prisma.messagebus.pubsub.inmemory.InMemoryAkkaPubSub
import com.prisma.shared.models.{ProjectIdEncoder, Schema}
import org.scalactic.{Bad, Good}

case class TestDeployDependencies()(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends DeployDependencies {
  import system.dispatcher

  override implicit def self: DeployDependencies = this

  val config = ConfigLoader.load()

  implicit val reporter: ErrorReporter    = BugsnagErrorReporter(sys.env.getOrElse("BUGSNAG_API_KEY", ""))
  override lazy val migrator              = TestMigrator(migrationPersistence, deployConnector.deployMutactionExecutor, deployConnector.testFacilities.inspector)
  override lazy val managementAuth        = DummyManagementAuth()
  override lazy val invalidationPublisher = InMemoryAkkaPubSub[String]()

  override def apiAuth = AuthImpl

  def deployConnector = ConnectorLoader.loadDeployConnector(config.copy(databases = config.databases.map(_.copy(pooled = false))), isTest = true)

  override def projectIdEncoder: ProjectIdEncoder = deployConnector.projectIdEncoder

  override def functionValidator: FunctionValidator = new FunctionValidator {
    override def validateFunctionInputs(schema: Schema, functionInputs: Vector[FunctionInput]) = {
      if (functionInputs.map(_.name).contains("failing")) {
        Bad(Vector(DeployError(`type` = "model", field = "field", description = "error")))
      } else {
        Good(functionInputs.map(convertFunctionInput))
      }
    }
  }

  lazy val telemetryActor = TestProbe().ref
}
