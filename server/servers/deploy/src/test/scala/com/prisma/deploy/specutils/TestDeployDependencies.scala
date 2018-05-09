package com.prisma.deploy.specutils

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.TestProbe
import com.prisma.auth.AuthImpl
import com.prisma.config.ConfigLoader
import com.prisma.connectors.utils.ConnectorUtils
import com.prisma.deploy.DeployDependencies
import com.prisma.deploy.migration.validation.SchemaError
import com.prisma.deploy.schema.mutations.{FunctionInput, FunctionValidator}
import com.prisma.deploy.server.auth.DummyManagementAuth
import com.prisma.errors.{BugsnagErrorReporter, ErrorReporter}
import com.prisma.messagebus.pubsub.inmemory.InMemoryAkkaPubSub
import com.prisma.shared.models.{Project, ProjectIdEncoder}

case class TestDeployDependencies()(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends DeployDependencies {
  import system.dispatcher

  override implicit def self: DeployDependencies = this

  val config = ConfigLoader.load()

  implicit val reporter: ErrorReporter    = BugsnagErrorReporter(sys.env.getOrElse("BUGSNAG_API_KEY", ""))
  override lazy val migrator              = TestMigrator(migrationPersistence, deployConnector.deployMutactionExecutor)
  override lazy val managementAuth        = DummyManagementAuth()
  override lazy val invalidationPublisher = InMemoryAkkaPubSub[String]()

  override def apiAuth = AuthImpl

  def deployConnector = ConnectorUtils.loadDeployConnector(config.copy(databases = config.databases.map(_.copy(pooled = false))))

  override def projectIdEncoder: ProjectIdEncoder = deployConnector.projectIdEncoder

  override def functionValidator: FunctionValidator = (project: Project, fn: FunctionInput) => {
    if (fn.name == "failing") Vector(SchemaError(`type` = "model", field = "field", description = "error")) else Vector.empty
  }

  lazy val telemetryActor = TestProbe().ref
}
