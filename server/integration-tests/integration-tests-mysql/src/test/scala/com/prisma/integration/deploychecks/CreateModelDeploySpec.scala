package com.prisma.integration.deploychecks

import com.prisma.integration.IntegrationBaseSpec
import org.scalatest.{FlatSpec, Matchers}

class CreateModelDeploySpec extends FlatSpec with Matchers with IntegrationBaseSpec {
  "Creating a Model" should "succeed" in {

    val schema =
      """type A {
        | name: String! @unique
        |}""".stripMargin

    val (project, _) = setupProject(schema)

    val schema2 =
      """type A {
        | name: String! @unique
        |}
        |
        |type B{
        | name: String
        |}
        |""".stripMargin

    deployServer.deploySchemaThatMustSucceed(project, schema2, 3)
  }

}
