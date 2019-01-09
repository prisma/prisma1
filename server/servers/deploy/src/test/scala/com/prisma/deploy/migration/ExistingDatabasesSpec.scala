package com.prisma.deploy.migration
import com.prisma.api.connector.jdbc.impl.JdbcDatabaseMutactionExecutor
import com.prisma.deploy.connector.DatabaseSchema
import com.prisma.deploy.connector.jdbc.database.JdbcDeployMutactionExecutor
import com.prisma.deploy.specutils.{DataModelV2Base, DeploySpecBase, PassiveDeploySpecBase}
import com.prisma.shared.models.ConnectorCapability.IntIdCapability
import com.prisma.shared.models.{ConnectorCapabilities, ConnectorCapability}
import org.scalatest.{Matchers, WordSpecLike}

class ExistingDatabasesSpec extends WordSpecLike with Matchers with PassiveDeploySpecBase with DataModelV2Base {
  case class ExistingSchema(postgres: String, mysql: String)

  override def doNotRunForCapabilities: Set[ConnectorCapability] = Set.empty
  lazy val slickDatabase                                         = deployConnector.deployMutactionExecutor.asInstanceOf[JdbcDeployMutactionExecutor].slickDatabase

  "adding a type for an existing table should work" in {
    val postgres =
      s"""
         | CREATE TABLE blog (
         |   id SERIAL PRIMARY KEY  -- implicit primary key constraint
         |);
       """.stripMargin
    val mysql =
      s"""
         | CREATE TABLE blog (
         |   id int NOT NULL,
         |   PRIMARY KEY(id)
         | );
       """.stripMargin
    setup(ExistingSchema(postgres = postgres, mysql = mysql))
    val initialResult = inspect

    val dataModel =
      s"""
         |type Blog @db(name: "blog"){
         |  id: Int! @id
         |}
       """.stripMargin

    val result = deploy(dataModel, ConnectorCapabilities(IntIdCapability))

    result should equal(initialResult)
  }

  def setup(existingSchema: ExistingSchema): Unit = {
    deployConnector.deleteProjectDatabase(projectId).await()
    if (slickDatabase.isMySql) {
      setupProjectDatabaseForProject(existingSchema.mysql)
    } else if (slickDatabase.isPostgres) {
      setupProjectDatabaseForProject(existingSchema.postgres)
    } else {
      sys.error("This is neither Postgres nor MySQL")
    }
  }
}
