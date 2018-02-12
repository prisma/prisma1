package com.prisma.deploy.database.schema.queries

import com.prisma.auth.{AuthImpl, AuthSuccess}
import com.prisma.deploy.specutils.DeploySpecBase
import com.prisma.shared.models.{Project, ProjectId}
import org.scalatest.{FlatSpec, Matchers}

class GenerateProjectTokenSpec extends FlatSpec with Matchers with DeploySpecBase {
  val auth = AuthImpl

  "the GenerateProjectToken query" should "return a proper token for the requested project" in {
    val (project: Project, _)  = setupProject(schema = basicTypesGql, secrets = Vector("super-duper-secret"))
    val ProjectId(name, stage) = ProjectId.fromEncodedString(project.id)
    val result                 = server.query(s"""
                                       |query {
                                       |  generateProjectToken(name: "$name", stage: "$stage")
                                       |}
      """.stripMargin)

    println(project.secrets)
    val token = result.pathAsString("data.generateProjectToken")
    auth.verify(project.secrets, Some(token)) should be(AuthSuccess)
  }
}
