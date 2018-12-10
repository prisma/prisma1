package com.prisma.deploy.schema.queries

import com.prisma.deploy.specutils.{ActiveDeploySpecBase, DeploySpecBase}
import org.scalatest.{FlatSpec, Matchers}

class ProjectSpec extends FlatSpec with Matchers with DeploySpecBase {

  "Project query" should "return a project that exists" in {
    val (project, _) = setupProject(basicTypesGql)
    val nameAndStage = testDependencies.projectIdEncoder.fromEncodedString(project.id)
    val result       = server.query(s"""
       |query {
       |  project(name: "${nameAndStage.name}", stage: "${nameAndStage.stage}") {
       |    name
       |    stage
       |  }
       |}
      """.stripMargin)

    result.pathAsString("data.project.name") shouldEqual nameAndStage.name
    result.pathAsString("data.project.stage") shouldEqual nameAndStage.stage
  }

  "Project query" should "return an error if the project does not exist" in {
    val result = server.queryThatMustFail(
      """
       |query {
       |  project(name: "nope", stage: "nope") {
       |    name
       |    stage
       |  }
       |}
      """.stripMargin,
      4000
    )
  }
}
