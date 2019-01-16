package com.prisma.deploy.schema.queries

import com.prisma.deploy.specutils.ActiveDeploySpecBase
import com.prisma.shared.models.{CreateField, CreateModel, Migration, ProjectId}
import org.scalatest.{FlatSpec, Matchers}

class MigrationStatusSpec extends FlatSpec with Matchers with ActiveDeploySpecBase {

  val projectPersistence   = testDependencies.projectPersistence
  val migrationPersistence = testDependencies.migrationPersistence

  "MigrationStatus" should "return the last applied migration if there is no pending migration" in {
    val (project, _) = setupProject(basicTypesGql)
    val nameAndStage = testDependencies.projectIdEncoder.fromEncodedString(project.id)

    val result = server.query(s"""
       |query {
       |  migrationStatus(name: "${nameAndStage.name}", stage: "${nameAndStage.stage}") {
       |    projectId
       |    revision
       |    applied
       |    rolledBack
       |    status
       |    steps {
       |      type
       |    }
       |  }
       |}
      """.stripMargin)

    result.pathAsString("data.migrationStatus.projectId") shouldEqual project.id
    result.pathAsLong("data.migrationStatus.revision") shouldEqual 2
    result.pathAsString("data.migrationStatus.status") shouldEqual "SUCCESS"
    result.pathAsLong("data.migrationStatus.applied") shouldEqual 4
    result.pathAsSeq("data.migrationStatus.steps") shouldNot be(empty)
    result.pathAsLong("data.migrationStatus.rolledBack") shouldEqual 0
  }

  "MigrationStatus" should "return the next pending migration if one exists" in {
    val (project, _) = setupProject(basicTypesGql)
    val nameAndStage = testDependencies.projectIdEncoder.fromEncodedString(project.id)
    val migration = migrationPersistence
      .create(
        Migration(
          projectId = project.id,
          schema = project.schema,
          steps = Vector(
            CreateModel("TestModel"),
            CreateField(
              "TestModel",
              "TestField"
            )
          ),
          functions = Vector.empty
        )
      )
      .await

    val result = server.query(s"""
       |query {
       |  migrationStatus(name: "${nameAndStage.name}", stage: "${nameAndStage.stage}") {
       |    projectId
       |    revision
       |    applied
       |    rolledBack
       |    status
       |    steps {
       |      type
       |    }
       |  }
       |}
      """.stripMargin)

    result.pathAsString("data.migrationStatus.projectId") shouldEqual project.id
    result.pathAsLong("data.migrationStatus.revision") shouldEqual migration.revision
    result.pathAsString("data.migrationStatus.status") shouldEqual "PENDING"
    result.pathAsLong("data.migrationStatus.applied") shouldEqual 0
    result.pathAsLong("data.migrationStatus.rolledBack") shouldEqual 0
  }
}
