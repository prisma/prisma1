package cool.graph.deploy.specutils

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.cuid.Cuid
import cool.graph.shared.models.Project
import cool.graph.utils.await.AwaitUtils
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}
import spray.json.JsString

import scala.collection.mutable.ArrayBuffer

trait DeploySpecBase extends BeforeAndAfterEach with BeforeAndAfterAll with AwaitUtils with SprayJsonExtensions { self: Suite =>

  implicit lazy val system                                   = ActorSystem()
  implicit lazy val materializer                             = ActorMaterializer()
  implicit lazy val testDependencies: DeployTestDependencies = DeployTestDependencies()

  val server            = DeployTestServer()
  val internalDb        = testDependencies.internalTestDb
  val clientDb          = testDependencies.clientTestDb
  val projectsToCleanUp = new ArrayBuffer[String]

  val basicTypesGql =
    """
      |type TestModel {
      |  id: ID! @unique
      |}
    """.stripMargin.trim()

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    internalDb.createInternalDatabaseSchema()
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    internalDb.shutdown()
    clientDb.shutdown()
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    internalDb.truncateTables()
    projectsToCleanUp.foreach(clientDb.delete)
    projectsToCleanUp.clear()
  }

  def setupProject(schema: String, name: String = Cuid.createCuid(), stage: String = Cuid.createCuid()): Project = {
    server.query(s"""
        |mutation {
        | addProject(input: {
        |   name: "$name",
        |   stage: "$stage"
        | }) {
        |   project {
        |     name
        |     stage
        |   }
        | }
        |}
      """.stripMargin)

    val projectId = name + "@" + stage
    projectsToCleanUp :+ projectId

    server.query(s"""
        |mutation {
        |  deploy(input:{name: "$name", stage: "$stage", types: ${formatSchema(schema)}}){
        |    errors {
        |      description
        |    }
        |  }
        |}
      """.stripMargin)

    testDependencies.projectPersistence.load(projectId).await.get
  }

  def formatSchema(schema: String): String = JsString(schema).toString()
  def escapeString(str: String): String    = JsString(str).toString()
}
