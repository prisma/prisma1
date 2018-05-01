package com.prisma.api.mutations

import com.prisma.api.ApiSpecBase
import com.prisma.deploy.connector.postgresql.PostgresDeployConnector
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class PassiveConnectorSpec extends FlatSpec with Matchers with ApiSpecBase {
  //
  val projectId = "passive_test"

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

  val inlineRelationProject = SchemaDsl.fromString(id = projectId, withReservedFields = false) {
    """
      | type List @model(table: "list"){
      |   id: String!
      |   name: String!
      |   todos: [Todo]
      | }
      |
      | type Todo @model(table: "todo"){
      |   id: String!
      |   title: String!
      |   list: List @field(column: "list_id")
      | }
    """.stripMargin
  }

  def executeOnInternalDatabase(sql: String) = {
    val connector = testDependencies.deployConnector.asInstanceOf[PostgresDeployConnector]
    val session   = connector.internalDatabase.createSession()
    val statement = session.createStatement()
    statement.execute(sql)
    session.close()
  }

  "A Create Mutation" should "create and return item" in {
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
//
//    val queryRes = server.query("""{ scalarModels{optString, optInt, optFloat, optBoolean, optEnum, optDateTime, optJson}}""", project = project)
//
//    queryRes.toString should be(
//      s"""{"data":{"scalarModels":[{"optJson":[1,2,3],"optInt":1337,"optBoolean":true,"optDateTime":"2016-07-31T23:59:01.000Z","optString":"lala${TroubleCharacters.value}","optEnum":"A","optFloat":1.234}]}}""")
  }

  "A Create Mutation" should "created nested items" in {
    executeOnInternalDatabase(inlineRelationSchema)
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
      project = inlineRelationProject
    )
    res.toString should be(s"""{"data":{"createTodo":{"title":"the todo"}}}""")
  }

  "A Create Mutation" should "created nested items 2" in {
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

  "the connector" should "support diverging names for models/tables and fields/columns" in {
    executeOnInternalDatabase(inlineRelationSchema)
    val project = SchemaDsl.fromString(id = projectId, withReservedFields = false) {
      """
        | type List @model(table: "list"){
        |   id: String!
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
