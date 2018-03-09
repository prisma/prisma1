package com.prisma.deploy.specutils

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.prisma.auth.AuthImpl
import com.prisma.deploy.DeployDependencies
import com.prisma.deploy.database.{ClientDbQueries, EmptyClientDbQueries, FullClientDbQueries}
import com.prisma.deploy.migration.validation.SchemaError
import com.prisma.deploy.schema.mutations.{FunctionInput, FunctionValidator}
import com.prisma.deploy.server.DummyClusterAuth
import com.prisma.errors.{BugsnagErrorReporter, ErrorReporter}
import com.prisma.messagebus.pubsub.inmemory.InMemoryAkkaPubSub
import com.prisma.shared.models.Project

case class DeployTestDependencies()(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends DeployDependencies {
  override implicit def self: DeployDependencies = this

  implicit val reporter: ErrorReporter = BugsnagErrorReporter(sys.env.getOrElse("BUGSNAG_API_KEY", ""))

  val internalTestDb = new InternalTestDatabase()
  val clientTestDb   = new ClientTestDatabase()

  override def clientDbQueries(project: Project): ClientDbQueries = FullClientDbQueries // else EmptyClientDbQueries

  override lazy val internalDb = internalTestDb.internalDatabase
  override lazy val clientDb   = clientTestDb.clientDatabase

  override lazy val migrator              = TestMigrator(clientDb, internalDb, migrationPersistence)
  override lazy val clusterAuth           = DummyClusterAuth()
  override lazy val invalidationPublisher = InMemoryAkkaPubSub[String]()

  override def apiAuth = AuthImpl
  override def functionValidator: FunctionValidator = new FunctionValidator {
    override def validateFunctionInput(project: Project, fn: FunctionInput): Vector[SchemaError] = {
      if (fn.name == "failing") Vector(SchemaError(`type` = "model", field = "field", description = "error")) else Vector.empty
    }
  }
}
