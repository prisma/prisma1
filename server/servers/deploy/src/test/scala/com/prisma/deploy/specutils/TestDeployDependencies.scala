package com.prisma.deploy.specutils

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.TestProbe
import com.prisma.cache.factory.{CacheFactory, CaffeineCacheFactory}
import com.prisma.config.ConfigLoader
import com.prisma.connectors.utils.{ConnectorLoader, SupportedDrivers}
import com.prisma.deploy.DeployDependencies
import com.prisma.deploy.migration.validation.DeployError
import com.prisma.deploy.schema.mutations.{FunctionInput, FunctionValidator}
import com.prisma.errors.{DummyErrorReporter, ErrorReporter}
import com.prisma.jwt.jna.JnaAuth
import com.prisma.jwt.{Algorithm, NoAuth}
import com.prisma.messagebus.pubsub.inmemory.InMemoryAkkaPubSub
import com.prisma.metrics.MetricsRegistry
import com.prisma.native_jdbc.CustomJdbcDriver
import com.prisma.shared.models.{ProjectIdEncoder, Schema}
import org.scalactic.{Bad, Good}

case class TestDeployDependencies()(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends DeployDependencies {
  override implicit def self: DeployDependencies = this

  val config          = ConfigLoader.load()
  val useNativeDriver = sys.env.getOrElse("USE_NATIVE_DRIVER", "0") == "1"

  implicit val supportedDrivers: SupportedDrivers = SupportedDrivers(
    SupportedDrivers.MYSQL -> new org.mariadb.jdbc.Driver,
    SupportedDrivers.POSTGRES -> (if (useNativeDriver) {
                                    println("Using native driver for testing")
                                    CustomJdbcDriver.jna
                                  } else {
                                    new org.postgresql.Driver
                                  })
  )

  implicit val reporter: ErrorReporter = DummyErrorReporter
  override lazy val migrator = TestMigrator(
    migrationPersistence,
    projectPersistence,
    deployConnector.deployMutactionExecutor,
    deployConnector.databaseInspector
  )
  override lazy val managementAuth        = NoAuth
  override lazy val invalidationPublisher = InMemoryAkkaPubSub[String]()

  override val auth = JnaAuth(Algorithm.HS256)

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

  lazy val telemetryActor                            = TestProbe().ref
  override val managementSecret: String              = ""
  override val cacheFactory: CacheFactory            = new CaffeineCacheFactory()
  override lazy val metricsRegistry: MetricsRegistry = ???
}
