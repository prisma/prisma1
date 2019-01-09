package com.prisma.deploy.schema.queries

import com.prisma.deploy.specutils.ActiveDeploySpecBase
import com.prisma.shared.models.ProjectId
import org.scalatest.{FlatSpec, Matchers}

class ListProjectsSpec extends FlatSpec with Matchers with ActiveDeploySpecBase {
  "ListProjects" should "return an empty list with no projects" in {
    val result = server.query(s"""
       |query {
       |  listProjects {
       |    name
       |    stage
       |  }
       |}
      """.stripMargin)

    result.pathAsSeq("data.listProjects") should have(size(0))
  }

  "MigrationStatus" should "return all projects" in {
    val (project, _)  = setupProject(basicTypesGql, stage = "stage1")
    val (project2, _) = setupProject(basicTypesGql, stage = "stage2")
    val (project3, _) = setupProject(basicTypesGql, stage = "stage3")
    val result        = server.query(s"""
       |query {
       |  listProjects {
       |    name
       |    stage
       |  }
       |}
      """.stripMargin)

    result
      .pathAsSeq("data.listProjects")
      .map(p => s"${p.pathAsString("name")}${testDependencies.projectIdEncoder.stageSeparator}${p.pathAsString("stage")}") should contain allOf (
      project.id,
      project2.id,
      project3.id
    )
  }
}
