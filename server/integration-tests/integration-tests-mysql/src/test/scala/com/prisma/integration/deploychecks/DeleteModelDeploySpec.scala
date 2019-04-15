package com.prisma.integration.deploychecks

import com.prisma.integration.IntegrationBaseSpec
import org.scalatest.{FlatSpec, Matchers}

class DeleteModelDeploySpec extends FlatSpec with Matchers with IntegrationBaseSpec {

  "Deleting a model" should "throw a warning if nodes already exist" in {

    val schema =
      """|type A {
         |  id: ID! @id
         |  name: String! @unique
         |  value: Int
         |}
         |
         |type B {
         |  id: ID! @id
         |  name: String! @unique
         |  value: Int
         |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name: "A", value: 1}){name}}""", project)

    val schema2 =
      """type B {
        |  id: ID! @id
        |  name: String! @unique
        |  value: Int
        |}"""

    deployServer.deploySchemaThatMustWarn(project, schema2).toString should be(
      """{"data":{"deploy":{"migration":null,"errors":[],"warnings":[{"description":"You already have nodes for this model. This change will result in data loss."}]}}}""")
  }

  "Deleting a model" should "throw a warning if nodes already exist but proceed if the -force flag is present" in {

    val schema =
      """|type A {
         |  id: ID! @id
         |  name: String! @unique
         |  value: Int
         |}
         |
         |type B {
         |  id: ID! @id
         |  name: String! @unique
         |  value: Int
         |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name: "A", value: 1}){name}}""", project)

    val schema2 =
      """type B {
        |  id: ID! @id
        |  name: String! @unique
        |  value: Int
        |}"""

    deployServer.deploySchemaThatMustWarn(project, schema2, force = true).toString should be(
      """{"data":{"deploy":{"migration":{"applied":0,"revision":3},"errors":[],"warnings":[{"description":"You already have nodes for this model. This change will result in data loss."}]}}}""")
  }

  "Deleting a model" should "not throw a warning or error if no nodes  exist" in {

    val schema =
      """|type A {
         |  id: ID! @id
         |  name: String! @unique
         |  value: Int
         |}
         |
         |type B {
         |  id: ID! @id
         |  name: String! @unique
         |  value: Int
         |}"""

    val (project, _) = setupProject(schema)

    val schema2 =
      """type B {
        |  id: ID! @id
        |  name: String! @unique
        |  value: Int
        |}"""

    deployServer.deploySchemaThatMustSucceed(project, schema2, 3)
  }

}
