package com.prisma.deploy.migration

import com.prisma.deploy.connector.Tables
import com.prisma.deploy.specutils.DeploySpecBase
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{Matchers, WordSpecLike}

class MigrationsSpec extends WordSpecLike with Matchers with DeploySpecBase {

  val name      = this.getClass.getSimpleName
  val stage     = "default"
  val serviceId = s"$name@$stage"
  val initialDataModel =
    """
      |type A {
      |  id: ID! @id
      |}
    """.stripMargin
  val inspector = deployConnector.testFacilities.inspector

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    val (project, _) = setupProject(initialDataModel)
  }

  "adding a scalar field should work" ignore {
    val dataModel =
      """
        |type A {
        |  id: ID! @id
        |  field: String
        |}
      """.stripMargin

    server.deploySchema(serviceId, dataModel)

    val result = inspect
    val column = result.table_!("A").column_!("field")
    column.tpe should be("text")
  }

  def inspect: Tables = {
    deployConnector.testFacilities.inspector.inspect(serviceId).await()
  }
}
