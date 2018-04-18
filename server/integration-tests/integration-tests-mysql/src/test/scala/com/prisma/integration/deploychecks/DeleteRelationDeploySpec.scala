package com.prisma.integration.deploychecks

import com.prisma.integration.IntegrationBaseSpec
import org.scalatest.{FlatSpec, Matchers}

class DeleteRelationDeploySpec extends FlatSpec with Matchers with IntegrationBaseSpec {

  "Deleting a relation when there is no data" should "succeed" in {

    val schema =
      """|type A {
         | name: String! @unique
         | b: B!
         |}
         |
         |type B {
         | name: String! @unique
         | a: A
         |}"""

    val (project, _) = setupProject(schema)

    val schema2 =
      """|type C {
         | name: String! @unique
         |}
         |"""

    deployServer.deploySchemaThatMustSucceed(project, schema2, 3)
  }

  "Deleting a relation when there is data" should "should warn" in {

    val schema =
      """|type A {
         | name: String! @unique
         | b: B!
         |}
         |
         |type B {
         | name: String! @unique
         | a: A
         |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name: "A", b :{create:{name: "B"}}}){name}}""", project)

    val schema2 =
      """|type A {
         | name: String! @unique
         |}
         |
         |type B {
         | name: String! @unique
         |}"""

    deployServer.deploySchemaThatMustWarn(project, schema2).toString should be(
      """{"data":{"deploy":{"migration":{"applied":0,"revision":0},"errors":[],"warnings":[{"description":"You already have nodes for this relation. This change will result in data loss."}]}}}""")
  }

}
