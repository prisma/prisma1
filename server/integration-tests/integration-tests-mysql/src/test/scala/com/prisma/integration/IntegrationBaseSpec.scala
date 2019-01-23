package com.prisma.integration

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.prisma.api.connector.DataResolver
import com.prisma.api.util.StringMatchers
import com.prisma.api.{ApiTestServer, TestApiDependenciesImpl}
import com.prisma.deploy.specutils.{TestDeployDependencies, DeployTestServer}
import com.prisma.shared.models.{Migration, Project}
import com.prisma.utils.await.AwaitUtils
import com.prisma.utils.json.PlayJsonExtensions
import cool.graph.cuid.Cuid
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}
import play.api.libs.json.JsString

import scala.collection.mutable.ArrayBuffer

trait IntegrationBaseSpec extends BeforeAndAfterEach with BeforeAndAfterAll with PlayJsonExtensions with AwaitUtils with StringMatchers { self: Suite =>

  implicit lazy val system       = ActorSystem()
  implicit lazy val materializer = ActorMaterializer()

  override protected def afterAll(): Unit = {
    super.afterAll()
    deployTestDependencies.deployConnector.shutdown().await
    apiTestDependencies.destroy
  }

  // API

  implicit lazy val apiTestDependencies = new TestApiDependenciesImpl
  val apiServer                         = ApiTestServer()

  def dataResolver(project: Project): DataResolver = apiTestDependencies.dataResolver(project)

  // DEPLOY

  implicit lazy val deployTestDependencies: TestDeployDependencies = TestDeployDependencies()

  val deployServer      = DeployTestServer()
  val projectsToCleanUp = new ArrayBuffer[String]
  val internalDB        = deployTestDependencies.deployConnector

  val basicTypesGql =
    """
      |type TestModel {
      |  id: ID! @unique
      |}
    """.stripMargin.trim()

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    deployTestDependencies.deployConnector.initialize().await()
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    projectsToCleanUp.foreach(id => internalDB.deleteProjectDatabase(id).await)
    projectsToCleanUp.clear()
  }

  def setupProject(
      schema: String,
      name: String = Cuid.createCuid(),
      stage: String = Cuid.createCuid(),
      secrets: Vector[String] = Vector.empty
  ): (Project, Migration) = {

    val projectId = name + "@" + stage
    projectsToCleanUp += projectId
    deployServer.addProject(name, stage)
    deployServer.deploySchema(name, stage, schema.stripMargin, secrets)
  }

  def formatSchema(schema: String): String = JsString(schema).toString()
}
