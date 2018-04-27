package com.prisma.api.mutations

import com.prisma.api.ApiSpecBase
import com.prisma.deploy.connector.postgresql.PostgresDeployConnector
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class PassiveConnectorSpec extends FlatSpec with Matchers with ApiSpecBase {
  //
  val projectId = "passive_test"

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    val connector = testDependencies.deployConnector.asInstanceOf[PostgresDeployConnector]
    val session   = connector.internalDatabase.createSession()
    val statement = session.createStatement()
    statement.execute(s"""
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
      """.stripMargin)
    session.close()
  }
  val project = SchemaDsl.fromString(id = projectId, withReservedFields = false) {
    """
      | type List {
      |   id: String!
      |   name: String!
      |   todos: [Todo]
      | }
      |
      | type Todo {
      |   id: String!
      |   title: String!
      |   list: List
      | }
    """.stripMargin
  }

  "A Create Mutation" should "create and return item" in {
    val res = server.query(
      s"""mutation {
         |  createList(data: {
         |    name: "the list name"
         |  }){ name }
         |}""".stripMargin,
      project = project
    )
    res.toString should be(s"""{"data":{"createList":{"name":"the list name"}}}""")
//
//    val queryRes = server.query("""{ scalarModels{optString, optInt, optFloat, optBoolean, optEnum, optDateTime, optJson}}""", project = project)
//
//    queryRes.toString should be(
//      s"""{"data":{"scalarModels":[{"optJson":[1,2,3],"optInt":1337,"optBoolean":true,"optDateTime":"2016-07-31T23:59:01.000Z","optString":"lala${TroubleCharacters.value}","optEnum":"A","optFloat":1.234}]}}""")
  }

  "A Create Mutation" should "created nested items" in {
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
      project = project
    )
    res.toString should be(s"""{"data":{"createTodo":{"title":"the todo"}}}""")
  }

  "A Create Mutation" should "created nested items 2" in {
    val res = server.query(
      s"""mutation {
         |  createList(data: {
         |    name: "the list"
         |    todos: {
         |      create: [{ title: "the list" }]
         |    }
         |  }){ name }
         |}""".stripMargin,
      project = project
    )
    res.toString should be(s"""{"data":{"createList":{"name":"the list"}}}""")
  }
}
