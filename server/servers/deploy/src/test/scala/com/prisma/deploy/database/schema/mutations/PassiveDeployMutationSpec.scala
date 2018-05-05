package com.prisma.deploy.database.schema.mutations

import com.prisma.deploy.specutils.PassiveDeploySpecBase
import com.prisma.shared.models.Manifestations.{InlineRelationManifestation, ModelManifestation, RelationTableManifestation}
import org.scalatest.{FlatSpec, Matchers}

class PassiveDeployMutationSpec extends FlatSpec with Matchers with PassiveDeploySpecBase {
  val projectId = "passive$test"

  "a schema without any relations" should "work" in {
    val sqlSchema =
      s"""
         |CREATE TABLE list (
         |  id      SERIAL PRIMARY KEY  -- implicit primary key constraint
         |, name    text NOT NULL
         |, foo     text
         |);
       """.stripMargin

    val schema =
      """
       | type List @model(table: "list"){
       |   id: ID! @unique
       |   name: String!
       |   foo: String
       | }
     """.stripMargin

    setupProjectDatabaseForProject(projectId, sqlSchema)

    val result = server.deploySchema(projectId, schema)

    result.schema.models should have(size(1))
    val model = result.schema.models.head
    model.name should equal("List")
    model.manifestation should equal(Some(ModelManifestation("list")))
    model.fields should have(size(3))
  }

  "a schema with an explicit inline relation" should "work" in {
    val sqlSchema = s"""
     |CREATE TABLE List (
     |  id      varchar PRIMARY KEY  -- implicit primary key constraint
     |, name    text NOT NULL
     |);
     |
     |CREATE TABLE Todo (
     |  id       varchar PRIMARY KEY
     |, title     text NOT NULL
     |, list_id varchar NOT NULL REFERENCES list (id) ON UPDATE CASCADE
     |);
      """.stripMargin

    val schema =
      """
        | type List @model(table: "list"){
        |   id: ID! @unique
        |   name: String!
        |   todos: [Todo!]!
        | }
        |
        | type Todo @model(table: "todo"){
        |   id: ID! @unique
        |   title: String!
        |   list: List! @pgRelation(name: "list_id")
        | }
      """.stripMargin

    setupProjectDatabaseForProject(projectId, sqlSchema)

    val result = server.deploySchema(projectId, schema)

    result.schema.models should have(size(2))
    val todoModel = result.schema.getModelByName_!("Todo")
    todoModel.fields should have(size(3))
    result.schema.relations should have(size(1))
    val relation = result.schema.relations.head
    relation.manifestation should be(Some(InlineRelationManifestation("Todo", "list_id")))
  }

  "a schema with an inferred inline relation" should "work" in {
    val sqlSchema = s"""
      |CREATE TABLE List (
      |  id      varchar PRIMARY KEY  -- implicit primary key constraint
      |, name    text NOT NULL
      |);
      |
      |CREATE TABLE Todo (
      |  id       varchar PRIMARY KEY
      |, title     text NOT NULL
      |, list_id varchar NOT NULL REFERENCES list (id) ON UPDATE CASCADE
      |);
      """.stripMargin

    val schema =
      """
        | type List @model(table: "list"){
        |   id: ID! @unique
        |   name: String!
        |   todos: [Todo!]!
        | }
        |
        | type Todo @model(table: "todo"){
        |   id: ID! @unique
        |   title: String!
        |   list: List!
        | }
      """.stripMargin

    setupProjectDatabaseForProject(projectId, sqlSchema)

    val result = server.deploySchema(projectId, schema)

    result.schema.models should have(size(2))
    val todoModel = result.schema.getModelByName_!("Todo")
    todoModel.fields should have(size(3))
    result.schema.relations should have(size(1))
    val relation = result.schema.relations.head
    relation.manifestation should be(Some(InlineRelationManifestation("Todo", "list_id")))
  }

