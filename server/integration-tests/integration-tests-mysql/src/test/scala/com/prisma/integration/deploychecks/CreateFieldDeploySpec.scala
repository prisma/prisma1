package com.prisma.integration.deploychecks

import com.prisma.integration.IntegrationBaseSpec
import org.scalatest.{FlatSpec, Matchers}

class CreateFieldDeploySpec extends FlatSpec with Matchers with IntegrationBaseSpec {

  "Creating a required Field" should "error when nodes already exist and there is no default value" in {

    val schema =
      """type A {
        | name: String! @unique
        |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name:"A"}){name}}""", project).toString should be("""{"data":{"createA":{"name":"A"}}}""")

    val schema2 =
      """type A {
        | name: String! @unique
        | value: Int!
        |}"""

    deployServer.deploySchemaThatMustError(project, schema2).toString should be(
      """{"data":{"deploy":{"migration":{"applied":0,"revision":0},"errors":[{"description":"You are creating a required field but there are already nodes present that would violate that constraint."}],"warnings":[]}}}""")
  }

  "Creating a required Field" should "error when nodes already exist and there is a defaultValue" in {

    val schema =
      """type A {
        | name: String! @unique
        |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name:"A"}){name}}""", project).toString should be("""{"data":{"createA":{"name":"A"}}}""")

    val schema2 =
      """type A {
        | name: String! @unique
        | value: Int! @default(value: 12)
        |}"""

    deployServer.deploySchemaThatMustError(project, schema2).toString should be(
      """{"data":{"deploy":{"migration":{"applied":0,"revision":0},"errors":[{"description":"You are creating a required field but there are already nodes present that would violate that constraint."}],"warnings":[]}}}""")
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

    deployServer.deploySchemaThatMustSucceed(project, schema2, 3)
  }

}
