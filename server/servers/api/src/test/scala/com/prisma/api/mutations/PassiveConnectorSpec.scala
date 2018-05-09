package com.prisma.api.mutations

import com.prisma.IgnoreActive
import com.prisma.api.ApiSpecBase
import com.prisma.deploy.connector.postgresql.PostgresDeployConnector
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

trait PassiveConnectorSpec extends FlatSpec with Matchers with ApiSpecBase {
  val projectId = "passive_test"

  override def runSuiteOnlyForPassiveConnectors = true

  def executeOnInternalDatabase(sql: String) = {
    val connector = testDependencies.deployConnector.asInstanceOf[PostgresDeployConnector]
    val session   = connector.internalDatabase.createSession()
    val statement = session.createStatement()
    statement.execute(sql)
    session.close()
  }

}

class PassiveConnectorSpecForInlineRelations extends PassiveConnectorSpec {

  val inlineRelationSchema = s"""
                                |DROP SCHEMA IF EXISTS $projectId CASCADE;
                                |CREATE SCHEMA $projectId;
                                |CREATE TABLE $projectId.list (
                                |  id      varchar PRIMARY KEY  -- implicit primary key constraint
                                |, name    text NOT NULL
                                |);
                                |
                                |CREATE TABLE $projectId.todo (
                                |  id       varchar PRIMARY KEY
                                |, title     text NOT NULL
                                |, list_id varchar NOT NULL REFERENCES $projectId.list (id) ON UPDATE CASCADE
                                |);
      """.stripMargin

  lazy val inlineRelationProject = SchemaDsl.fromPassiveConnectorSdl(id = projectId, testDependencies.deployConnector) {
    """
      | type List @model(table: "list"){
      |   id: ID!
      |   name: String!
      |   todos: [Todo]
      | }
      |
      | type Todo @model(table: "todo"){
      |   id: ID!
      |   title: String!
      |   list: List @pgRelation(column: "list_id")
      | }
    """.stripMargin
  }

  "A Create Mutation" should "create and return item" taggedAs (IgnoreActive) in {
    executeOnInternalDatabase(inlineRelationSchema)
    val res = server.query(
      s"""mutation {
         |  createList(data: {
         |    name: "the list name"
         |  }){ name }
         |}""".stripMargin,
      project = inlineRelationProject
    )
    res.toString should be(s"""{"data":{"createList":{"name":"the list name"}}}""")
  }

  "A Create Mutation" should "created nested items" taggedAs (IgnoreActive) ignore {
    executeOnInternalDatabase(inlineRelationSchema)
    // TODO: how do we implement this? We would have to reorder in this case?
    val res = server.query(
      s"""mutation {
         |  createTodo(data: {
         |    title: "the todo"
         |    list: {
         |      create: { name: "the list" }
         |    }
         |  }){ title }
         |}""".stripMargin,
      project = inlineRelationProject
    )
    res.toString should be(s"""{"data":{"createTodo":{"title":"the todo"}}}""")
  }

  "A Create Mutation" should "created nested items 2" taggedAs (IgnoreActive) in {
    executeOnInternalDatabase(inlineRelationSchema)
    val res = server.query(
      s"""mutation {
         |  createList(data: {
         |    name: "the list"
         |    todos: {
         |      create: [{ title: "the list" }]
         |    }
         |  }){ name }
         |}""".stripMargin,
      project = inlineRelationProject
    )
    res.toString should be(s"""{"data":{"createList":{"name":"the list"}}}""")
  }

  "the connector" should "support diverging names for models/tables and fields/columns" taggedAs (IgnoreActive) in {
    executeOnInternalDatabase(inlineRelationSchema)
    val project = SchemaDsl.fromPassiveConnectorSdl(id = projectId, testDependencies.deployConnector) {
      """
        | type List @model(table: "list"){
        |   id: ID!
        |   theName: String! @field(column: "name")
        | }
      """.stripMargin
    }
    val res = server.query(
      s"""mutation {
         |  createList(data: {
         |    theName: "the list"
         |  }){ theName }
         |}""".stripMargin,
      project = project
    )
    res.toString should be(s"""{"data":{"createList":{"theName":"the list"}}}""")
  }
}

