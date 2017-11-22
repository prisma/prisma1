package cool.graph.deploy.database.persistence

import cool.graph.deploy.InternalTestDatabase
import cool.graph.shared.models.MigrationSteps
import cool.graph.shared.project_dsl.TestProject
import cool.graph.util.AwaitUtils
import org.scalatest.{FlatSpec, Matchers}
import slick.jdbc.MySQLProfile.api._

class ProjectPersistenceImplSpec extends FlatSpec with Matchers with AwaitUtils with InternalTestDatabase {
  import scala.concurrent.ExecutionContext.Implicits.global

  val internalDatabase   = Database.forConfig("internal")
  val projectPersistence = ProjectPersistenceImpl(internalDatabase = internalDatabase)

  val project                        = TestProject()
  val migrationSteps: MigrationSteps = null

  ".load()" should "return None if there's no project yet in the database" in {
    val result = await(projectPersistence.load("non-existent-id"), 20)
    result should be(None)
  }

  ".save()" should "store the project in the db" in {
    val result = await(projectPersistence.save(project, migrationSteps))
  }
}
