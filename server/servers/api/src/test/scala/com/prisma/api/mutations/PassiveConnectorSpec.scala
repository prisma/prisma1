package com.prisma.api.mutations

import com.prisma.IgnoreActive
import com.prisma.api.ApiSpecBase
import com.prisma.deploy.connector.postgres.PostgresDeployConnector
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

trait PassiveConnectorSpec extends FlatSpec with Matchers with ApiSpecBase {
  val schema = "passive_test"

  override def runSuiteOnlyForPassiveConnectors = true

  def executeOnInternalDatabase(sql: String) = {
    val connector = testDependencies.deployConnector.asInstanceOf[PostgresDeployConnector]
    val session   = connector.managementDatabase.createSession()
    val statement = session.createStatement()
    statement.execute(sql)
    session.close()
  }

}

class PassiveConnectorSpecForInlineRelations extends PassiveConnectorSpec {

  val inlineRelationSchema = s"""
                                |DROP SCHEMA IF EXISTS $schema CASCADE;
                                |CREATE SCHEMA $schema;
                                |CREATE TABLE $schema.list (
                                |  id      varchar PRIMARY KEY  -- implicit primary key constraint
                                |, name    text NOT NULL
                                |);
                                |
                                |CREATE TABLE $schema.user (
                                |  id      varchar PRIMARY KEY  -- implicit primary key constraint
                                |, name    text NOT NULL
                                |);
                                |
                                |CREATE TABLE $schema.todo (
                                |  id       varchar PRIMARY KEY
                                |, title     text NOT NULL
                                |, list_id varchar REFERENCES $schema.list (id) ON UPDATE CASCADE
                                |, user_id varchar REFERENCES $schema.user (id) ON UPDATE CASCADE
                                |);
      """.stripMargin

  lazy val inlineRelationProject = SchemaDsl.fromPassiveConnectorSdl(testDependencies.deployConnector, id = schema) {
    """
      | type List @pgTable(name: "list"){
      |   id: ID! @unique
      |   name: String!
      |   todos: [Todo!]!
      | }
      |
      | type Todo @pgTable(name: "todo"){
      |   id: ID! @unique
      |   title: String!
      |   list: List @pgRelation(column: "list_id")
      |   user: MyUser @pgRelation(column: "user_id")
      | }
      |
      | type MyUser @pgTable(name: "user"){ # it's called MyUser so that the type is on the right side of the relation to ensure a bug is not there
      |   id: ID! @unique
      |   name: String!
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

  "Expanding 2 inline relations on a type" should "work" taggedAs (IgnoreActive) in {
    executeOnInternalDatabase(inlineRelationSchema)
    val userId = server
      .query(
        s"""mutation {
         |    createMyUser(data: {
         |      name: "the user"
         |    }){ id }
         |}""".stripMargin,
        project = inlineRelationProject
      )
      .pathAsString("data.createMyUser.id")

    server.query(
      s"""mutation {
         |  createList(data: {
         |    name: "the list"
         |    todos: {
         |      create: [{
         |         title: "todo"
         |         user: {
         |           connect: { id: "$userId" }
         |         }
         |      }]
         |    }
         |  }){ name }
         |}""".stripMargin,
      project = inlineRelationProject
    )

    val res = server.query(
      s"""{
         |  todoes {
         |    title
         |    list { name }
         |    user { name }
         |  }
         |}""".stripMargin,
      project = inlineRelationProject
    )
    res should be(s"""{"data":{"todoes":[{"title":"todo","list":{"name":"the list"},"user":{"name":"the user"}}]}}""".parseJson)
  }

  "the connector" should "support diverging names for models/tables and fields/columns" taggedAs (IgnoreActive) in {
    executeOnInternalDatabase(inlineRelationSchema)
    val project = SchemaDsl.fromPassiveConnectorSdl(testDependencies.deployConnector, id = schema) {
      """
        | type List @pgTable(name: "list"){
        |   id: ID! @unique
        |   theName: String! @pgColumn(name: "name")
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
       |DROP SCHEMA IF EXISTS $schema CASCADE;
       |CREATE SCHEMA $schema;
       |CREATE TABLE $schema.list (
       |  id      varchar PRIMARY KEY  -- implicit primary key constraint
       |, name    text NOT NULL
       |);
       |
       |CREATE TABLE $schema.todo (
       |  id       varchar PRIMARY KEY
       |, title     text NOT NULL
       |);
       |
       |CREATE TABLE $schema.list_to_todo (
       |  list_id varchar REFERENCES $schema.list (id) ON UPDATE CASCADE ON DELETE CASCADE
       |, todo_id varchar REFERENCES $schema.todo (id) ON UPDATE CASCADE ON DELETE CASCADE
       |);
     """.stripMargin

  lazy val relationTableProject = SchemaDsl.fromPassiveConnectorSdl(testDependencies.deployConnector, id = schema) {
    """
      | type List @pgTable(name: "list"){
      |   id: ID! @unique
      |   name: String!
      |   todos: [Todo!]! @pgRelationTable(table: "list_to_todo", relationColumn: "list_id", targetColumn: "todo_id")
      | }
      |
      | type Todo @pgTable(name: "todo"){
      |   id: ID! @unique
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
       |DROP SCHEMA IF EXISTS $schema CASCADE;
       |CREATE SCHEMA $schema;
       |CREATE TABLE $schema.list (
       |  id      SERIAL PRIMARY KEY  -- implicit primary key constraint
       |, name    text NOT NULL
       |, foo     text
       |);
       |
       |CREATE TABLE $schema.todo (
       |  id      SERIAL PRIMARY KEY
       |, title   text NOT NULL
       |, list_id int REFERENCES $schema.list (id) ON UPDATE CASCADE
       |);
     """.stripMargin

  lazy val project = SchemaDsl.fromPassiveConnectorSdl(testDependencies.deployConnector, id = schema) {
    """
      | type List @pgTable(name: "list"){
      |   id: Int! @unique
      |   name: String!
      |   foo: String
      |   todos: [Todo!]!
      | }
      |
      | type Todo @pgTable(name: "todo"){
      |   id: Int! @unique
      |   title: String
      |   list: List @pgRelation(column: "list_id")
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

  "A nested Create" should "create and return the item" taggedAs (IgnoreActive) in {
    executeOnInternalDatabase(sqlSchema)
    val res1 = server.query(
      s"""mutation {
         |  createList(
         |    data: {
         |      name: "the list name"
         |      todos: {
         |        create: [{title: "the todo"}]
         |      }
         |    }
         |  ){
         |   id, name
         |   todos { title }
         | }
         |}""".stripMargin,
      project = project
    )
    res1.pathAsString("data.createList.todos.[0].title") should equal("the todo")
  }
}
