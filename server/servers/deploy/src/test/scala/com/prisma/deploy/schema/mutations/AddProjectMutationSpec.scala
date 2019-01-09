package com.prisma.deploy.schema.mutations

import com.prisma.deploy.specutils.DeploySpecBase
import cool.graph.cuid.Cuid
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
    val name  = s"${Cuid.createCuid()}~test"
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

    server.queryThatMustFail(
      s"""
       |mutation {
       | addProject(input: {
       |   name: "${name}",
       |   stage: "${stage}"
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

  "AddProjectMutation" should "fail if a service name is reserved" in {
    testDependencies.projectIdEncoder.reservedServiceAndStageNames.foreach { reserved =>
      server.queryThatMustFail(
        s"""
           |mutation {
           |  addProject(input: {
           |    name: "$reserved",
           |    stage: "default"
           |  }) {
           |    project {
           |      name
           |      stage
           |    }
           |  }
           |}
      """.stripMargin,
        4006
      )
    }
  }

  "AddProjectMutation" should "fail if a stage name is reserved" in {
    testDependencies.projectIdEncoder.reservedServiceAndStageNames.foreach { reserved =>
      server.queryThatMustFail(
        s"""
           |mutation {
           |  addProject(input: {
           |    name: "default",
           |    stage: "$reserved"
           |  }) {
           |    project {
           |      name
           |      stage
           |    }
           |  }
           |}
      """.stripMargin,
        4007
      )
    }
  }
}
