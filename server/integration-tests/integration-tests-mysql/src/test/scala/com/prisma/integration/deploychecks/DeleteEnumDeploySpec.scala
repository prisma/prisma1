package com.prisma.integration.deploychecks

import com.prisma.IgnoreSQLite
import com.prisma.integration.IntegrationBaseSpec
import org.scalatest.{FlatSpec, Matchers}

class DeleteEnumDeploySpec extends FlatSpec with Matchers with IntegrationBaseSpec {

  "Deleting an Enum" should "not throw a warning if there is no data yet" taggedAs (IgnoreSQLite) in {

    val schema =
      """|type A {
         |  id: ID! @id
         |  name: String! @unique
         |  enum: AB
         |}
         |
         |enum AB{
         |  A
         |  B
         |}"""

    val (project, _) = setupProject(schema)

    val schema2 =
      """type A {
        |  id: ID! @id
        |  name: String! @unique
        |}"""

    deployServer.deploySchemaThatMustSucceed(project, schema2, 3)
  }

  "Deleting an Enum" should "throw a warning if there is already data" in {

    val schema =
      """|type A {
         |  id: ID! @id
         |  name: String! @unique
         |  enum: AB
         |}
         |
         |enum AB{
         |  A
         |  B
         |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name: "A", enum: A}){name}}""", project)

    val schema2 =
      """type A {
        |  id: ID! @id
        |  name: String! @unique
        |}"""

    deployServer.deploySchemaThatMustWarn(project, schema2).toString should be(
      """{"data":{"deploy":{"migration":null,"errors":[],"warnings":[{"description":"You already have nodes for this model. This change may result in data loss."}]}}}""")
  }

  "Deleting an Enum" should "throw a warning if there is already data but proceed with -force" taggedAs (IgnoreSQLite) in {

    val schema =
      """|type A {
         |  id: ID! @id
         |  name: String! @unique
         |  enum: AB
         |}
         |
         |enum AB{
         |  A
         |  B
         |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name: "A", enum: A}){name}}""", project)

    val schema2 =
      """type A {
        |  id: ID! @id
        |  name: String! @unique
        |}"""

    deployServer.deploySchemaThatMustWarn(project, schema2, force = true).toString should be(
      """{"data":{"deploy":{"migration":{"applied":0,"revision":3},"errors":[],"warnings":[{"description":"You already have nodes for this model. This change may result in data loss."}]}}}""")
  }
}
