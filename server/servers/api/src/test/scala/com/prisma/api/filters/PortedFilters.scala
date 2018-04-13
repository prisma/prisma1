package com.prisma.api.filters

import com.prisma.api.ApiBaseSpec
import com.prisma.shared.models.Project
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class PortedFiltersSpec extends FlatSpec with Matchers with ApiBaseSpec {

  val project: Project = SchemaDsl.fromString() { """
                                                    |type ScalarModel {
                                                    |  id: ID! @unique
                                                    |  idTest: String
                                                    |  optString: String
                                                    |  optInt: Int
                                                    |  optFloat: Float
                                                    |  optBoolean: Boolean
                                                    |  optDateTime: DateTime
                                                    |  optEnum: Enum
                                                    |}
                                                    |
                                                    |enum Enum{
                                                    | A
                                                    | B
                                                    |}
                                                    |""".stripMargin }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(project)
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    database.truncate(project)
  }

  def createTest(id: String, optString: String, optInt: Int, optFloat: Float, optBoolean: Boolean, optEnum: String, optDateTime: String): Unit = {
    val string = if (optString == null) "null" else s""""$optString""""

    server.query(
      s"""mutation{createScalarModel(data:{
         |idTest:"$id",
         |optString: $string,
         |optInt: $optInt,
         |optFloat: $optFloat,
         |optBoolean: $optBoolean,
         |optEnum: $optEnum,
         |optDateTime: "$optDateTime"}){id}}""".stripMargin,
      project
    )
  }

  //region Recursion
  "A filter query" should "support the AND filter in one recursion level" in {

    createTest("id1", "bar", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "foo bar", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")
    createTest("id3", "foo", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")
    createTest("id4", "foo bar barz", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")
    createTest("id5", "foo bar barz", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")

    val res =
      server.query("""{scalarModels(where: {optString_starts_with: "foo", AND: [{optBoolean: false, idTest_ends_with: "5"}]}){optBoolean}}""",
                   project = project)

    res.toString() should be("""{"data":{"scalarModels":[{"optBoolean":false}]}}""")
  }

  "A filter query" should "support the AND filter in two recursion levels" in {

    createTest("id1", "bar", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "foo bar", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")
    createTest("id3", "foo", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")
    createTest("id4", "foo bar barz", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")
    createTest("id5", "foo bar barz", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")
    createTest("id6", "foo bar barz", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")

    val res =
      server.query(
        query = """{scalarModels(where: {AND: [{optBoolean: false, idTest_ends_with: "5", AND: [{optString_starts_with: "foo"}]}]}){optBoolean}}""",
        project = project
      )

    res.toString() should be("""{"data":{"scalarModels":[{"optBoolean":false}]}}""")
  }

  "A filter query" should "support the OR filter in one recursion level" in {

    createTest("id1", "bar", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "foo bar", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")
    createTest("id3", "foo", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")
    createTest("id4", "foo bar barz", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")
    createTest("id5", "foo bar barz", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")

    val res =
      server.query(query = """{scalarModels(where: {optBoolean: false, OR: [{optString_starts_with: "foo"}, {idTest_ends_with: "5"}]}){optBoolean}}""",
                   project = project)

    res.toString() should be("""{"data":{"scalarModels":[{"optBoolean":false},{"optBoolean":false},{"optBoolean":false},{"optBoolean":false}]}}""")
  }

  "A filter query" should "support the OR filter in two recursion levels" in {

    createTest("id1", "bar", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "foo bar", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")
    createTest("id3", "foo", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")
    createTest("id4", "foo bar barz", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")
    createTest("id5", "foo bar barz", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")
    createTest("id6", "foo bar barz", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")

    val res =
      server.query(
        query = """{scalarModels(where: {OR: [{optString_starts_with: "foo", OR: [{optBoolean: false},{idTest_ends_with: "5"}]}]}){optBoolean}}""",
        project = project
      )

    res.toString() should be(
      """{"data":{"scalarModels":[{"optBoolean":false},{"optBoolean":false},{"optBoolean":false},{"optBoolean":false},{"optBoolean":false}]}}""")
  }
  //endregion

  //region null
  "A filter query" should "support filtering on null" in {

    createTest("id1", optString = null, 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", optString = "foo bar", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")
    createTest("id3", optString = null, 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")

    val filterOnNull = server.query(query = """{scalarModels(where: {optString: null}){idTest}}""", project = project)

    filterOnNull.toString() should be("""{"data":{"scalarModels":[{"idTest":"id1"},{"idTest":"id3"}]}}""")

    val filterOnNotNull = server.query(query = """{scalarModels(where: {optString_not: null}){idTest}}""", project = project)

    filterOnNotNull.toString() should be("""{"data":{"scalarModels":[{"idTest":"id2"}]}}""")

    val filterOnInNull = server.query(query = """{scalarModels(where: {optString_in: null}){optBoolean}}""", project = project)

    filterOnInNull.toString() should be("""{"data":{"scalarModels":[]}}""")

    val filterOnNotInNull = server.query(query = """{scalarModels(where: {optString_not_in: null}){optBoolean}}""", project = project)

    filterOnNotInNull.toString() should be("""{"data":{"scalarModels":[]}}""")

  }

  //endregion

  //region String

  "A filter query" should "support the equality filter on strings" in {

    createTest("id1", "bar", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "foo bar", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")
    createTest("id3", "foo bar barz", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")

    val res = server.query(query = """{scalarModels(where: {optString: "bar"}){optBoolean}}""", project = project)

    res.toString() should be("""{"data":{"scalarModels":[{"optBoolean":true}]}}""")
  }

  "A filter query" should "support the not-equality filter on strings" in {

    createTest("id1", "bar", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "foo bar", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")
    createTest("id3", "foo bar barz", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")

    val res = server.query(query = """{scalarModels(where: {optString_not: "bar"}){optBoolean}}""", project = project)

    res.toString() should be("""{"data":{"scalarModels":[{"optBoolean":false},{"optBoolean":false}]}}""")
  }

  "A filter query" should "support the contains filter on strings" in {

    createTest("id1", "bara", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "foo bar", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")
    createTest("id3", "foo bar barz", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")

    val res = server.query(query = """{scalarModels(where: {optString_contains: "bara"}){optBoolean}}""", project = project)

    res.toString() should be("""{"data":{"scalarModels":[{"optBoolean":true}]}}""")
  }

  "A filter query" should "support the not_contains filter on strings" in {

    createTest("id1", "bara", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "foo bar", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")
    createTest("id3", "foo bar barz", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")

    val res = server.query(query = """{scalarModels(where: {optString_not_contains: "bara"}){optBoolean}}""", project = project)

    res.toString() should be("""{"data":{"scalarModels":[{"optBoolean":false},{"optBoolean":false}]}}""")
  }

  "A filter query" should "support the starts_with filter on strings" in {

    createTest("id1", "bara", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "foo bar", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")
    createTest("id3", "foo bar barz", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")

    val res = server.query(query = """{scalarModels(where: {optString_starts_with: "bar"}){optBoolean}}""", project = project)

    res.toString() should be("""{"data":{"scalarModels":[{"optBoolean":true}]}}""")
  }

  "A filter query" should "support the not_starts_with filter on strings" in {

    createTest("id1", "bara", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "foo bar", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")
    createTest("id3", "foo bar barz", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")

    val res = server.query(query = """{scalarModels(where: {optString_not_starts_with: "bar"}){optBoolean}}""", project = project)

    res.toString() should be("""{"data":{"scalarModels":[{"optBoolean":false},{"optBoolean":false}]}}""")
  }

  "A filter query" should "support the ends_with filter on strings" in {

    createTest("id1", "bara", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "foo bar", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")
    createTest("id3", "foo bar bar", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")

    val res = server.query(query = """{scalarModels(where: {optString_ends_with: "bara"}){optBoolean}}""", project = project)

    res.toString() should be("""{"data":{"scalarModels":[{"optBoolean":true}]}}""")
  }

  "A filter query" should "support the not_ends_with filter on strings" in {

    createTest("id1", "bara", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "foo bar", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")
    createTest("id3", "foo bar bar", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")

    val res = server.query(query = """{scalarModels(where: {optString_not_ends_with: "bara"}){optBoolean}}""", project = project)

    res.toString() should be("""{"data":{"scalarModels":[{"optBoolean":false},{"optBoolean":false}]}}""")
  }

  "A filter query" should "support the lt filter on strings" in {

    createTest("id1", "1", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "2", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")
    createTest("id3", "3", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")

    val res = server.query(query = """{scalarModels(where: {optString_lt: "2"}){optBoolean}}""", project = project)

    res.toString() should be("""{"data":{"scalarModels":[{"optBoolean":true}]}}""")
  }

  "A filter query" should "support the lte filter on strings" in {

    createTest("id1", "1", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "2", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")
    createTest("id3", "3", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")

    val res = server.query(query = """{scalarModels(where: {optString_lte: "2"}){optBoolean}}""", project = project)

    res.toString() should be("""{"data":{"scalarModels":[{"optBoolean":true},{"optBoolean":false}]}}""")
  }

  "A filter query" should "support the gt filter on strings" in {

    createTest("id1", "1", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "2", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")
    createTest("id3", "3", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")

    val res = server.query(query = """{scalarModels(where: {optString_gt: "2"}){optBoolean}}""", project = project)

    res.toString() should be("""{"data":{"scalarModels":[{"optBoolean":false}]}}""")
  }

  "A filter query" should "support the gte filter on strings" in {

    createTest("id1", "1", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "2", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")
    createTest("id3", "3", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")

    val res = server.query(query = """{scalarModels(where: {optString_gte: "2"}){optBoolean}}""", project = project)

    res.toString() should be("""{"data":{"scalarModels":[{"optBoolean":false},{"optBoolean":false}]}}""")
  }

  "A filter query" should "support the in filter on strings" in {

    createTest("id1", "a", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "ab", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")
    createTest("id3", "abc", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")

    server
      .query(query = """{scalarModels(where: {optString_in: ["a"]}){optBoolean}}""", project = project)
      .toString() should be("""{"data":{"scalarModels":[{"optBoolean":true}]}}""")

    server.query(query = """{scalarModels(where: {optString_in: ["a","b"]}){optBoolean}}""", project = project).toString() should be(
      """{"data":{"scalarModels":[{"optBoolean":true}]}}""")

    server.query(query = """{scalarModels(where: {optString_in: ["a","abc"]}){optBoolean}}""", project = project).toString() should be(
      """{"data":{"scalarModels":[{"optBoolean":true},{"optBoolean":false}]}}""")

    server
      .query(query = """{scalarModels(where: {optString_in: []}){optBoolean}}""", project = project)
      .toString() should be("""{"data":{"scalarModels":[]}}""")
  }

  "A filter query" should "support the not_in filter on strings" in {

    createTest("id1", "a", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "ab", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")
    createTest("id3", "abc", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")

    server.query(query = """{scalarModels(where: {optString_not_in: ["a"]}){optBoolean}}""", project = project).toString should be(
      """{"data":{"scalarModels":[{"optBoolean":false},{"optBoolean":false}]}}""")

    server.query(query = """{scalarModels(orderBy: idTest_ASC, where: {optString_not_in: []}){optBoolean}}""", project = project).toString should be(
      """{"data":{"scalarModels":[{"optBoolean":true},{"optBoolean":false},{"optBoolean":false}]}}""")
  }
  //endregion

  //region Integer

  "A filter query" should "support the equality filter on integers" in {

    createTest("id1", "1", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "2", 2, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")
    createTest("id3", "3", 3, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")

    val res = server.query(query = """{scalarModels(where: {optInt: 1}){optBoolean}}""", project = project)

    res.toString() should be("""{"data":{"scalarModels":[{"optBoolean":true}]}}""")
  }

  "A filter query" should "support the not equality filter on integers" in {

    createTest("id1", "a", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "ab", 2, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")
    createTest("id3", "abc", 3, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")

    val res = server.query(query = """{scalarModels(where: {optInt_not: 1}){optBoolean}}""", project = project)

    res.toString() should be("""{"data":{"scalarModels":[{"optBoolean":false},{"optBoolean":false}]}}""")
  }

  "A filter query" should "support the lt filter on integers" in {

    createTest("id1", "1", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "2", 2, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")
    createTest("id3", "3", 3, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")

    val res = server.query(query = """{scalarModels(where: {optInt_lt: 2}){optBoolean}}""", project = project)

    res.toString() should be("""{"data":{"scalarModels":[{"optBoolean":true}]}}""")
  }

  "A filter query" should "support the lte filter on integers" in {

    createTest("id1", "1", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "2", 2, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")
    createTest("id3", "3", 3, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")

    val res = server.query(query = """{scalarModels(where: {optInt_lte: 2}){optBoolean}}""", project = project)

    res.toString() should be("""{"data":{"scalarModels":[{"optBoolean":true},{"optBoolean":false}]}}""")
  }

  "A filter query" should "support the gt filter on integers" in {

    createTest("id1", "1", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "2", 2, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")
    createTest("id3", "3", 3, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")

    val res = server.query(query = """{scalarModels(where: {optInt_gt: 2}){optBoolean}}""", project = project)

    res.toString() should be("""{"data":{"scalarModels":[{"optBoolean":false}]}}""")
  }

  "A filter query" should "support the gte filter on integers" in {

    createTest("id1", "1", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "2", 2, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")
    createTest("id3", "3", 3, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")

    val res = server.query(query = """{scalarModels(where: {optInt_gte: 2}){optBoolean}}""", project = project)

    res.toString() should be("""{"data":{"scalarModels":[{"optBoolean":false},{"optBoolean":false}]}}""")
  }

  "A filter query" should "support the in filter on integers" in {

    createTest("id1", "a", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "ab", 2, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")
    createTest("id3", "abc", 3, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")

    val res = server.query(query = """{scalarModels(where: {optInt_in: [1]}){optBoolean}}""", project = project)

    res.toString() should be("""{"data":{"scalarModels":[{"optBoolean":true}]}}""")
  }

  "A filter query" should "support the not_in filter on integers" in {

    createTest("id1", "a", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "ab", 2, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")
    createTest("id3", "abc", 3, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")

    val res = server.query(query = """{scalarModels(where: {optInt_not_in: [1]}){optBoolean}}""", project = project)

    res.toString() should be("""{"data":{"scalarModels":[{"optBoolean":false},{"optBoolean":false}]}}""")
  }
  //endregion

  //region Float

  "A filter query" should "support the equality filter on float" in {

    createTest("id1", "1", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "2", 2, 2, optBoolean = false, "A", "2016-09-23T12:29:32.342")
    createTest("id3", "3", 3, 3, optBoolean = false, "A", "2016-09-23T12:29:32.342")

    val res = server.query(query = """{scalarModels(where: {optFloat: 1}){optBoolean}}""", project = project)

    res.toString() should be("""{"data":{"scalarModels":[{"optBoolean":true}]}}""")
  }

  "A filter query" should "support the not equality filter on float" in {

    createTest("id1", "a", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "ab", 2, 2, optBoolean = false, "A", "2016-09-23T12:29:32.342")
    createTest("id3", "abc", 3, 3, optBoolean = false, "A", "2016-09-23T12:29:32.342")

    val res = server.query(query = """{scalarModels(where: {optFloat_not: 1}){optBoolean}}""", project = project)

    res.toString() should be("""{"data":{"scalarModels":[{"optBoolean":false},{"optBoolean":false}]}}""")
  }

  "A filter query" should "support the lt filter on floats" in {

    createTest("id1", "1", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "2", 2, 2, optBoolean = false, "A", "2016-09-23T12:29:32.342")
    createTest("id3", "3", 3, 3, optBoolean = false, "A", "2016-09-23T12:29:32.342")

    val res = server.query(query = """{scalarModels(where: {optFloat_lt: 2}){optBoolean}}""", project = project)

    res.toString() should be("""{"data":{"scalarModels":[{"optBoolean":true}]}}""")
  }

  "A filter query" should "support the lte filter on floats" in {

    createTest("id1", "1", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "2", 2, 2, optBoolean = false, "A", "2016-09-23T12:29:32.342")
    createTest("id3", "3", 3, 3, optBoolean = false, "A", "2016-09-23T12:29:32.342")

    val res = server.query(query = """{scalarModels(where: {optFloat_lte: 2}){optBoolean}}""", project = project)

    res.toString() should be("""{"data":{"scalarModels":[{"optBoolean":true},{"optBoolean":false}]}}""")
  }

  "A filter query" should "support the gt filter on floats" in {

    createTest("id1", "1", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "2", 2, 2, optBoolean = false, "A", "2016-09-23T12:29:32.342")
    createTest("id3", "3", 3, 3, optBoolean = false, "A", "2016-09-23T12:29:32.342")

    val res = server.query(query = """{scalarModels(where: {optFloat_gt: 2}){optBoolean}}""", project = project)

    res.toString() should be("""{"data":{"scalarModels":[{"optBoolean":false}]}}""")
  }

  "A filter query" should "support the gte filter on floats" in {

    createTest("id1", "1", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "2", 2, 2, optBoolean = false, "A", "2016-09-23T12:29:32.342")
    createTest("id3", "3", 3, 3, optBoolean = false, "A", "2016-09-23T12:29:32.342")

    val res = server.query(query = """{scalarModels(where: {optFloat_gte: 2}){optBoolean}}""", project = project)

    res.toString() should be("""{"data":{"scalarModels":[{"optBoolean":false},{"optBoolean":false}]}}""")
  }

  "A filter query" should "support the in filter on floats" in {

    createTest("id1", "a", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "ab", 2, 2, optBoolean = false, "A", "2016-09-23T12:29:32.342")
    createTest("id3", "abc", 3, 3, optBoolean = false, "A", "2016-09-23T12:29:32.342")

    val res = server.query(query = """{scalarModels(where: {optFloat_in: [1]}){optBoolean}}""", project = project)

    res.toString() should be("""{"data":{"scalarModels":[{"optBoolean":true}]}}""")
  }

  "A filter query" should "support the not_in filter on floats" in {

    createTest("id1", "a", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "ab", 2, 2, optBoolean = false, "A", "2016-09-23T12:29:32.342")
    createTest("id3", "abc", 3, 3, optBoolean = false, "A", "2016-09-23T12:29:32.342")

    val res = server.query(query = """{scalarModels(where: {optFloat_not_in: [1]}){optBoolean}}""", project = project)

    res.toString() should be("""{"data":{"scalarModels":[{"optBoolean":false},{"optBoolean":false}]}}""")
  }
  //endregion

  // region Boolean

  "A filter query" should "support the equality filter on booleans" in {

    createTest("id1", "bar", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "foo bar", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")
    createTest("id3", "foo bar barz", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")

    val res = server.query(query = """{scalarModels(where: {optBoolean: true}){optBoolean}}""", project = project)

    res.toString() should be("""{"data":{"scalarModels":[{"optBoolean":true}]}}""")
  }

  "A filter query" should "support the not-equality filter on booleans" in {

    createTest("id1", "bar", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "foo bar", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")
    createTest("id3", "foo bar barz", 1, 1, optBoolean = false, "A", "2016-09-23T12:29:32.342")

    val res = server.query(query = """{scalarModels(where: {optBoolean_not: true}){optBoolean}}""", project = project)

    res.toString() should be("""{"data":{"scalarModels":[{"optBoolean":false},{"optBoolean":false}]}}""")
  }
  //endregion

  //region DateTime

  "A filter query" should "support the equality filter on DateTime" in {

    createTest("id1", "1", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "2", 2, 2, optBoolean = false, "A", "2016-09-24T12:29:32.342")
    createTest("id3", "3", 3, 3, optBoolean = false, "A", "2016-09-25T12:29:32.342")

    val res = server.query(query = """{scalarModels(where: {optDateTime: "2016-09-24T12:29:32.342"}){optBoolean}}""", project = project)

    res.toString() should be("""{"data":{"scalarModels":[{"optBoolean":false}]}}""")
  }

  "A filter query" should "support the not equality filter on DateTime" in {

    createTest("id1", "1", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "2", 2, 2, optBoolean = false, "A", "2016-09-24T12:29:32.342")
    createTest("id3", "3", 3, 3, optBoolean = false, "A", "2016-09-25T12:29:32.342")

    val res = server.query(query = """{scalarModels(where: {optDateTime_not: "2016-09-24T12:29:32.342Z"}){optBoolean}}""", project = project)

    res.toString() should be("""{"data":{"scalarModels":[{"optBoolean":true},{"optBoolean":false}]}}""")
  }

  "A filter query" should "support the lt filter on DateTime" in {

    createTest("id1", "1", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "2", 2, 2, optBoolean = false, "A", "2016-09-24T12:29:32.342")
    createTest("id3", "3", 3, 3, optBoolean = false, "A", "2016-09-25T12:29:32.342")

    val res = server.query(query = """{scalarModels(where: {optDateTime_lt: "2016-09-24T12:29:32.342Z"}){optBoolean}}""", project = project)

    res.toString() should be("""{"data":{"scalarModels":[{"optBoolean":true}]}}""")
  }

  "A filter query" should "support the lte filter on DateTime" in {

    createTest("id1", "1", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "2", 2, 2, optBoolean = false, "A", "2016-09-24T12:29:32.342")
    createTest("id3", "3", 3, 3, optBoolean = false, "A", "2016-09-25T12:29:32.342")

    val res = server.query(query = """{scalarModels(where: {optDateTime_lte: "2016-09-24T12:29:32.342Z"}){optBoolean}}""", project = project)

    res.toString() should be("""{"data":{"scalarModels":[{"optBoolean":true},{"optBoolean":false}]}}""")
  }

  "A filter query" should "support the gt filter on DateTime" in {

    createTest("id1", "1", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "2", 2, 2, optBoolean = false, "A", "2016-09-24T12:29:32.342")
    createTest("id3", "3", 3, 3, optBoolean = false, "A", "2016-09-25T12:29:32.342")

    val res = server.query(query = """{scalarModels(where: {optDateTime_gt: "2016-09-24T12:29:32.342Z"}){optBoolean}}""", project = project)

    res.toString() should be("""{"data":{"scalarModels":[{"optBoolean":false}]}}""")
  }

  "A filter query" should "support the gte filter on DateTime" in {

    createTest("id1", "1", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "2", 2, 2, optBoolean = false, "A", "2016-09-24T12:29:32.342")
    createTest("id3", "3", 3, 3, optBoolean = false, "A", "2016-09-25T12:29:32.342")

    val res = server.query(query = """{scalarModels(where: {optDateTime_gte: "2016-09-24T12:29:32.342Z"}){optBoolean}}""", project = project)

    res.toString() should be("""{"data":{"scalarModels":[{"optBoolean":false},{"optBoolean":false}]}}""")
  }

  "A filter query" should "support the in filter on DateTime" in {

    createTest("id1", "1", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "2", 2, 2, optBoolean = false, "A", "2016-09-24T12:29:32.342")
    createTest("id3", "3", 3, 3, optBoolean = false, "A", "2016-09-25T12:29:32.342")

    val res = server.query(query = """{scalarModels(where: {optDateTime_in: ["2016-09-24T12:29:32.342Z"]}){optBoolean}}""", project = project)

    res.toString() should be("""{"data":{"scalarModels":[{"optBoolean":false}]}}""")
  }

  "A filter query" should "support the not_in filter on DateTime" in {

    createTest("id1", "1", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "2", 2, 2, optBoolean = false, "A", "2016-09-24T12:29:32.342")
    createTest("id3", "3", 3, 3, optBoolean = false, "A", "2016-09-25T12:29:32.342")

    val res = server.query(query = """{scalarModels(where: {optDateTime_not_in: ["2016-09-24T12:29:32.342Z"]}){optBoolean}}""", project = project)

    res.toString() should be("""{"data":{"scalarModels":[{"optBoolean":true},{"optBoolean":false}]}}""")
  }
  //endregion

  //region Enum

  "A filter query" should "support the equality filter on Enum" in {

    createTest("id1", "1", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "2", 2, 2, optBoolean = false, "B", "2016-09-24T12:29:32.342")
    createTest("id3", "3", 3, 3, optBoolean = false, "B", "2016-09-25T12:29:32.342")

    val res = server.query(query = """{scalarModels(where: {optEnum: A}){optBoolean}}""", project = project)

    res.toString() should be("""{"data":{"scalarModels":[{"optBoolean":true}]}}""")
  }

  "A filter query" should "support the not equality filter on Enum" in {

    createTest("id1", "1", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "2", 2, 2, optBoolean = false, "B", "2016-09-24T12:29:32.342")
    createTest("id3", "3", 3, 3, optBoolean = false, "B", "2016-09-25T12:29:32.342")

    val res = server.query(query = """{scalarModels(where: {optEnum_not: A}){optBoolean}}""", project = project)

    res.toString() should be("""{"data":{"scalarModels":[{"optBoolean":false},{"optBoolean":false}]}}""")
  }

  "A filter query" should "support the in filter on Enum" in {

    createTest("id1", "1", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "2", 2, 2, optBoolean = false, "B", "2016-09-24T12:29:32.342")
    createTest("id3", "3", 3, 3, optBoolean = false, "B", "2016-09-25T12:29:32.342")

    val res = server.query(query = """{scalarModels(where: {optEnum_in: [A]}){optBoolean}}""", project = project)

    res.toString() should be("""{"data":{"scalarModels":[{"optBoolean":true}]}}""")
  }

  "A filter query" should "support the not in filter on Enum" in {
    createTest("id1", "1", 1, 1, optBoolean = true, "A", "2016-09-23T12:29:32.342")
    createTest("id2", "2", 2, 2, optBoolean = false, "B", "2016-09-24T12:29:32.342")
    createTest("id3", "3", 3, 3, optBoolean = false, "B", "2016-09-25T12:29:32.342")

    val res = server.query(query = """{scalarModels(where: {optEnum_not_in: [A]}){optBoolean}}""", project = project)

    res.toString() should be("""{"data":{"scalarModels":[{"optBoolean":false},{"optBoolean":false}]}}""")
  }
  //endregion
}