  "a schema with an explicit relation table relation" should "work" in {
    val sqlSchema = s"""
     |CREATE TABLE list (
     |  id      varchar PRIMARY KEY  -- implicit primary key constraint
     |, name    text NOT NULL
     |);
     |
     |CREATE TABLE todo (
     |  id       varchar PRIMARY KEY
     |, title     text NOT NULL
     |);
     |
     |CREATE TABLE TodoToList(
     |  list_id varchar NOT NULL REFERENCES list (id) ON UPDATE CASCADE,
     |  todo_id varchar NOT NULL REFERENCES todo (id) ON UPDATE CASCADE
     |)
      """.stripMargin

    val schema =
      """
        | type List @model(table: "list"){
        |   id: ID! @unique
        |   name: String!
        |   todos: [Todo!]! @pgRelationTable(table: "todotolist")
        | }
        |
        | type Todo @model(table: "todo"){
        |   id: ID! @unique
        |   title: String!
        |   list: [List!]!
        | }
      """.stripMargin

    setupProjectDatabaseForProject(projectId, sqlSchema)

    val result = server.deploySchema(projectId, schema)

    result.schema.models should have(size(2))
    result.schema.relations should have(size(1))
    val relation = result.schema.relations.head
    relation.modelAId should be("List")
    relation.modelBId should be("Todo")
    relation.manifestation should be(Some(RelationTableManifestation(table = "todotolist", modelAColumn = "todo_id", modelBColumn = "list_id")))
  }

  "a schema with an inferred relation table relation" should "work" in {
    val sqlSchema = s"""
     |CREATE TABLE list (
     |  id      varchar PRIMARY KEY  -- implicit primary key constraint
     |, name    text NOT NULL
     |);
     |
     |CREATE TABLE todo (
     |  id       varchar PRIMARY KEY
     |, title     text NOT NULL
     |);
     |
     |CREATE TABLE TodoToList(
     |  list_id varchar NOT NULL REFERENCES list (id) ON UPDATE CASCADE,
     |  todo_id varchar NOT NULL REFERENCES todo (id) ON UPDATE CASCADE
     |)
      """.stripMargin

    val schema =
      """
        | type List @model(table: "list"){
        |   id: ID! @unique
        |   name: String!
        |   todos: [Todo!]!
        | }
        |
        | type Todo @model(table: "todo"){
        |   id: ID! @unique
        |   title: String!
        |   list: [List!]!
        | }
      """.stripMargin

    setupProjectDatabaseForProject(projectId, sqlSchema)

    val result = server.deploySchema(projectId, schema)

    result.schema.models should have(size(2))
    result.schema.relations should have(size(1))
    val relation = result.schema.relations.head
    relation.modelAId should be("List")
    relation.modelBId should be("Todo")
    relation.manifestation should be(Some(RelationTableManifestation(table = "todotolist", modelAColumn = "todo_id", modelBColumn = "list_id")))
  }

  "a sdl schema with an inline relation that does not match the db schema" should "return a error" in {
    val sqlSchema = s"""
     |CREATE TABLE list (
     |  id      varchar PRIMARY KEY  -- implicit primary key constraint
     |, name    text NOT NULL
     |);
     |
     |CREATE TABLE todo (
     |  id       varchar PRIMARY KEY
     |, title     text NOT NULL
     |);
     |
     |CREATE TABLE TodoToTodo(
     |  todo_id varchar NOT NULL REFERENCES todo (id) ON UPDATE CASCADE,
     |  todo_id2 varchar NOT NULL REFERENCES todo (id) ON UPDATE CASCADE
     |)
      """.stripMargin

    val schema =
      """
        | type List @model(table: "list"){
        |   id: ID! @unique
        |   name: String!
        |   todos: [Todo!]!
        | }
        |
        | type Todo @model(table: "todo"){
        |   id: ID! @unique
        |   title: String!
        |   list: [List!]!
        | }
      """.stripMargin

    setupProjectDatabaseForProject(projectId, sqlSchema)

    val result = server.deploySchemaThatMustError(projectId, schema)

    println(result)
  }
}
