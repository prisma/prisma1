package com.prisma.deploy.migration
import com.prisma.api.connector.jdbc.impl.JdbcDatabaseMutactionExecutor
import com.prisma.deploy.connector.DatabaseSchema
import com.prisma.deploy.connector.jdbc.database.JdbcDeployMutactionExecutor
import com.prisma.deploy.specutils.{DataModelV2Base, DeploySpecBase, PassiveDeploySpecBase}
import com.prisma.shared.models.ConnectorCapability.{IntIdCapability, MigrationsCapability}
import com.prisma.shared.models.{ConnectorCapabilities, ConnectorCapability, TypeIdentifier}
import org.scalatest.{Matchers, WordSpecLike}

class ExistingDatabasesSpec extends WordSpecLike with Matchers with PassiveDeploySpecBase with DataModelV2Base {
  val TI = TypeIdentifier
  case class SQLs(postgres: String, mysql: String)

  override def doNotRunForCapabilities: Set[ConnectorCapability] = Set.empty
  override def runOnlyForCapabilities                            = Set(MigrationsCapability)
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

  "creating a field for an existing column (compatible type) should work" in {
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

  "creating a field for an existing column and simultaneously changing its type and unique constraint should work" in {
    val postgres =
      s"""
         | CREATE TABLE blog (
         |   id SERIAL PRIMARY KEY,
         |   title int
         |);
       """.stripMargin
    val mysql =
      s"""
         | CREATE TABLE blog (
         |   id int NOT NULL,
         |   title int,
         |   PRIMARY KEY(id)
         | );
       """.stripMargin
    val initialResult = setup(SQLs(postgres = postgres, mysql = mysql))
    val tableBefore   = initialResult.table_!("blog")
    val columnBefore  = tableBefore.column_!("title")
    columnBefore.typeIdentifier should be(TI.Int)
    columnBefore.isRequired should be(false)
    tableBefore.indexByColumns("title").isDefined should be(false)

    val dataModel =
      s"""
         |type Blog @db(name: "blog"){
         |  id: Int! @id
         |  title: String! @unique
         |}
       """.stripMargin

    val result      = deploy(dataModel, ConnectorCapabilities(IntIdCapability))
    val tableAfter  = result.table_!("blog")
    val columnAfter = tableAfter.column_!("title")
    columnAfter.typeIdentifier should be(TI.String)
    columnAfter.isRequired should be(true)
    tableAfter.indexByColumns_!("title").unique should be(true)
  }

  "creating a field for an existing column and simultaneously adding a unique constraint should work" in {
    val postgres =
      s"""
         | CREATE TABLE blog (
         |   id SERIAL PRIMARY KEY,
         |   title int
         |);
       """.stripMargin
    val mysql =
      s"""
         | CREATE TABLE blog (
         |   id int NOT NULL,
         |   title int,
         |   PRIMARY KEY(id)
         | );
       """.stripMargin
    val initialResult = setup(SQLs(postgres = postgres, mysql = mysql))
    val tableBefore   = initialResult.table_!("blog")
    tableBefore.indexByColumns("title") should be(empty)

    val dataModel =
      s"""
         |type Blog @db(name: "blog"){
         |  id: Int! @id
         |  title: String! @unique
         |}
       """.stripMargin

    val result     = deploy(dataModel, ConnectorCapabilities(IntIdCapability))
    val tableAfter = result.table_!("blog")
    tableAfter.indexByColumns_!("title").unique should be(true)
  }

  "creating a field for an existing column and simultaneously removing the unique constraint should work" in {
    val postgres =
      s"""
         | CREATE TABLE blog (
         |   id SERIAL PRIMARY KEY,
         |   title int
         |);
         |CREATE UNIQUE INDEX "title_index" ON blog(title ASC);
       """.stripMargin
    val mysql =
      s"""
         | CREATE TABLE blog (
         |   id int NOT NULL,
         |   title int,
         |   PRIMARY KEY(id)
         | );
       """.stripMargin
    val initialResult = setup(SQLs(postgres = postgres, mysql = mysql))
    val tableBefore   = initialResult.table_!("blog")
    tableBefore.indexByColumns_!("title").unique should be(true)

    val dataModel =
      s"""
         |type Blog @db(name: "blog"){
         |  id: Int! @id
         |  title: String!
         |}
       """.stripMargin

    val result     = deploy(dataModel, ConnectorCapabilities(IntIdCapability))
    val tableAfter = result.table_!("blog")
    tableAfter.indexByColumns("title") should be(empty)
  }

  "deleting a field for a non existing column should work" in {
    addProject()

    val initialDataModel =
      s"""
         |type Blog @db(name: "blog"){
         |  id: Int! @id
         |  title: String!
         |}
       """.stripMargin

    val initialResult = deploy(initialDataModel, ConnectorCapabilities(IntIdCapability))
    initialResult.table_!("blog").column("title").isDefined should be(true)

    val dropPostTable = "ALTER TABLE blog DROP COLUMN title;"
    val result        = executeSql(SQLs(postgres = dropPostTable, mysql = dropPostTable))
    result.table_!("blog").column("title").isDefined should be(false)

    val dataModel =
      s"""
         |type Blog @db(name: "blog"){
         |  id: Int! @id
         |}
       """.stripMargin

    val finalResult = deploy(dataModel, ConnectorCapabilities(IntIdCapability))

    finalResult should equal(result)
  }

  "updating a field for a non existing column should work" in {
    addProject()

    val initialDataModel =
      s"""
         |type Blog @db(name: "blog"){
         |  id: Int! @id
         |  title: String!
         |}
       """.stripMargin

    deploy(initialDataModel, ConnectorCapabilities(IntIdCapability))

    val dropPostTable = "ALTER TABLE blog DROP COLUMN title;"
    executeSql(SQLs(postgres = dropPostTable, mysql = dropPostTable))

    val dataModel =
      s"""
         |type Blog @db(name: "blog"){
         |  id: Int! @id
         |  title: Int @unique
         |}
       """.stripMargin

    val finalResult = deploy(dataModel, ConnectorCapabilities(IntIdCapability))
    val column      = finalResult.table_!("blog").column_!("title")
    val index       = finalResult.table_!("blog").indexByColumns("title")
    column.typeIdentifier should be(TI.Int)
    column.isRequired should be(false)
    index.isDefined should be(true)
    index.get.unique should be(true)
  }

  // TODO: this is probably really hard to achieve
  "renaming a field where the column was already renamed should work" ignore {
    addProject()

    val initialDataModel =
      s"""
         |type Blog @db(name: "blog"){
         |  id: Int! @id
         |  title: Int!
         |}
       """.stripMargin

    deploy(initialDataModel, ConnectorCapabilities(IntIdCapability))

    val dropPostTable = "ALTER TABLE blog RENAME COLUMN title TO new_title;"
    val result        = executeSql(SQLs(postgres = dropPostTable, mysql = dropPostTable))
    result.table_!("blog").column("title") should be(empty)

    val dataModel =
      s"""
         |type Blog @db(name: "blog"){
         |  id: Int! @id
         |  title: Float @db(name: "new_title")
         |}
       """.stripMargin

    val finalResult = deploy(dataModel, ConnectorCapabilities(IntIdCapability))
    val column      = finalResult.table_!("blog").column_!("new_title")
    column.typeIdentifier should be(TI.Float)
    column.isRequired should be(false)
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
