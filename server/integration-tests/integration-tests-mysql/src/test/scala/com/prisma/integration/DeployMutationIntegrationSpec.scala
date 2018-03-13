package com.prisma.integration

import org.scalatest.{FlatSpec, Matchers}

class DeployMutationIntegrationSpec extends FlatSpec with Matchers with IntegrationBaseSpec {

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

  "DeployMutation" should "throw an error if a new required field without a default value is added and there are existing nodes." ignore {

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

    val errors = deployServer.deploySchemaThatMustFail(project, schema2)
  }

  "DeployMutation" should "throw a warning if a field is deleted and there are existing nodes." ignore {

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

    val errors = deployServer.deploySchemaThatMustWarn(project, schema2)
  }

}
