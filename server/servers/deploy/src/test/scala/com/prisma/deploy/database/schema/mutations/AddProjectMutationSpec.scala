package com.prisma.deploy.database.schema.mutations

import cool.graph.cuid.Cuid
import com.prisma.deploy.specutils.DeploySpecBase
import com.prisma.shared.models.ProjectId
import org.scalatest.{FlatSpec, Matchers}

class AddProjectMutationSpec extends FlatSpec with Matchers with DeploySpecBase {

  val projectPersistence = testDependencies.projectPersistence

  "AddProjectMutation" should "succeed for valid input" in {
    val name  = s"${Cuid.createCuid()}~test"
    val stage = Cuid.createCuid()

    val result = server.query(s"""
        |mutation {
        | addProject(input: {
        |   name: "$name",
        |   stage: "$stage"
        | }) {
        |   project {
        |     name
        |     stage
        |   }
        | }
        |}
      """.stripMargin)

    result.pathAsString("data.addProject.project.name") shouldEqual name
    result.pathAsString("data.addProject.project.stage") shouldEqual stage

    projectPersistence.loadAll().await should have(size(1))
  }

  "AddProjectMutation" should "fail if a project already exists" in {
    val (project, _) = setupProject(basicTypesGql)
    val nameAndStage = ProjectId.fromEncodedString(project.id)
    val result = server.queryThatMustFail(
      s"""
       |mutation {
       | addProject(input: {
       |   name: "${nameAndStage.name}",
       |   stage: "${nameAndStage.stage}"
       | }) {
       |   project {
       |     name
       |     stage
       |   }
       | }
       |}
      """.stripMargin,
      4005
    )
  }
}
