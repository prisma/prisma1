package com.prisma.api.queries.embedded

import com.prisma.IgnoreMongo
import com.prisma.api.ApiSpecBase
import com.prisma.api.connector.ApiConnectorCapability.{EmbeddedScalarListsCapability, EmbeddedTypesCapability}
import com.prisma.shared.models.Project
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class EmbeddedScalarListsQuerySpec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities = Set(EmbeddedScalarListsCapability)

  "empty scalar list" should "return empty list" in {
    val project = SchemaDsl.fromString() {
      """type Model{
        |   id: ID! @unique
        |   ints: [Int!]!
        |   strings: [String!]!
        |}"""
    }

    database.setup(project)

    val id = server
      .query(
        s"""mutation {
           |  createModel(data: {strings: { set: [] }}) {
           |    id
           |    strings
           |  }
           |}""".stripMargin,
        project
      )
      .pathAsString("data.createModel.id")

    val result = server.query(
      s"""{
         |  model(where: {id:"$id"}) {
         |    ints
         |    strings
         |  }
         |}""".stripMargin,
      project
    )

    result.toString should equal("""{"data":{"model":{"ints":[],"strings":[]}}}""")
  }

  "full scalar list" should "return full list" in {

    val project = SchemaDsl.fromString() {
      """type Model{
        |   id: ID! @unique
        |   ints: [Int!]!
        |   strings: [String!]!
        |}"""
    }

    database.setup(project)

    val id = server
      .query(
        s"""mutation {
           |  createModel(data: {ints: { set: [1] }, strings: { set: ["short", "looooooooooong"]}}) {
           |    id
           |    strings
           |    ints
           |  }
           |}""".stripMargin,
        project
      )
      .pathAsString("data.createModel.id")

    val result = server.query(
      s"""{
         |  model(where: {id:"$id"}) {
         |    ints
         |    strings
         |  }
         |}""".stripMargin,
      project
    )

    result.toString should equal("""{"data":{"model":{"ints":[1],"strings":["short","looooooooooong"]}}}""")
  }

  "full scalar list" should "preserve order of elements" in {

    val project = SchemaDsl.fromString() {
      """type Model{
        |   id: ID! @unique
        |   ints: [Int!]!
        |   strings: [String!]!
        |}"""
    }
    database.setup(project)

    val id = server
      .query(
        s"""mutation {
           |  createModel(data: {ints: { set: [1,2] }, strings: { set: ["short", "looooooooooong"] }}) {
           |    id
           |  }
           |}""".stripMargin,
        project
      )
      .pathAsString("data.createModel.id")

    server
      .query(
        s"""mutation {
           |  updateModel(where: {id: "$id"} data: {ints: { set: [2,1] }}) {
           |    id
           |    ints
           |    strings
           |  }
           |}""".stripMargin,
        project
      )

    val result = server.query(
      s"""{
         |  model(where: {id:"$id"}) {
         |    ints
         |    strings
         |  }
         |}""".stripMargin,
      project
    )

    result.toString should equal("""{"data":{"model":{"ints":[2,1],"strings":["short","looooooooooong"]}}}""")
  }

  "full scalar list" should "return full list for strings" in {

    val fieldName   = "strings"
    val inputValue  = """"STRING""""
    val outputValue = """"STRING""""

    val project = SchemaDsl.fromString() {
      s"""type Model{
        |   id: ID! @unique
        |   $fieldName: [String!]!
        |}"""
    }

    verifySuccessfulSetAndRetrieval(fieldName, inputValue, outputValue, project)
  }

  "full scalar list" should "return full list for ints" in {

    val fieldName   = "ints"
    val inputValue  = 1
    val outputValue = 1

    val project = SchemaDsl.fromString() {
      s"""type Model{
         |   id: ID! @unique
         |   $fieldName: [Int!]!
         |}"""
    }

    verifySuccessfulSetAndRetrieval(fieldName, inputValue, outputValue, project)
  }

  "full scalar list" should "return full list for floats" in {

    val fieldName   = "floats"
    val inputValue  = 1.345
    val outputValue = 1.345

    val project = SchemaDsl.fromString() {
      s"""type Model{
         |   id: ID! @unique
         |   $fieldName: [Float!]!
         |}"""
    }

    verifySuccessfulSetAndRetrieval(fieldName, inputValue, outputValue, project)
  }

  "full scalar list" should "return full list for booleans" in {

    val fieldName   = "booleans"
    val inputValue  = true
    val outputValue = true

    val project = SchemaDsl.fromString() {
      s"""type Model{
         |   id: ID! @unique
         |   $fieldName: [Boolean!]!
         |}"""
    }

    verifySuccessfulSetAndRetrieval(fieldName, inputValue, outputValue, project)
  }

  "full scalar list" should "return full list for GraphQLIds" in {

    val fieldName   = "graphqlids"
    val inputValue  = """"someID123""""
    val outputValue = """"someID123""""

    val project = SchemaDsl.fromString() {
      s"""type Model{
         |   id: ID! @unique
         |   $fieldName: [ID!]!
         |}"""
    }

    verifySuccessfulSetAndRetrieval(fieldName, inputValue, outputValue, project)
  }

  "full scalar list" should "return full list for json" in {

    val fieldName   = "jsons"
    val inputValue  = """"{\"a\":2}""""
    val outputValue = """{"a":2}"""

    val project = SchemaDsl.fromString() {
      s"""type Model{
         |   id: ID! @unique
         |   $fieldName: [Json!]!
         |}"""
    }

    verifySuccessfulSetAndRetrieval(fieldName, inputValue, outputValue, project)
  }

  "full scalar list" should "return full list for datetime" in {

    val fieldName   = "datetimes"
    val inputValue  = """"2018""""
    val outputValue = """"2018-01-01T00:00:00.000Z""""

    val project = SchemaDsl.fromString() {
      s"""type Model{
         |   id: ID! @unique
         |   $fieldName: [DateTime!]!
         |}"""
    }

    verifySuccessfulSetAndRetrieval(fieldName, inputValue, outputValue, project)
  }

  "full scalar list" should "return full list for enum" in {

    val fieldName   = "enum"
    val inputValue  = "HA"
    val outputValue = """"HA""""

    val project = SchemaDsl.fromString() {
      s"""type Model{
         |   id: ID! @unique
         |   $fieldName: [Ha!]!
         |}
         |
         |enum Ha{
         |   HA,
         |   HI
         |}"""
    }

    verifySuccessfulSetAndRetrieval(fieldName, inputValue, outputValue, project)
  }

  "Overwriting a full scalar list with an empty list" should "return an empty list" in {

    val project = SchemaDsl.fromString() {
      """type Model{
         |   id: ID! @unique
         |   ints: [Int!]!
         |   strings: [String!]!
         |}"""
    }

    database.setup(project)

    val id = server
      .query(
        s"""mutation {
           |  createModel(data: {ints: { set: [1] }, strings: { set: ["short", "looooooooooong"]}}) {
           |    id
           |    strings
           |    ints
           |  }
           |}""".stripMargin,
        project
      )
      .pathAsString("data.createModel.id")

    val result = server.query(
      s"""{
         |  model(where: {id:"$id"}) {
         |    ints
         |    strings
         |  }
         |}""".stripMargin,
      project
    )

    result.toString should equal("""{"data":{"model":{"ints":[1],"strings":["short","looooooooooong"]}}}""")

    server
      .query(
        s"""mutation {
           |  updateModel(
           |  where: {id: "$id"}
           |  data: {ints: { set: [] }, strings: { set: []}}) {
           |    id
           |    strings
           |    ints
           |  }
           |}""".stripMargin,
        project
      )
    val result2 = server.query(
      s"""{
         |  model(where: {id:"$id"}) {
         |    ints
         |    strings
         |  }
         |}""".stripMargin,
      project
    )

    result2.toString should equal("""{"data":{"model":{"ints":[],"strings":[]}}}""")

  }

  "Overwriting a full scalar list with a list of different length" should "delete all members of the old list" in {

    val project = SchemaDsl.fromString() {
      """type Model{
        |   id: ID! @unique
        |   ints: [Int!]!
        |   strings: [String!]!
        |}"""
    }

    database.setup(project)

    val id = server
      .query(
        s"""mutation {
           |  createModel(data: {ints: { set: [1] }, strings: { set: ["short", "looooooooooong", "three", "four", "five"]}}) {
           |    id
           |    strings
           |    ints
           |  }
           |}""".stripMargin,
        project
      )
      .pathAsString("data.createModel.id")

    val result = server.query(
      s"""{
         |  model(where: {id:"$id"}) {
         |    ints
         |    strings
         |  }
         |}""".stripMargin,
      project
    )

    result.toString should equal("""{"data":{"model":{"ints":[1],"strings":["short","looooooooooong","three","four","five"]}}}""")

    server
      .query(
        s"""mutation {
           |  updateModel(
           |  where: {id: "$id"}
           |  data: {ints: { set: [1,2] }, strings: { set: ["one", "two"]}}) {
           |    id
           |    strings
           |    ints
           |  }
           |}""".stripMargin,
        project
      )
    val result2 = server.query(
      s"""{
         |  model(where: {id:"$id"}) {
         |    ints
         |    strings
         |  }
         |}""".stripMargin,
      project
    )

    result2.toString should equal("""{"data":{"model":{"ints":[1,2],"strings":["one","two"]}}}""")

  }

  "Nested scalar lists" should "work in creates " in {

    val project = SchemaDsl.fromString() {
      s"""type List{
         |   id: ID! @unique
         |   todos: [Todo!]!
         |   listInts: [Int!]!
         |}
         |
         |type Todo @embedded{
         |   todoInts: [Int!]!
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
         |   listInts: [Int!]!
         |}
         |
         |type Todo @embedded {
         |   tag: Tag
         |   todoInts: [Int!]!
         |}
         |
         |type Tag @embedded {
         |   tagInts: [Int!]!
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
         |   listInts: [Int!]!
         |}
         |
         |type Todo @embedded {
         |   uTodo: String! @unique
         |   tag: Tag
         |   todoInts: [Int!]!
         |}
         |
         |type Tag @embedded {
         |   uTag: String! @unique
         |   tagInts: [Int!]!
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

  "Nested scalar lists" should "work in upserts and only execute one branch of the upsert" taggedAs (IgnoreMongo) in {

    val project = SchemaDsl.fromString() {
      s"""type List{
         |   id: ID! @unique
         |   todo: Todo
         |   uList: String! @unique
         |   listInts: [Int!]!
         |}
         |
         |type Todo @embedded {
         |   uTodo: String! @unique
         |   todoInts: [Int!]!
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

  private def verifySuccessfulSetAndRetrieval(fieldName: String, inputValue: Any, outputValue: Any, project: Project) = {
    database.setup(project)

    val id = server
      .query(
        s"""mutation {
           |  createModel(data: {$fieldName: { set: [$inputValue] }}) {
           |    id
           |    $fieldName
           |  }
           |}""".stripMargin,
        project
      )
      .pathAsString("data.createModel.id")

    val result = server.query(
      s"""{
         |  model(where: {id:"$id"}) {
         |    $fieldName
         |  }
         |}""".stripMargin,
      project
    )

    result.toString should equal(s"""{"data":{"model":{"$fieldName":[$outputValue]}}}""")
  }
}
