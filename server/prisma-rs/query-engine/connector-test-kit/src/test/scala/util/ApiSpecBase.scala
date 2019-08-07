package util

import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}
import play.api.libs.json.JsString
import util.ConnectorCapability.{Prisma2Capability, RelationLinkListCapability}

import scala.concurrent.ExecutionContext

trait ApiSpecBase extends ConnectorAwareTest with BeforeAndAfterEach with BeforeAndAfterAll with PlayJsonExtensions with StringMatchers {
  self: Suite =>

  implicit val ec                 = ExecutionContext.global
  implicit lazy val implicitSuite = self
  val server                      = TestServer()
  val database                    = TestDatabase()

  def capabilities = {
    // TODO: implement Capabilities resolution
    ConnectorCapabilities.sqliteNative
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    // TODO: does the migration-engine need to perform an initialize before the tests?
//    testDependencies.deployConnector.initialize().await()
  }

  def escapeString(str: String) = JsString(str).toString()

  implicit def testDataModelsWrapper(testDataModel: TestDataModels): TestDataModelsWrapper = {
    TestDataModelsWrapper(testDataModel, connectorTag, connector, database)
  }

  val listInlineArgument = if (capabilities.has(RelationLinkListCapability)) {
    "references: [id]"
  } else {
    ""
  }

  val listInlineDirective = if (capabilities.has(RelationLinkListCapability)) {
    s"@relation($listInlineArgument)"
  } else {
    ""
  }

  val scalarListDirective = ""
//  val scalarListDirective = if (capabilities.hasNot(EmbeddedScalarListsCapability)) {
//    "@scalarList(strategy: RELATION)"
//  } else {
//    ""
//  }
}
