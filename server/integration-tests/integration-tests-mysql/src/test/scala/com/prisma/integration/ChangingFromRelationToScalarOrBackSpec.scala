package com.prisma.integration

import com.prisma.IgnoreSQLite
import org.scalatest.{FlatSpec, Matchers}

class ChangingFromRelationToScalarOrBackSpec extends FlatSpec with Matchers with IntegrationBaseSpec {

  "Changing a field from scalar to relation" should "work when there is no data yet" taggedAs (IgnoreSQLite) in {

    val schema =
      """type A {
        |  a: String! @unique
        |  b: String
        |}
        |
        |type B {
        |  b: String! @unique
        |}"""

    val (project, _) = setupProject(schema)

    val schema1 =
      """type A {
        |  a: String! @unique
        |  b: B
        |}
        |
        |type B {
        |  b: String! @unique
        |}"""

    deployServer.deploySchema(project, schema1)
  }

  "Changing a field from scalar to relation" should "work when there is already data and should delete the old column" taggedAs (IgnoreSQLite) in {

    val schema =
      """type A {
        |  a: String! @unique
        |  b: String
        |}
        |
        |type B {
        |  b: String! @unique
        |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{a:"A", b: "B"}){a}}""", project)

    val as = apiServer.query("""{as{a}}""", project)
    as.toString should be("""{"data":{"as":[{"a":"A"}]}}""")

    val schema1 =
      """type A {
        |  a: String! @unique
        |  b: B
        |}
        |
        |type B {
        |  b: String! @unique
        |}"""

    deployServer.deploySchemaThatMustWarn(project, schema1, force = true)
  }

  "Changing a relation to scalar" should "work when there is no data yet" in {

    val schema =
      """type A {
        |  a: String! @unique
        |  b: B
        |}
        |
        |type B {
        |  b: String! @unique
        |}"""

    val (project, _) = setupProject(schema)

    val schema1 =
      """type A {
        |  a: String! @unique
        |  b: String
        |}
        |
        |type B {
        |  b: String! @unique
        |}"""

    deployServer.deploySchema(project, schema1)
  }

  "Changing a relation to scalar" should "work when there is already data" in {

    val schema =
      """type A {
        |  a: String! @unique
        |  b: B
        |}
        |
        |type B {
        |  b: String! @unique
        |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{a:"A", b: {create:{b: "B"}}}){a}}""", project)

    val as = apiServer.query("""{as{a, b{b}}}""", project)
    as.toString should be("""{"data":{"as":[{"a":"A","b":{"b":"B"}}]}}""")

    val schema1 =
      """type A {
        |  a: String! @unique
        |  b: String
        |}
        |
        |type B {
        |  b: String! @unique
        |}"""

    deployServer.deploySchemaThatMustWarn(project, schema1, force = true)
  }

}
