package com.prisma.api

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.prisma.ConnectorAwareTest
import com.prisma.api.connector.DataResolver
import com.prisma.api.util.StringMatchers
import com.prisma.config.PrismaConfig
import com.prisma.shared.models.{ConnectorCapability, Project}
import com.prisma.utils.await.AwaitUtils
import com.prisma.utils.json.PlayJsonExtensions
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}
import play.api.libs.json.JsString

trait ApiSpecBase extends ConnectorAwareTest with BeforeAndAfterEach with BeforeAndAfterAll with PlayJsonExtensions with StringMatchers with AwaitUtils {
  self: Suite =>

  implicit lazy val system                = ActorSystem()
  implicit lazy val materializer          = ActorMaterializer()
  implicit lazy val testDependencies      = new TestApiDependenciesImpl
  implicit lazy val implicitSuite         = self
  implicit lazy val deployConnector       = testDependencies.deployConnector
  val server                              = ApiTestServer()
  val database                            = ApiTestDatabase()
  def capabilities                        = testDependencies.apiConnector.capabilities
  override def prismaConfig: PrismaConfig = testDependencies.config

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
