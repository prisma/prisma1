package com.prisma.deploy.schema.queries

import com.prisma.deploy.specutils.ActiveDeploySpecBase
import com.prisma.shared.models.{Project, ProjectId}
import org.scalatest.{FlatSpec, Matchers}

class GenerateProjectTokenSpec extends FlatSpec with Matchers with ActiveDeploySpecBase {
  val auth = testDependencies.auth

  "the GenerateProjectToken query" should "return a proper token for the requested project" in {
    val (project: Project, _)  = setupProject(schema = basicTypesGql, secrets = Vector("super-duper-secret"))
    val ProjectId(name, stage) = testDependencies.projectIdEncoder.fromEncodedString(project.id)
    val result                 = server.query(s"""
                                       |query {
                                       |  generateProjectToken(name: "$name", stage: "$stage")
                                       |}
      """.stripMargin)

    val token = result.pathAsString("data.generateProjectToken")
    auth.verifyToken(token, project.secrets).isSuccess should be(true)
  }

  "the GenerateProjectToken query" should "return an empty string if the requested project does not have any secrets" in {
    val (project, _)           = setupProject(basicTypesGql)
    val ProjectId(name, stage) = testDependencies.projectIdEncoder.fromEncodedString(project.id)
    val result                 = server.query(s"""
                                                 |query {
                                                 |  generateProjectToken(name: "$name", stage: "$stage")
                                                 |}
      """.stripMargin)

    val token = result.pathAsString("data.generateProjectToken")
    token should equal("")
  }
}
