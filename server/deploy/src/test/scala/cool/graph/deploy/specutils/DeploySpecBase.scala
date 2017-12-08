package cool.graph.deploy.specutils

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.deploy.schema.mutations.{AddProjectInput, AddProjectMutation}
import cool.graph.shared.models.{Project, ProjectId}
import cool.graph.utils.await.AwaitUtils
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}

trait DeploySpecBase extends BeforeAndAfterEach with BeforeAndAfterAll with AwaitUtils { self: Suite =>

  implicit lazy val system           = ActorSystem()
  implicit lazy val materializer     = ActorMaterializer()
  implicit lazy val testDependencies = DeployTestDependencies()

  val server     = DeployTestServer()
  val internalDb = testDependencies.internalTestDb
  val clientDb   = testDependencies.clientTestDb

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    internalDb.createInternalDatabaseSchema()
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    internalDb.truncateTables()
    // todo do something with client db?
  }

  def setupProject(project: Project): Unit = {
    val nameAndStage = ProjectId.fromEncodedString(project.id)
    val mutation = AddProjectMutation(
      AddProjectInput(None, None, nameAndStage.name, nameAndStage.stage, Vector.empty),
      testDependencies.projectPersistence,
      testDependencies.migrationPersistence,
      clientDb.clientDatabase
    ).execute.await

  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    internalDb.shutdown()
    clientDb.shutdown() // db delete client dbs created during test?
  }
}
