package com.prisma.integration.deploychecks

import com.prisma.integration.IntegrationBaseSpec
import org.scalatest.{FlatSpec, Matchers}

class UpdateRelationDeploySpec extends FlatSpec with Matchers with IntegrationBaseSpec {

  "Updating a relation to make it required" should "should suceed if there is no data yet " in {

    val schema =
      """|type A {
         | name: String! @unique
         | b: B
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
         | b: B!
         |}
         |
         |type B {
         | name: String! @unique
         | a: A
         |}"""

    deployServer.deploySchemaThatMustSucceed(project, schema2, 3)
  }

  "Updating a relation to make it required" should "should suceed if there is data but the required relation is fulfilled " in {

    val schema =
      """|type A {
         | name: String! @unique
         | b: B
         |}
         |
         |type B {
         | name: String! @unique
         | a: A
         |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name: "A", b :{create:{name: "B"}}}){name}}""", project)
    apiServer.query("""mutation{createB(data:{name: "A"}){name}}""", project)

    val schema2 =
      """|type A {
         | name: String! @unique
         | b: B!
         |}
         |
         |type B {
         | name: String! @unique
         | a: A
         |}"""

    deployServer.deploySchemaThatMustSucceed(project, schema2, 3)
  }

  "Updating a relation to make it required" should "should error if there is data but the required relation is violated " in {

    val schema =
      """|type A {
         | name: String! @unique
         | b: B
         |}
         |
         |type B {
         | name: String! @unique
         | a: A
         |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name: "A", b :{create:{name: "B"}}}){name}}""", project)
    apiServer.query("""mutation{createA(data:{name: "A2"}){name}}""", project)

    val schema2 =
      """|type A {
         | name: String! @unique
         | b: B!
         |}
         |
         |type B {
         | name: String! @unique
         | a: A
         |}"""

    deployServer.deploySchemaThatMustError(project, schema2).toString should be(
      """{"data":{"deploy":{"migration":null,"errors":[{"description":"You are making a field required, but there are already nodes with null values that would violate that constraint."}],"warnings":[]}}}""")
  }

}
