package com.prisma.integration.deploychecks

import com.prisma.{ConnectorAwareTest, IgnoreSQLite}
import com.prisma.integration.IntegrationBaseSpec
import org.scalatest.{FlatSpec, Matchers}

class DeleteFieldDeploySpec extends FlatSpec with Matchers with IntegrationBaseSpec {

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
      """{"data":{"deploy":{"migration":null,"errors":[],"warnings":[{"description":"You already have nodes for this model. This change may result in data loss."}]}}}""")
  }

  "Deleting a field" should "throw a warning if nodes are present but proceed with -force flag" taggedAs (IgnoreSQLite) in {

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

  "Deleting a field" should "succeed if no nodes are present" taggedAs (IgnoreSQLite) in {

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

    deployServer.deploySchemaThatMustSucceed(project, schema2, 3)
  }

}
