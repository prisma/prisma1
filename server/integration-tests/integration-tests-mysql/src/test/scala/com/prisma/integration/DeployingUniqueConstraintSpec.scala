package com.prisma.integration

import org.scalatest.{FlatSpec, Matchers}

class DeployingUniqueConstraintSpec extends FlatSpec with Matchers with IntegrationBaseSpec {

  "Adding a unique constraint with violating data" should "throw a deploy error" in {

    val schema =
      """type Team {
        |  name: String! @unique
        |  dummy: String
        |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createTeam(data:{name:"Bayern", dummy: "String"}){name}}""", project)
    apiServer.query("""mutation{createTeam(data:{name:"Real", dummy: "String"}){name}}""", project)

    val schema1 =
      """type Team {
        |  name: String! @unique
        |  dummy: String @unique
        |}"""

    val res = deployServer.deploySchemaThatMustError(project, schema1)
    res.toString should be(
      """{"data":{"deploy":{"migration":{"applied":0,"revision":0},"errors":[{"description":"You are making a field unique, but there are already nodes that would violate that constraint."}],"warnings":[]}}}""")
  }

  "Adding a unique constraint without violating data" should "work" in {

    val schema =
      """type Team {
        |  name: String! @unique
        |  dummy: String
        |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createTeam(data:{name:"Bayern", dummy: "String"}){name}}""", project)
    apiServer.query("""mutation{createTeam(data:{name:"Real", dummy: "String2"}){name}}""", project)

    val schema1 =
      """type Team {
        |  name: String! @unique
        |  dummy: String @unique
        |}"""

    deployServer.deploySchemaThatMustSucceed(project, schema1, 3)
  }

  "Adding a new String field with a unique constraint" should "work" in {

    val schema =
      """type Team {
        |  name: String! @unique
        |  dummy: String
        |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createTeam(data:{name:"Bayern", dummy: "String"}){name}}""", project)
    apiServer.query("""mutation{createTeam(data:{name:"Real", dummy: "String2"}){name}}""", project)

    val schema1 =
      """type Team {
        |  name: String! @unique
        |  dummy: String
        |  newField: String @unique
        |}"""

    deployServer.deploySchemaThatMustSucceed(project, schema1, 3)
  }

  "Adding a new Int field with a unique constraint" should "work" in {

    val schema =
      """type Team {
        |  name: String! @unique
        |  dummy: String
        |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createTeam(data:{name:"Bayern", dummy: "String"}){name}}""", project)
    apiServer.query("""mutation{createTeam(data:{name:"Real", dummy: "String2"}){name}}""", project)

    val schema1 =
      """type Team {
        |  name: String! @unique
        |  dummy: String
        |  newField: Int @unique
        |}"""

    deployServer.deploySchemaThatMustSucceed(project, schema1, 3)
  }
}
