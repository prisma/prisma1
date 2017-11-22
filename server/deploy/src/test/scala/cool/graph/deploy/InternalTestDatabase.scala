package cool.graph.deploy

import cool.graph.deploy.database.schema.InternalDatabaseSchema
import cool.graph.util.AwaitUtils
import org.scalatest.{BeforeAndAfterAll, Suite}
import slick.jdbc.MySQLProfile.api._

trait InternalTestDatabase extends BeforeAndAfterAll with AwaitUtils { this: Suite =>
  val internalDatabaseRoot = Database.forConfig("internalRoot")

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    createInternalDatabaseSchema
  }

  private def createInternalDatabaseSchema = {
    internalDatabaseRoot.run(InternalDatabaseSchema.createSchemaActions(recreate = true)).await()
  }
}