class PassiveConnectorSpecForTableRelations extends FlatSpec with PassiveConnectorSpec with Matchers with ApiSpecBase {
  val relationTableSchema =
    s"""
       |DROP SCHEMA IF EXISTS $projectId CASCADE;
       |CREATE SCHEMA $projectId;
       |CREATE TABLE $projectId.list (
       |  id      varchar PRIMARY KEY  -- implicit primary key constraint
       |, name    text NOT NULL
       |);
       |
       |CREATE TABLE $projectId.todo (
       |  id       varchar PRIMARY KEY
       |, title     text NOT NULL
       |);
       |
       |CREATE TABLE $projectId.list_to_todo (
       |  list_id varchar REFERENCES $projectId.list (id) ON UPDATE CASCADE ON DELETE CASCADE
       |, todo_id varchar REFERENCES $projectId.todo (id) ON UPDATE CASCADE ON DELETE CASCADE
       |);
     """.stripMargin

  lazy val relationTableProject = SchemaDsl.fromPassiveConnectorSdl(id = projectId, testDependencies.deployConnector) {
    """
      | type List @model(table: "list"){
      |   id: ID!
      |   name: String!
      |   todos: [Todo] @pgRelationTable(table: "list_to_todo", relationColumn: "list_id", targetColumn: "todo_id")
      | }
      |
      | type Todo @model(table: "todo"){
      |   id: ID!
      |   title: String!
      |   list: List
      | }
    """.stripMargin
  }

  "A Create Mutation" should "create and return item" taggedAs (IgnoreActive) in {
    executeOnInternalDatabase(relationTableSchema)
    val res = server.query(
      s"""mutation {
         |  createList(data: {
         |    name: "the list name"
         |  }){ name }
         |}""".stripMargin,
      project = relationTableProject
    )
    res.toString should be(s"""{"data":{"createList":{"name":"the list name"}}}""")
  }

  "A Create Mutation" should "created nested items" taggedAs (IgnoreActive) in {
    executeOnInternalDatabase(relationTableSchema)
    // how do we implement this? We would have to reorder in this case?
    val res = server.query(
      s"""mutation {
         |  createTodo(data: {
         |    title: "the todo"
         |    list: {
         |      create: { name: "the list" }
         |    }
         |  }){ title }
         |}""".stripMargin,
      project = relationTableProject
    )
    res.toString should be(s"""{"data":{"createTodo":{"title":"the todo"}}}""")
  }
}

class PassiveConnectorSpecForAutoGeneratedIds extends FlatSpec with PassiveConnectorSpec with Matchers with ApiSpecBase {
  val sqlSchema =
    s"""
       |DROP SCHEMA IF EXISTS $projectId CASCADE;
       |CREATE SCHEMA $projectId;
       |CREATE TABLE $projectId.list (
       |  id      SERIAL PRIMARY KEY  -- implicit primary key constraint
       |, name    text NOT NULL
       |, foo     text
       |);
     """.stripMargin

  lazy val project = SchemaDsl.fromPassiveConnectorSdl(id = projectId, testDependencies.deployConnector) {
    """
      | type List @model(table: "list"){
      |   id: Int!
      |   name: String!
      |   foo: String
      | }
    """.stripMargin
  }

  "A Create Mutation" should "create and return item" taggedAs (IgnoreActive) in {
    executeOnInternalDatabase(sqlSchema)
    val res1 = server.query(
      s"""mutation {
         |  createList(data: {
         |    name: "the list name"
         |  }){ id, name }
         |}""".stripMargin,
      project = project
    )
    res1.toString should be(s"""{"data":{"createList":{"id":1,"name":"the list name"}}}""")

    val res2 = server.query(
      s"""mutation {
         |  createList(data: {
         |    name: "the list name"
         |  }){ id, name }
         |}""".stripMargin,
      project = project
    )
    res2.toString should be(s"""{"data":{"createList":{"id":2,"name":"the list name"}}}""")
  }
}
