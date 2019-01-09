package com.prisma.deploy.migration
import com.prisma.api.connector.jdbc.impl.JdbcDatabaseMutactionExecutor
import com.prisma.deploy.connector.DatabaseSchema
import com.prisma.deploy.connector.jdbc.database.JdbcDeployMutactionExecutor
import com.prisma.deploy.specutils.{DataModelV2Base, DeploySpecBase, PassiveDeploySpecBase}
import com.prisma.shared.models.ConnectorCapability.IntIdCapability
import com.prisma.shared.models.{ConnectorCapabilities, ConnectorCapability}
import org.scalatest.{Matchers, WordSpecLike}

class ExistingDatabasesSpec extends WordSpecLike with Matchers with PassiveDeploySpecBase with DataModelV2Base {
  case class SQLs(postgres: String, mysql: String)

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
    val initialResult = setup(SQLs(postgres = postgres, mysql = mysql))

    val dataModel =
      s"""
         |type Blog @db(name: "blog"){
         |  id: Int! @id
         |}
       """.stripMargin

    val result = deploy(dataModel, ConnectorCapabilities(IntIdCapability))

    result should equal(initialResult)
  }

  "removing a type for a table that is already deleted should work" in {
    addProject()

    val initialDataModel =
      s"""
        |type Blog @db(name: "blog"){
        |  id: Int! @id
        |}
        |
        |type Post @db(name: "post"){
        |  id: ID! @id
        |}
       """.stripMargin

    val initialResult = deploy(initialDataModel, ConnectorCapabilities(IntIdCapability))
    initialResult.table("post").isDefined should be(true)

    //
    val dropPostTable = "DROP TABLE post;"
    val result        = executeSql(SQLs(postgres = dropPostTable, mysql = dropPostTable))
    result.table("post").isDefined should be(false)

    val dataModel =
      s"""
         |type Blog @db(name: "blog"){
         |  id: Int! @id
         |}
       """.stripMargin

    val finalResult = deploy(dataModel, ConnectorCapabilities(IntIdCapability))

    finalResult should equal(result)
  }

  "creating a field for an existing column should work" in {
    val postgres =
      s"""
         | CREATE TABLE blog (
         |   id SERIAL PRIMARY KEY,
         |   title text
         |);
       """.stripMargin
    val mysql =
      s"""
         | CREATE TABLE blog (
         |   id int NOT NULL,
         |   title mediumtext,
         |   PRIMARY KEY(id)
         | );
       """.stripMargin
    val initialResult = setup(SQLs(postgres = postgres, mysql = mysql))

    val dataModel =
      s"""
         |type Blog @db(name: "blog"){
         |  id: Int! @id
         |  title: String!
         |}
       """.stripMargin

    val result = deploy(dataModel, ConnectorCapabilities(IntIdCapability))
    result should equal(initialResult)
  }

  def setup(sqls: SQLs): DatabaseSchema = {
    deployConnector.deleteProjectDatabase(projectId).await()
    if (slickDatabase.isMySql) {
      setupProjectDatabaseForProject(sqls.mysql)
    } else if (slickDatabase.isPostgres) {
      setupProjectDatabaseForProject(sqls.postgres)
    } else {
      sys.error("This is neither Postgres nor MySQL")
    }
    inspect
  }

  def executeSql(sqls: SQLs): DatabaseSchema = {
    if (slickDatabase.isMySql) {
      executeSql(sqls.mysql)
    } else if (slickDatabase.isPostgres) {
      executeSql(sqls.postgres)
    } else {
      sys.error("This is neither Postgres nor MySQL")
    }
    inspect
  }
}
