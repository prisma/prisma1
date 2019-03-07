package com.prisma.integration.deploychecks

import com.prisma.integration.IntegrationBaseSpec
import org.scalatest.{FlatSpec, Matchers}

class CreateEnumDeploySpec extends FlatSpec with Matchers with IntegrationBaseSpec {

  "Creating an Enum " should "succeed even when there are nodes for the models where it is added" in {

    val schema =
      """type A {
        | name: String! @unique
        |}""".stripMargin

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name:"A"}){name}}""", project).toString should be("""{"data":{"createA":{"name":"A"}}}""")

    val schema2 =
      """type A {
        | name: String! @unique
        | value: AB
        |}
        |
        |enum AB{
        | A
        | B
        |}
        |""".stripMargin

    deployServer.deploySchemaThatMustSucceed(project, schema2, 3)
  }

  "Creating an Enum and making it required on field " should "error there are nodes for the models where it is added" in {

    val schema =
      """type A {
        | name: String! @unique
        |}""".stripMargin

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name:"A"}){name}}""", project).toString should be("""{"data":{"createA":{"name":"A"}}}""")

    val schema2 =
      """type A {
        | name: String! @unique
        | value: AB!
        |}
        |
        |enum AB{
        | A
        | B
        |}
        |""".stripMargin

    deployServer.deploySchemaThatMustWarn(project, schema2, force = true).toString should include("""The fields will be pre-filled with the value `A`.""")
  }
}
