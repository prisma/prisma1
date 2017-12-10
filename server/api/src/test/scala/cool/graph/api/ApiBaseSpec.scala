package cool.graph.api

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.api.database.DataResolver
import cool.graph.shared.models.Project
import cool.graph.util.json.SprayJsonExtensions
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}

trait ApiBaseSpec extends BeforeAndAfterEach with BeforeAndAfterAll with SprayJsonExtensions { self: Suite =>

  implicit lazy val system           = ActorSystem()
  implicit lazy val materializer     = ActorMaterializer()
  implicit lazy val testDependencies = new ApiDependenciesForTest
  val server                         = ApiTestServer()
  val database                       = ApiTestDatabase()

  //def dataResolver(project: Project): DataResolver = DataResolver(project = project)

  override protected def afterAll(): Unit = {
    super.afterAll()
    testDependencies.destroy
  }
}
