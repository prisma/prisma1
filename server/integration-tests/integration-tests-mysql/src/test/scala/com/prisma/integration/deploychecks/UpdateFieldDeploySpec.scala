package com.prisma.integration.deploychecks

import com.prisma.IgnoreSQLite
import com.prisma.integration.IntegrationBaseSpec
import org.scalatest.{FlatSpec, Matchers}

class UpdateFieldDeploySpec extends FlatSpec with Matchers with IntegrationBaseSpec {
  override def doNotRunForPrototypes: Boolean = true

  "Updating a field from scalar non-list to scalar list" should "throw a warning if there is already data" in {

    val schema =
      """|type A {
         | name: String! @unique
         | value: Int
         |}""".stripMargin

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name: "A", value: 1}){name}}""", project)

    val schema2 =
      """type A {
        | name: String! @unique
        | value: [Int]
        |}""".stripMargin

    deployServer.deploySchemaThatMustWarn(project, schema2).toString should be(
      """{"data":{"deploy":{"migration":null,"errors":[],"warnings":[{"description":"You already have nodes for this model. This change may result in data loss."}]}}}""")
  }

  "Updating a field from scalar non-list to scalar list" should "throw a warning if there is already data but proceed with -force" taggedAs (IgnoreSQLite) in {

    val schema =
      """|type A {
         | name: String! @unique
         | value: Int
         |}""".stripMargin

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name: "A", value: 1}){name}}""", project)

    val schema2 =
      """type A {
        | name: String! @unique
        | value: [Int]
        |}""".stripMargin

    deployServer.deploySchemaThatMustWarn(project, schema2, force = true).toString should be(
      """{"data":{"deploy":{"migration":{"applied":0,"revision":3},"errors":[],"warnings":[{"description":"You already have nodes for this model. This change may result in data loss."}]}}}""")
  }

  "Updating a field from scalar non-list to scalar list" should "succeed if there is no data yet" taggedAs (IgnoreSQLite) in {

    val schema =
      """|type A {
         | name: String! @unique
         | value: Int
         |}""".stripMargin

    val (project, _) = setupProject(schema)

    val schema2 =
      """type A {
        | name: String! @unique
        | value: [Int]
        |}""".stripMargin

    deployServer.deploySchemaThatMustSucceed(project, schema2, 3)
  }

  "Updating a field from scalar List to scalar non-list" should "warn if there is already data" in {

    val schema =
      """type A {
        | name: String! @unique
        | value: [Int]
        |}""".stripMargin

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{ name: "A", value: {set: [1,2,3]}}){value}}""", project).toString should be(
      """{"data":{"createA":{"value":[1,2,3]}}}""")

    val schema2 =
      """type A {
        | name: String! @unique
        | value: Int
        |}""".stripMargin

    deployServer.deploySchemaThatMustWarn(project, schema2).toString should be(
      """{"data":{"deploy":{"migration":null,"errors":[],"warnings":[{"description":"You already have nodes for this model. This change may result in data loss."}]}}}""")
  }

  "Updating a field from scalar List to scalar non-list" should "succeed if there are no nodes yet" in {

    val schema =
      """type A {
        | name: String! @unique
        | value: [Int]
        |}""".stripMargin

    val (project, _) = setupProject(schema)

    val schema2 =
      """type A {
        | name: String! @unique
        | value: Int
        |}""".stripMargin

    deployServer.deploySchemaThatMustSucceed(project, schema2, 3)
  }

  "Updating a field from string to int" should "throw a warning if there is already data" in {

    val schema =
      """|type A {
         | name: String! @unique
         | value: String
         |}""".stripMargin

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name: "A", value: "1"}){name}}""", project)

    val schema2 =
      """type A {
        | name: String! @unique
        | value: Int
        |}""".stripMargin

    deployServer.deploySchemaThatMustWarn(project, schema2).toString should be(
      """{"data":{"deploy":{"migration":null,"errors":[],"warnings":[{"description":"You already have nodes for this model. This change may result in data loss."}]}}}""")
  }

  "Updating a field from string to int" should "throw a warning if there is already data but proceed with -force" taggedAs (IgnoreSQLite) in {

    val schema =
      """|type A {
         | name: String! @unique
         | value: String
         |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name: "A", value: "not a number"}){name}}""", project)

    val schema2 =
      """type A {
        | name: String! @unique
        | value: Int
        |}"""

    deployServer.deploySchemaThatMustWarn(project, schema2, true).toString should be(
      """{"data":{"deploy":{"migration":{"applied":0,"revision":3},"errors":[],"warnings":[{"description":"You already have nodes for this model. This change may result in data loss."}]}}}""")
  }

  "Updating a field from string to int" should "not throw a warning if there is no data yet" taggedAs (IgnoreSQLite) in {

    val schema =
      """|type A {
         | name: String! @unique
         | value: String
         |}"""

    val (project, _) = setupProject(schema)

    val schema2 =
      """type A {
        | name: String! @unique
        | value: Int
        |}"""

    deployServer.deploySchemaThatMustSucceed(project, schema2, 3)
  }

  "Updating a field from string to a relation" should "throw a warning if there is already data" in {

    val schema =
      """|type A {
         | name: String! @unique
         | value: String
         |}""".stripMargin

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name: "A", value: "1"}){name}}""", project)

    val schema2 =
      """type A {
        | name: String! @unique
        | value: A
        |}""".stripMargin

    deployServer.deploySchemaThatMustWarn(project, schema2).toString should be(
      """{"data":{"deploy":{"migration":null,"errors":[],"warnings":[{"description":"You already have nodes for this model. This change may result in data loss."}]}}}""")
  }

  "Updating a field from string to a relation" should "throw a warning if there is already data but proceed with -force" taggedAs (IgnoreSQLite) in {

    val schema =
      """|type A {
         | name: String! @unique
         | value: String
         |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name: "A", value: "not a number"}){name}}""", project)

    val schema2 =
      """type A {
        | name: String! @unique
        | value: A
        |}"""

    deployServer.deploySchemaThatMustWarn(project, schema2, true).toString should be(
      """{"data":{"deploy":{"migration":{"applied":0,"revision":3},"errors":[],"warnings":[{"description":"You already have nodes for this model. This change may result in data loss."}]}}}""")
  }

  "Updating a field from string to a relation" should "not throw a warning if there is no data yet" taggedAs (IgnoreSQLite) in {

    val schema =
      """|type A {
         | name: String! @unique
         | value: String
         |}"""

    val (project, _) = setupProject(schema)

    val schema2 =
      """type A {
        | name: String! @unique
        | value: A
        |}"""

    deployServer.deploySchemaThatMustSucceed(project, schema2, 3)
  }

  "Updating a scalar field to required" should "not throw an error if there is no data yet" taggedAs (IgnoreSQLite) in {

    val schema =
      """|type A {
         | name: String! @unique
         | value: String
         |}"""

    val (project, _) = setupProject(schema)

    val schema2 =
      """type A {
        | name: String! @unique
        | value: String!
        |}"""

    deployServer.deploySchemaThatMustSucceed(project, schema2, 3)
  }

  "Updating a scalar field to required" should "not throw an error if all required fields already have data" taggedAs (IgnoreSQLite) in {

    val schema =
      """|type A {
         | name: String! @unique
         | value: String
         |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name: "A", value: "not a number"}){name}}""", project)

    val schema2 =
      """type A {
        | name: String! @unique
        | value: String!
        |}"""

    deployServer.deploySchemaThatMustSucceed(project, schema2, 3)
  }

  "Updating a scalar field to required" should "warn if a newly required field is null" in {

    val schema =
      """|type A {
         | name: String! @unique
         | value: String
         |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name: "A"}){name}}""", project)

    val schema2 =
      """type A {
        | name: String! @unique
        | value: String!
        |}"""

    deployServer.deploySchemaThatMustWarn(project, schema2).toString should include("""The fields will be pre-filled with the value: ``.""")
  }

  "Updating the type of a required scalar field" should "warn if there are nodes for that type" in {

    val schema =
      """|type A {
         | name: String! @unique
         | value: String!
         |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name: "A", value: "A"}){name}}""", project)

    val schema2 =
      """type A {
        | name: String! @unique
        | value: Int!
        |}"""

    deployServer.deploySchemaThatMustWarn(project, schema2, force = true).toString should include("""The fields will be pre-filled with the value: `0`.""")
  }

  "Updating a relation field to required" should "not throw an error if there is no data yet" in {

    val schema =
      """|type A {
         | name: String! @unique
         | b: [B]
         |}
         |
         |type B {
         | name: String! @unique
         | a: A
         |}"""

    val (project, _) = setupProject(schema)

    val schema2 =
      """|type A {
         | name: String! @unique
         | b: [B]
         |}
         |
         |type B {
         | name: String! @unique
         | a: A!
         |}"""

    deployServer.deploySchemaThatMustSucceed(project, schema2, 3)
  }

  "Updating a relation field to required" should "not throw an error if all required fields already have data" in {

    val schema =
      """|type A {
         | name: String! @unique
         | b: [B]
         |}
         |
         |type B {
         | name: String! @unique
         | a: A
         |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createB(data:{name: "B", a:{create: {name: "A"}}}){name}}""", project)

    val schema2 =
      """|type A {
         | name: String! @unique
         | b: [B]
         |}
         |
         |type B {
         | name: String! @unique
         | a: A!
         |}"""

    deployServer.deploySchemaThatMustSucceed(project, schema2, 3)
  }

  "Updating a relation field to required" should "throw an error if a newly required field is null" in {

    val schema =
      """|type A {
         | name: String! @unique
         | b: [B]
         |}
         |
         |type B {
         | name: String! @unique
         | a: A
         |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createB(data:{name: "B"}){name}}""", project)

    val schema2 =
      """|type A {
         | name: String! @unique
         | b: [B]
         |}
         |
         |type B {
         | name: String! @unique
         | a: A!
         |}"""

    deployServer.deploySchemaThatMustError(project, schema2).toString should include(
      """You are updating the field `a` to be required. But there are already nodes for the model `B` that would violate that constraint.""")
  }
}
