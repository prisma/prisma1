package com.prisma.deploy.schema.mutations

import com.prisma.deploy.specutils.{ActiveDeploySpecBase, DeploySpecBase}
import cool.graph.cuid.Cuid
import org.scalatest.{FlatSpec, Matchers}

class DeleteServiceMutationSpec extends FlatSpec with Matchers with DeploySpecBase {

  val projectPersistence = testDependencies.projectPersistence

  "DeleteServiceMutation" should "succeed for valid input" in {
    val name  = Cuid.createCuid()
    val stage = Cuid.createCuid()

    server.query(s"""
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

    projectPersistence.loadAll().await should have(size(1))

    val result = server.query(s"""
                             |mutation {
                             | deleteProject(input: {
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

    result.pathAsString("data.deleteProject.project.name") shouldEqual name
    result.pathAsString("data.deleteProject.project.stage") shouldEqual stage

    projectPersistence.loadAll().await should have(size(0))
  }
}
