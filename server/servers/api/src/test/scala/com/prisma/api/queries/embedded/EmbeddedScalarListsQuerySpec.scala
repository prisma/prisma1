package com.prisma.api.queries.embedded

import com.prisma.IgnoreMongo
import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability.{EmbeddedScalarListsCapability, EmbeddedTypesCapability}
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class EmbeddedScalarListsQuerySpec extends FlatSpec with Matchers with ApiSpecBase {

  override def runOnlyForCapabilities = Set(EmbeddedScalarListsCapability, EmbeddedTypesCapability)

  "Nested scalar lists" should "work in creates " in {

    val project = SchemaDsl.fromString() {
      s"""type List{
         |   id: ID! @unique
         |   todos: [Todo]
         |   listInts: [Int]
         |}
         |
         |type Todo @embedded{
         |   todoInts: [Int]
         |}"""
    }

    database.setup(project)

    server.query(
      s"""mutation{createList(data: {listInts: {set: [1, 2]}, todos: {create: {todoInts: {set: [3, 4]}}}}) {id}}""".stripMargin,
      project
    )

    val result = server.query(s"""query{lists {listInts, todos {todoInts}}}""".stripMargin, project)

    result.toString should equal("""{"data":{"lists":[{"listInts":[1,2],"todos":[{"todoInts":[3,4]}]}]}}""")
  }

  "Deeply nested scalar lists" should "work in creates " in {

    val project = SchemaDsl.fromString() {
      s"""type List {
         |   id: ID! @unique
         |   todo: Todo
         |   listInts: [Int]
         |}
         |
         |type Todo @embedded {
         |   tag: Tag
         |   todoInts: [Int]
         |}
         |
         |type Tag @embedded {
         |   tagInts: [Int]
         |}
         |"""
    }

    database.setup(project)

    server.query(
      s"""mutation{createList(data: {listInts: {set: [1, 2]}, todo: {create: {todoInts: {set: [3, 4]}, tag: {create: {tagInts: {set: [5, 6]}}}}}}) {id}}""".stripMargin,
      project
    )

    val result = server.query(s"""query{lists {listInts, todo {todoInts, tag {tagInts}}}}""".stripMargin, project)

    result.toString should equal("""{"data":{"lists":[{"listInts":[1,2],"todo":{"todoInts":[3,4],"tag":{"tagInts":[5,6]}}}]}}""")
  }

  "Deeply nested scalar lists" should "work in updates " in {

    val project = SchemaDsl.fromString() {
      s"""type List{
         |   id: ID! @unique
         |   todo: Todo
         |   uList: String! @unique
         |   listInts: [Int]
         |}
         |
         |type Todo @embedded {
         |   uTodo: String! @unique
         |   tag: Tag
         |   todoInts: [Int]
         |}
         |
         |type Tag @embedded {
         |   uTag: String! @unique
         |   tagInts: [Int]
         |}
         |"""
    }

    database.setup(project)

    server
      .query(
        s"""mutation{createList(data: {uList: "A", listInts: {set: [1, 2]}, todo: {create: {uTodo: "B", todoInts: {set: [3, 4]}, tag: {create: {uTag: "C",tagInts: {set: [5, 6]}}}}}}) {id}}""".stripMargin,
        project
      )

    server.query(
      s"""mutation{updateList(where: {uList: "A"}
         |                    data: {listInts: {set: [7, 8]},
         |                           todo: {update: {todoInts: {set: [9, 10]},
         |                                           tag: {update: { tagInts: {set: [11, 12]}}}}}}) {id}}""".stripMargin,
      project
    )
    val result = server.query(s"""query{lists {listInts, todo {todoInts, tag {tagInts}}}}""".stripMargin, project)

    result.toString should equal("""{"data":{"lists":[{"listInts":[7,8],"todo":{"todoInts":[9,10],"tag":{"tagInts":[11,12]}}}]}}""")
  }

  "Nested scalar lists" should "work in upserts and only execute one branch of the upsert" in {

    val project = SchemaDsl.fromString() {
      s"""type List{
         |   id: ID! @unique
         |   todo: Todo
         |   uList: String! @unique
         |   listInts: [Int]
         |}
         |
         |type Todo @embedded {
         |   uTodo: String! @unique
         |   todoInts: [Int]
         |}
         |"""
    }

    database.setup(project)

    server
      .query(
        s"""mutation{createList(data: {uList: "A", listInts: {set: [1, 2]}, todo: {create: {uTodo: "B", todoInts: {set: [3, 4]}}}}) {id}}""".stripMargin,
        project
      )
      .pathAsString("data.createList.id")

    server
      .query(
        s"""mutation upsertListValues {upsertList(
        |                             where:{uList: "A"}
	      |                             create:{uList:"Should Not Matter" listInts:{set: [75, 85]}}
	      |                             update:{listInts:{set: [70, 80]} }
        |){id}}""".stripMargin,
        project
      )
      .pathAsString("data.upsertList.id")

    val result = server.query(s"""query{lists {uList, listInts}}""".stripMargin, project)

    result.toString should equal("""{"data":{"lists":[{"uList":"A","listInts":[70,80]}]}}""")
  }
}
