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

    apiServer.executeQuerySimple("""mutation{createA(data:{name: "A", value: 1}){name}}""", project)

    val schema2 =
      """type A {
        | name: String! @unique
        | value: [Int!]!
        |}""".stripMargin

    val updatedProject = deployServer.deploySchema(project, schema2).await.get

    apiServer.executeQuerySimple("""query{as{name, value}}""", updatedProject).toString should be("""{"data":{"as":[{"name":"A","value":[]}]}}""")

    apiServer.executeQuerySimple("""mutation{updateA(where:{name: "A"} data:{ value: {set: [1,2,3]}}){value}}""", updatedProject).toString should be(
      """{"data":{"updateA":{"value":[1,2,3]}}}""")
  }

  "DeployMutation" should "be able to change a field from scalar list to scalar non-list" in {

    val schema =
      """type A {
        | name: String! @unique
        | value: [Int!]!
        |}""".stripMargin

    val (project, _) = setupProject(schema)

    apiServer.executeQuerySimple("""mutation{createA(data:{ name: "A", value: {set: [1,2,3]}}){value}}""", project).toString should be(
      """{"data":{"createA":{"value":[1,2,3]}}}""")

    val schema2 =
      """type A {
        | name: String! @unique
        | value: Int
        |}""".stripMargin

    val updatedProject = deployServer.deploySchema(project, schema2).await.get

    apiServer.executeQuerySimple("""query{as{name, value}}""", updatedProject).toString should be("""{"data":{"as":[{"name":"A","value":null}]}}""")

    apiServer.executeQuerySimple("""mutation{updateA(where:{name: "A"}, data:{value: 1}){name}}""", updatedProject).toString should be(
      """{"data":{"updateA":{"name":"A"}}}""")
  }
}
