package com.prisma.deploy.migration
import com.prisma.ConnectorTag
import com.prisma.ConnectorTag.SQLiteConnectorTag
import com.prisma.deploy.specutils.PassiveDeploySpecBase
import com.prisma.shared.models.ConnectorCapability.{IntIdCapability, MigrationsCapability}
import com.prisma.shared.models.{ConnectorCapabilities, ConnectorCapability, TypeIdentifier}
import org.scalatest.{Matchers, WordSpecLike}

class ExistingDatabasesSpec extends WordSpecLike with Matchers with PassiveDeploySpecBase {
  val TI = TypeIdentifier

  override def doNotRunForCapabilities: Set[ConnectorCapability] = Set.empty
  override def runOnlyForCapabilities                            = Set(MigrationsCapability)
  override def doNotRunForConnectors: Set[ConnectorTag]          = Set(SQLiteConnectorTag)

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

    val initialResult = setup(SQLs(postgres = postgres, mysql = mysql, sqlite = ""))

    val dataModel =
      s"""
         |type Blog @db(name: "blog"){
         |  id: Int! @id
         |}
       """.stripMargin

    val result = deploy(dataModel, ConnectorCapabilities(IntIdCapability))

    result should equal(initialResult)
  }

  "bigint columns should work" in {
    val postgres =
      s"""
         | CREATE TABLE blog (
         |   id bigint  -- implicit primary key constraint
         |);
       """.stripMargin
    val mysql =
      s"""
         | CREATE TABLE blog (
         |   id bigint NOT NULL,
         |   PRIMARY KEY(id)
         | );
       """.stripMargin

    val initialResult = setup(SQLs(postgres = postgres, mysql = mysql, sqlite = ""))

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
    val result        = executeSql(SQLs(postgres = dropPostTable, mysql = dropPostTable, sqlite = dropPostTable))
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

    val initialResult = setup(SQLs(postgres = postgres, mysql = mysql, sqlite = ""))

    val dataModel =
      s"""
         |type Blog @db(name: "blog"){
         |  id: Int! @id
         |  title: String
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

    val initialResult = setup(SQLs(postgres = postgres, mysql = mysql, sqlite = ""))
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

    val initialResult = setup(SQLs(postgres = postgres, mysql = mysql, sqlite = ""))
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

  "creating a field for an existing column and simultaneously making it required should work" in {
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

    val initialResult = setup(SQLs(postgres = postgres, mysql = mysql, sqlite = ""))
    val tableBefore   = initialResult.table_!("blog")
    tableBefore.column_!("title").isRequired should be(false)

    val dataModel =
      s"""
         |type Blog @db(name: "blog"){
         |  id: Int! @id
         |  title: Int!
         |}
       """.stripMargin

    val result     = deploy(dataModel, ConnectorCapabilities(IntIdCapability))
    val tableAfter = result.table_!("blog")
    tableAfter.column_!("title").isRequired should be(true)
  }

  "creating a field for an existing column and simultaneously removing the unique constraint should work" in {
    val postgres =
      s"""
         | CREATE TABLE blog (
         |   id SERIAL PRIMARY KEY,
         |   title int
         |);
         |CREATE UNIQUE INDEX title_index ON blog(title ASC);
       """.stripMargin

    val mysql =
      s"""
         | CREATE TABLE blog (
         |   id int NOT NULL,
         |   title int,
         |   PRIMARY KEY(id),
         |   UNIQUE INDEX (title ASC)
         | );
       """.stripMargin

    val initialResult = setup(SQLs(postgres = postgres, mysql = mysql, sqlite = ""))
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

    val dropTitleColumn = "ALTER TABLE blog DROP COLUMN title;"
    val result          = executeSql(SQLs(postgres = dropTitleColumn, mysql = dropTitleColumn, sqlite = dropTitleColumn))
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

    val dropTitleColumn = "ALTER TABLE blog DROP COLUMN title;"
    executeSql(SQLs(postgres = dropTitleColumn, mysql = dropTitleColumn, sqlite = dropTitleColumn))

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

    val renameTitleColumn = "ALTER TABLE blog RENAME COLUMN title TO new_title;"
    val result            = executeSql(SQLs(postgres = renameTitleColumn, mysql = renameTitleColumn, sqlite = renameTitleColumn))
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
}
