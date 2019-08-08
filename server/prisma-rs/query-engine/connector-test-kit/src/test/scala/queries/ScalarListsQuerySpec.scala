package queries

import org.scalatest.{FlatSpec, Matchers}
import util.ConnectorCapability.{NonEmbeddedScalarListCapability, ScalarListsCapability}
import util._

class ScalarListsQuerySpec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities = Set(ScalarListsCapability)

  "empty scalar list" should "return empty list" in {
    val project = SchemaDsl.fromStringV11() {
      s"""model Model{
        |   id      String   @id @default(cuid())
        |   ints    Int[]    $scalarListDirective
        |   strings String[] $scalarListDirective
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
      s"""model Model{
        |   id String @id @default(cuid())
        |   ints Int[] $scalarListDirective
        |   strings String[] $scalarListDirective
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
      s"""model Model{
        |   id String @id @default(cuid())
        |   ints Int[] $scalarListDirective
        |   strings String[] $scalarListDirective
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
      s"""model Model{
        |   id String @id @default(cuid())
        |   $fieldName String[] $scalarListDirective
        |}"""
    }

    verifySuccessfulSetAndRetrieval(fieldName, inputValue, outputValue, project)
  }

  "full scalar list" should "return full list for ints" in {

    val fieldName   = "ints"
    val inputValue  = 1
    val outputValue = 1

    val project = SchemaDsl.fromStringV11() {
      s"""model Model{
         |   id String @id @default(cuid())
         |   $fieldName Int[] $scalarListDirective
         |}"""
    }

    verifySuccessfulSetAndRetrieval(fieldName, inputValue, outputValue, project)
  }

  "full scalar list" should "return full list for floats" in {

    val fieldName   = "floats"
    val inputValue  = 1.345
    val outputValue = 1.345

    val project = SchemaDsl.fromStringV11() {
      s"""model Model{
         |   id String @id @default(cuid())
         |   $fieldName Float[] $scalarListDirective
         |}"""
    }

    verifySuccessfulSetAndRetrieval(fieldName, inputValue, outputValue, project)
  }

  "full scalar list" should "return full list for booleans" in {

    val fieldName   = "booleans"
    val inputValue  = true
    val outputValue = true

    val project = SchemaDsl.fromStringV11() {
      s"""model Model{
         |   id String @id @default(cuid())
         |   $fieldName Boolean[] $scalarListDirective
         |}"""
    }

    verifySuccessfulSetAndRetrieval(fieldName, inputValue, outputValue, project)
  }

  "full scalar list" should "return full list for GraphQLIds" in {

    val fieldName   = "graphqlids"
    val inputValue  = """"5beea4aa6183dd734b2dbd9b""""
    val outputValue = """"5beea4aa6183dd734b2dbd9b""""

    val project = SchemaDsl.fromStringV11() {
      s"""model Model{
         |   id String @id @default(cuid())
         |   $fieldName String[] $scalarListDirective
         |}"""
    }

    verifySuccessfulSetAndRetrieval(fieldName, inputValue, outputValue, project)
  }

  "full scalar list" should "return full list for datetime" in {

    val fieldName   = "datetimes"
    val inputValue  = """"2018-01-01T00:00:00.000Z""""
    val outputValue = inputValue

    val project = SchemaDsl.fromStringV11() {
      s"""model Model{
         |   id String @id @default(cuid())
         |   $fieldName DateTime[] $scalarListDirective
         |}"""
    }

    verifySuccessfulSetAndRetrieval(fieldName, inputValue, outputValue, project)
  }

  "full scalar list" should "return full list for enum" in {

    val fieldName   = "enum"
    val inputValue  = "HA"
    val outputValue = """"HA""""

    val project = SchemaDsl.fromStringV11() {
      s"""model Model{
         |   id String @id @default(cuid())
         |   $fieldName Ha[] $scalarListDirective
         |}
         |
         |enum Ha{
         |   HA
         |   HI
         |}"""
    }

    verifySuccessfulSetAndRetrieval(fieldName, inputValue, outputValue, project)
  }

  "Overwriting a full scalar list with an empty list" should "return an empty list" in {

    val project = SchemaDsl.fromStringV11() {
      s"""model Model{
         |   id String @id @default(cuid())
         |   ints Int[] $scalarListDirective
         |   strings String[] $scalarListDirective
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
      s"""model Model{
        |   id String @id @default(cuid())
        |   ints Int[] $scalarListDirective
        |   strings String[] $scalarListDirective
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
      s"""model Model{
         |   id Int @id
         |   name String?
         |   ints Int[] $scalarListDirective
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
