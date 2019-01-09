package com.prisma.integration.deploychecks

import com.prisma.integration.IntegrationBaseSpec
import org.scalatest.{FlatSpec, Matchers}

class CreateRelationDeploySpec extends FlatSpec with Matchers with IntegrationBaseSpec {

  "Creating a relation that is not required" should "just work even if there are already nodes" in {

    val schema =
      """|type A {
         | name: String! @unique
         |}
         |
         |type B {
         | name: String! @unique
         |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name: "A"}){name}}""", project)
    apiServer.query("""mutation{createB(data:{name: "B"}){name}}""", project)

    val schema2 =
      """|type A {
         | name: String! @unique
         | b: B
         |}
         |
         |type B {
         | name: String! @unique
         | a: [A]
         |}"""

    deployServer.deploySchemaThatMustSucceed(project, schema2, 3)
  }

  "Creating a relation that is required" should "just work if there are no nodes yet" in {

    val schema =
      """|type A {
         | name: String! @unique
         |}
         |
         |type B {
         | name: String! @unique
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
         | a: A!
         |}"""

    deployServer.deploySchemaThatMustSucceed(project, schema2, 3)
  }

  "Creating a relation that is required" should "error if there are already nodes" in {

    val schema =
      """|type A {
         | name: String! @unique
         |}
         |
         |type B {
         | name: String! @unique
         |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name: "A"}){name}}""", project)
    apiServer.query("""mutation{createB(data:{name: "B"}){name}}""", project)

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
      """{"data":{"deploy":{"migration":null,"errors":[{"description":"You are creating a required relation, but there are already nodes that would violate that constraint."}],"warnings":[]}}}""")
  }

  "Creating a relation that is required together with a new model" should "error if there are already nodes" in {

    val schema =
      """|type A {
         | name: String! @unique
         |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name: "A"}){name}}""", project)

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
      """{"data":{"deploy":{"migration":null,"errors":[{"description":"You are creating a required relation, but there are already nodes that would violate that constraint."}],"warnings":[]}}}""")
  }

  "Creating a relation that is required with both models new" should "work" in {

    val schema =
      """|type C {
         | name: String! @unique
         |}
         |"""

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

}
