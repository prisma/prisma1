package com.prisma.deploy.specutils

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.prisma.auth.AuthImpl
import com.prisma.errors.{BugsnagErrorReporter, ErrorReporter}
import com.prisma.deploy.DeployDependencies
import com.prisma.deploy.migration.validation.SchemaError
import com.prisma.deploy.schema.mutations.{FunctionInput, FunctionValidator}
import com.prisma.deploy.server.DummyClusterAuth
import com.prisma.graphql.GraphQlClient
import com.prisma.messagebus.pubsub.inmemory.InMemoryAkkaPubSub
import com.prisma.shared.models.Project
import play.api.libs.json.{JsArray, JsObject, JsString}

import scala.concurrent.Future

case class DeployTestDependencies()(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends DeployDependencies {
  override implicit def self: DeployDependencies = this

  implicit val reporter: ErrorReporter = BugsnagErrorReporter(sys.env.getOrElse("BUGSNAG_API_KEY", ""))

  val internalTestDb = new InternalTestDatabase()
  val clientTestDb   = new ClientTestDatabase()

  override lazy val internalDb = internalTestDb.internalDatabase
  override lazy val clientDb   = clientTestDb.clientDatabase

  override lazy val migrator              = TestMigrator(clientDb, internalDb, migrationPersistence)
  override lazy val clusterAuth           = DummyClusterAuth()
  override lazy val invalidationPublisher = InMemoryAkkaPubSub[String]()

  override lazy val graphQlClient = {
    val port = sys.props.getOrElse("STUB_SERVER_PORT", sys.error("No running stub server detected! Can't instantiate GraphQlClient."))
    GraphQlClient(s"http://localhost:$port")
  }

  override def apiAuth = AuthImpl
  override def functionValidator: FunctionValidator = new FunctionValidator {
    override def validateFunctionInput(project: Project, fn: FunctionInput): Future[Vector[SchemaError]] = {
      if (fn.name == "failing") { Future.successful(Vector(SchemaError(`type` = "model", field = "field", description = "error"))) } else {
        Future.successful(Vector.empty)

      }
    }
  }
}
