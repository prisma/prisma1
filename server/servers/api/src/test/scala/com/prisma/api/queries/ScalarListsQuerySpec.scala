package com.prisma.api.queries

import com.prisma.{IgnoreMongo, IgnoreMySql, IgnoreSQLite}
import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability.{NonEmbeddedScalarListCapability, ScalarListsCapability}
import com.prisma.shared.models.Project
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class ScalarListsQuerySpec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities = Set(ScalarListsCapability)

  val scalarListStrategy = if (capabilities.has(NonEmbeddedScalarListCapability)) {
    "@scalarList(strategy: RELATION)"
  } else {
    ""
  }

  "empty scalar list" should "return empty list" in {
    val project = SchemaDsl.fromStringV11() {
      s"""type Model{
        |   id: ID! @id
        |   ints: [Int] $scalarListStrategy
        |   strings: [String] $scalarListStrategy
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

    val project = SchemaDsl.fromStringV11() {
      s"""type Model{
        |   id: ID! @id
        |   ints: [Int] $scalarListStrategy
        |   strings: [String] $scalarListStrategy
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

    val project = SchemaDsl.fromStringV11() {
      s"""type Model{
        |   id: ID! @id
        |   ints: [Int] $scalarListStrategy
        |   strings: [String] $scalarListStrategy
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

    val project = SchemaDsl.fromStringV11() {
      s"""type Model{
        |   id: ID! @id
        |   $fieldName: [String] $scalarListStrategy
        |}"""
    }

    verifySuccessfulSetAndRetrieval(fieldName, inputValue, outputValue, project)
  }

  "full scalar list" should "return full list for ints" in {

    val fieldName   = "ints"
    val inputValue  = 1
    val outputValue = 1

    val project = SchemaDsl.fromStringV11() {
      s"""type Model{
         |   id: ID! @id
         |   $fieldName: [Int] $scalarListStrategy
         |}"""
    }

    verifySuccessfulSetAndRetrieval(fieldName, inputValue, outputValue, project)
  }

  "full scalar list" should "return full list for floats" in {

    val fieldName   = "floats"
    val inputValue  = 1.345
    val outputValue = 1.345

    val project = SchemaDsl.fromStringV11() {
      s"""type Model{
         |   id: ID! @id
         |   $fieldName: [Float] $scalarListStrategy
         |}"""
    }

    verifySuccessfulSetAndRetrieval(fieldName, inputValue, outputValue, project)
  }

  "full scalar list" should "return full list for booleans" in {

    val fieldName   = "booleans"
    val inputValue  = true
    val outputValue = true

    val project = SchemaDsl.fromStringV11() {
      s"""type Model{
         |   id: ID! @id
         |   $fieldName: [Boolean] $scalarListStrategy
         |}"""
    }

    verifySuccessfulSetAndRetrieval(fieldName, inputValue, outputValue, project)
  }

  "full scalar list" should "return full list for GraphQLIds" in {

    val fieldName   = "graphqlids"
    val inputValue  = """"5beea4aa6183dd734b2dbd9b""""
    val outputValue = """"5beea4aa6183dd734b2dbd9b""""

    val project = SchemaDsl.fromStringV11() {
      s"""type Model{
         |   id: ID! @id
         |   $fieldName: [ID] $scalarListStrategy
         |}"""
    }

    verifySuccessfulSetAndRetrieval(fieldName, inputValue, outputValue, project)
  }

  "full scalar list" should "return full list for json" in {

    val fieldName   = "jsons"
    val inputValue  = """"{\"a\":2}""""
    val outputValue = """{"a":2}"""

    val project = SchemaDsl.fromStringV11() {
      s"""type Model{
         |   id: ID! @id
         |   $fieldName: [Json] $scalarListStrategy
         |}"""
    }

    verifySuccessfulSetAndRetrieval(fieldName, inputValue, outputValue, project)
  }

  "full scalar list" should "return full list for datetime" in {

    val fieldName   = "datetimes"
    val inputValue  = """"2018""""
    val outputValue = """"2018-01-01T00:00:00.000Z""""

    val project = SchemaDsl.fromStringV11() {
      s"""type Model{
         |   id: ID! @id
         |   $fieldName: [DateTime] $scalarListStrategy
         |}"""
    }

    verifySuccessfulSetAndRetrieval(fieldName, inputValue, outputValue, project)
  }

  "full scalar list" should "return full list for enum" in {

    val fieldName   = "enum"
    val inputValue  = "HA"
    val outputValue = """"HA""""

    val project = SchemaDsl.fromStringV11() {
      s"""type Model{
         |   id: ID! @id
         |   $fieldName: [Ha] $scalarListStrategy
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

    val project = SchemaDsl.fromStringV11() {
      s"""type Model{
         |   id: ID! @id
         |   ints: [Int] $scalarListStrategy
         |   strings: [String] $scalarListStrategy
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

    val project = SchemaDsl.fromStringV11() {
      s"""type Model{
        |   id: ID! @id
        |   ints: [Int] $scalarListStrategy
        |   strings: [String] $scalarListStrategy
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

  "Using Int ids" should "work with scalar lists" taggedAs (IgnoreMySql, IgnoreSQLite, IgnoreMongo) in {

    val project = SchemaDsl.fromStringV11() {
      s"""type Model{
         |   id: Int! @id
         |   name: String
         |   ints: [Int] $scalarListStrategy
         |}"""
    }

    database.setup(project)

    server.query(
      s"""mutation {
           |  createModel(data: {ints: { set: [1,2] }, name: "test"}) {
           |    id
           |    name
           |    ints
           |  }
           |}""".stripMargin,
      project
    )

    val result = server.query(
      s"""{
         |  models{
         |    ints
         |    name
         |  }
         |}""".stripMargin,
      project
    )

    result.toString should equal("""{"data":{"models":[{"ints":[1,2],"name":"test"}]}}""")

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
