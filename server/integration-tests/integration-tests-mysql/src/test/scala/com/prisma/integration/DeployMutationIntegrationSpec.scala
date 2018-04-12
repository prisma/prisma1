package com.prisma.integration

import org.scalatest.{FlatSpec, Matchers}

class DeployMutationIntegrationSpec extends FlatSpec with Matchers with IntegrationBaseSpec {

  // test warning without -force
  // test warning with -force
  // test no warning
  // test error
  // test no error

  //test multiple warnings
  //test multiple warnings with -force
  //test warnings and errors
  //test warnings and errors with -force

  //region Delete Model

  "Deleting a model" should "throw a warning if nodes already exist" in {

    val schema =
      """|type A {
         | name: String! @unique
         | value: Int
         |}
         |
         |type B {
         | name: String! @unique
         | value: Int
         |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name: "A", value: 1}){name}}""", project)

    val schema2 =
      """type B {
        | name: String! @unique
        | value: Int
        |}"""

    deployServer.deploySchemaThatMustWarn(project, schema2).toString should be(
      """{"data":{"deploy":{"migration":{"applied":0,"revision":0},"errors":[],"warnings":[{"description":"You already have nodes for this model. This change will result in data loss."}]}}}""")
  }

  "Deleting a model" should "throw a warning if nodes already exist but proceed if the -force flag is present" in {

    val schema =
      """|type A {
         | name: String! @unique
         | value: Int
         |}
         |
         |type B {
         | name: String! @unique
         | value: Int
         |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name: "A", value: 1}){name}}""", project)

    val schema2 =
      """type B {
        | name: String! @unique
        | value: Int
        |}"""

    deployServer.deploySchemaThatMustWarn(project, schema2, force = true).toString should be(
      """{"data":{"deploy":{"migration":{"applied":0,"revision":3},"errors":[],"warnings":[{"description":"You already have nodes for this model. This change will result in data loss."}]}}}""")
  }

  "Deleting a model" should "not throw a warning or error if no nodes  exist" in {

    val schema =
      """|type A {
         | name: String! @unique
         | value: Int
         |}
         |
         |type B {
         | name: String! @unique
         | value: Int
         |}"""

    val (project, _) = setupProject(schema)

    val schema2 =
      """type B {
        | name: String! @unique
        | value: Int
        |}"""

    deployServer.deploySchemaThatMustSucceed(project, schema2).toString should be(
      """{"data":{"deploy":{"migration":{"applied":0,"revision":3},"errors":[],"warnings":[]}}}""")
  }

  //endregion

  // region Create Field

  "Creating a required Field" should "error when nodes already exist and there is no default value" in {

    val schema =
      """type A {
        | name: String! @unique
        |}""".stripMargin

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name:"A"}){name}}""", project).toString should be("""{"data":{"createA":{"name":"A"}}}""")

    val schema2 =
      """type A {
        | name: String! @unique
        | value: Int!
        |}""".stripMargin

    deployServer.deploySchemaThatMustFail(project, schema2).toString should be(
      """{"data":{"deploy":{"migration":{"applied":0,"revision":0},"errors":[{"description":"You are creating a required field without a defaultValue but there are already nodes present."}],"warnings":[]}}}""")
  }

  "Creating a required Field" should "succeed when nodes already exist but there is a defaultValue" in {

    val schema =
      """type A {
        | name: String! @unique
        |}""".stripMargin

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name:"A"}){name}}""", project).toString should be("""{"data":{"createA":{"name":"A"}}}""")

    val schema2 =
      """type A {
        | name: String! @unique
        | value: Int! @default(value: 12)
        |}""".stripMargin

    deployServer.deploySchemaThatMustSucceed(project, schema2).toString should be(
      """{"data":{"deploy":{"migration":{"applied":0,"revision":3},"errors":[],"warnings":[]}}}""")
  }

  "Creating a required Field" should "not error when there is no defaultValue but there are no nodes yet" in {

    val schema =
      """type A {
        | name: String! @unique
        |}""".stripMargin

    val (project, _) = setupProject(schema)

    val schema2 =
      """type A {
        | name: String! @unique
        | value: Int!
        |}""".stripMargin

    deployServer.deploySchemaThatMustSucceed(project, schema2).toString should be(
      """{"data":{"deploy":{"migration":{"applied":0,"revision":3},"errors":[],"warnings":[]}}}""")
  }

  // endregion

  // region Delete Field

  "Deleting a field" should "throw a warning if nodes are present" in {

    val schema =
      """type A {
        | name: String! @unique
        | dummy: String
        |}""".stripMargin

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name:"A", dummy: "test"}){name, dummy}}""", project).toString should be(
      """{"data":{"createA":{"name":"A","dummy":"test"}}}""")

    val schema2 =
      """type A {
        | name: String! @unique
        |}""".stripMargin

    deployServer.deploySchemaThatMustWarn(project, schema2).toString should be(
      """{"data":{"deploy":{"migration":{"applied":0,"revision":0},"errors":[],"warnings":[{"description":"You already have nodes for this model. This change may result in data loss."}]}}}""")
  }

  "Deleting a field" should "throw a warning if nodes are present but proceed with -force flag" in {

    val schema =
      """type A {
        | name: String! @unique
        | dummy: String
        |}""".stripMargin

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name:"A", dummy: "test"}){name, dummy}}""", project).toString should be(
      """{"data":{"createA":{"name":"A","dummy":"test"}}}""")

    val schema2 =
      """type A {
        | name: String! @unique
        |}""".stripMargin

    deployServer.deploySchemaThatMustWarn(project, schema2, true).toString should be(
      """{"data":{"deploy":{"migration":{"applied":0,"revision":3},"errors":[],"warnings":[{"description":"You already have nodes for this model. This change may result in data loss."}]}}}""")
  }

  "Deleting a field" should "succeed if no nodes are present" in {

    val schema =
      """type A {
        | name: String! @unique
        | dummy: String
        |}""".stripMargin

    val (project, _) = setupProject(schema)

    val schema2 =
      """type A {
        | name: String! @unique
        |}""".stripMargin

    deployServer.deploySchemaThatMustSucceed(project, schema2).toString should be(
      """{"data":{"deploy":{"migration":{"applied":0,"revision":3},"errors":[],"warnings":[]}}}""")
  }

  // endregion

  //region Update Field

  "DeployMutation" should "be able to change a field from scalar non-list to scalar list" in {

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
        | value: [Int!]!
        |}""".stripMargin

    val updatedProject = deployServer.deploySchema(project, schema2)

    apiServer.query("""query{as{name, value}}""", updatedProject).toString should be("""{"data":{"as":[{"name":"A","value":[]}]}}""")

    apiServer.query("""mutation{updateA(where:{name: "A"} data:{ value: {set: [1,2,3]}}){value}}""", updatedProject).toString should be(
      """{"data":{"updateA":{"value":[1,2,3]}}}""")
  }

  "DeployMutation" should "be able to change a field from scalar list to scalar non-list" in {

    val schema =
      """type A {
        | name: String! @unique
        | value: [Int!]!
        |}""".stripMargin

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{ name: "A", value: {set: [1,2,3]}}){value}}""", project).toString should be(
      """{"data":{"createA":{"value":[1,2,3]}}}""")

    val schema2 =
      """type A {
        | name: String! @unique
        | value: Int
        |}""".stripMargin

    val updatedProject = deployServer.deploySchema(project, schema2)

    apiServer.query("""query{as{name, value}}""", updatedProject).toString should be("""{"data":{"as":[{"name":"A","value":null}]}}""")

    apiServer.query("""mutation{updateA(where:{name: "A"}, data:{value: 1}){name}}""", updatedProject).toString should be(
      """{"data":{"updateA":{"name":"A"}}}""")
  }

  //endregion
}
