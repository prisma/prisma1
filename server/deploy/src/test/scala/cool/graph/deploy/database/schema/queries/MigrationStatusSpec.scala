package cool.graph.deploy.database.schema.queries

import cool.graph.deploy.specutils.DeploySpecBase
import cool.graph.shared.models.{CreateField, CreateModel, Migration, ProjectId}
import org.scalatest.{FlatSpec, Matchers}

class MigrationStatusSpec extends FlatSpec with Matchers with DeploySpecBase {

  val projectPersistence   = testDependencies.projectPersistence
  val migrationPersistence = testDependencies.migrationPersistence

  "MigrationStatus" should "return the last applied migration if there is no pending migration" in {
    val project      = setupProject(basicTypesGql)
    val nameAndStage = ProjectId.fromEncodedString(project.id)

    val result = server.query(s"""
       |query {
       |  migrationStatus(name: "${nameAndStage.name}", stage: "${nameAndStage.stage}") {
       |    projectId
       |    revision
       |    progress
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
    result.pathAsString("data.migrationStatus.progress") shouldEqual "0/4"
    result.pathAsSeq("data.migrationStatus.steps") shouldNot be(empty)
  }

  "MigrationStatus" should "return the next pending migration if one exists" in {
    val project      = setupProject(basicTypesGql)
    val nameAndStage = ProjectId.fromEncodedString(project.id)
    val migration = migrationPersistence
      .create(
        Migration(
          project.id,
          project.schema,
          Vector(
            CreateModel("TestModel"),
            CreateField(
              "TestModel",
              "TestField",
              "String",
              isRequired = false,
              isList = false,
              isUnique = false,
              None,
              None,
              None
            )
          )
        )
      )
      .await

    val result = server.query(s"""
       |query {
       |  migrationStatus(name: "${nameAndStage.name}", stage: "${nameAndStage.stage}") {
       |    projectId
       |    revision
       |    progress
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
    result.pathAsString("data.migrationStatus.progress") shouldEqual "0/2"
  }
}
