package com.prisma.api

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.prisma.ConnectorAwareTest
import com.prisma.api.connector.ApiConnectorCapability.ScalarListsCapability
import com.prisma.api.connector.{ApiConnectorCapability, DataResolver}
import com.prisma.api.util.StringMatchers
import com.prisma.shared.models.Project
import com.prisma.utils.await.AwaitUtils
import com.prisma.utils.json.PlayJsonExtensions
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}
import play.api.libs.json.JsString

trait ApiSpecBase extends ConnectorAwareTest with BeforeAndAfterEach with BeforeAndAfterAll with PlayJsonExtensions with StringMatchers with AwaitUtils {
  self: Suite =>

  def runOnlyForCapabilities: Set[ApiConnectorCapability]  = Set.empty
  def doNotRunForCapabilities: Set[ApiConnectorCapability] = Set.empty

  abstract override def tags: Map[String, Set[String]] = {
    val connectorHasTheRightCapabilities = runOnlyForCapabilities.forall(connectorHasCapability) || runOnlyForCapabilities.isEmpty
    val connectorHasAWrongCapability     = doNotRunForCapabilities.exists(connectorHasCapability)
    if (!connectorHasTheRightCapabilities) {
      println(
        s"""the suite ${self.getClass.getSimpleName} will be ignored because it does not have the right capabilities
           | required capabilities: ${runOnlyForCapabilities.mkString(",")}
           | connector capabilities: ${testDependencies.apiConnector.capabilities.mkString(",")}
         """.stripMargin
      )
    }
    if (connectorHasAWrongCapability) {
      println(
        s"""the suite ${self.getClass.getSimpleName} will be ignored because it has a wrong capability
           | wrong capabilities: ${doNotRunForCapabilities.mkString(",")}
           | connector capabilities: ${testDependencies.apiConnector.capabilities.mkString(",")}
         """.stripMargin
      )
    }
    if (!connectorHasTheRightCapabilities || connectorHasAWrongCapability) {
      ignoreAllTests
    } else {
      super.tags
    }
  }

  private def connectorHasCapability(capability: ApiConnectorCapability) = {
    val capabilities = testDependencies.apiConnector.capabilities.toSet
    capability match {
      case ScalarListsCapability => capabilities.exists(_.isInstanceOf[ScalarListsCapability])
      case c                     => capabilities.contains(c)
    }
  }

  implicit lazy val system           = ActorSystem()
  implicit lazy val materializer     = ActorMaterializer()
  implicit lazy val testDependencies = new TestApiDependenciesImpl
  implicit lazy val implicitSuite    = self
  implicit lazy val deployConnector  = testDependencies.deployConnector
  val server                         = ApiTestServer()
  val database                       = ApiTestDatabase()

  override def prismaConfig = testDependencies.config

  def dataResolver(project: Project): DataResolver = testDependencies.dataResolver(project)

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    testDependencies.deployConnector.initialize().await()
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    testDependencies.destroy
  }

  def escapeString(str: String) = JsString(str).toString()
}
