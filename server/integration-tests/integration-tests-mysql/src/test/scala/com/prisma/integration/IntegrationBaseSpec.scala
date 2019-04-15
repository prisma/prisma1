package com.prisma.integration

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.prisma.ConnectorAwareTest
import com.prisma.api.connector.DataResolver
import com.prisma.api.util.StringMatchers
import com.prisma.api.{ApiTestServer, ExternalApiTestServer, InternalApiTestServer, TestApiDependenciesImpl}
import com.prisma.config.PrismaConfig
import com.prisma.deploy.specutils.{DeployTestServer, TestDeployDependencies}
import com.prisma.shared.models.ConnectorCapability.EmbeddedScalarListsCapability
import com.prisma.shared.models.{ConnectorCapabilities, Migration, Project}
import com.prisma.utils.await.AwaitUtils
import com.prisma.utils.json.PlayJsonExtensions
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}
import play.api.libs.json.JsString

import scala.collection.mutable.ArrayBuffer

trait IntegrationBaseSpec
    extends BeforeAndAfterEach
    with BeforeAndAfterAll
    with PlayJsonExtensions
    with AwaitUtils
    with StringMatchers
    with ConnectorAwareTest { self: Suite =>

  implicit lazy val system        = ActorSystem()
  implicit lazy val materializer  = ActorMaterializer()
  implicit lazy val implicitSuite = self

  override protected def afterAll(): Unit = {
    super.afterAll()
    deployTestDependencies.deployConnector.shutdown().await
    apiTestDependencies.destroy
  }

  // API
  implicit lazy val apiTestDependencies = new TestApiDependenciesImpl
  val apiServer                         = loadTestServer()

  def dataResolver(project: Project): DataResolver = apiTestDependencies.dataResolver(project)

  override def capabilities: ConnectorCapabilities = apiTestDependencies.apiConnector.capabilities

  override def prismaConfig: PrismaConfig = apiTestDependencies.config

  // DEPLOY
  implicit lazy val deployTestDependencies: TestDeployDependencies = TestDeployDependencies()

  val deployServer      = DeployTestServer()
  val projectsToCleanUp = new ArrayBuffer[String]
  val internalDB        = deployTestDependencies.deployConnector

  val basicTypesGql =
    """
      |type TestModel {
      |  id: ID! @id
      |}
    """.stripMargin.trim()

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    deployTestDependencies.deployConnector.initialize().await()
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    internalDB.reset().await
  }

  def loadTestServer(): ApiTestServer = {
    prismaConfig.databases.head.connector match {
      case "native-integration-tests" => ExternalApiTestServer()
      case _                          => InternalApiTestServer()
    }
  }

  def setupProject(
      schema: String,
      secrets: Vector[String] = Vector.empty
  )(implicit suite: Suite): (Project, Migration) = {

    val (name, stage) = (suite.getClass.getSimpleName, "s")
    val idAsString    = deployTestDependencies.projectIdEncoder.toEncodedString(name, stage)
    internalDB.deleteProjectDatabase(idAsString).await()
    deployServer.addProject(name, stage)
    deployServer.deploySchema(name, stage, schema.stripMargin, secrets)
  }

  def formatSchema(schema: String): String = JsString(schema).toString()

  val scalarListDirective = if (capabilities.hasNot(EmbeddedScalarListsCapability)) {
    "@scalarList(strategy: RELATION)"
  } else {
    ""
  }
}
