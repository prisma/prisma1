package cool.graph.deploy.database.schema.mutations

import cool.graph.deploy.specutils.DeploySpecBase
import cool.graph.shared.models.ProjectId
import org.scalatest.{FlatSpec, Matchers}

class DeployMutationSpec extends FlatSpec with Matchers with DeploySpecBase {

  val projectPersistence   = testDependencies.projectPersistence
  val migrationPersistence = testDependencies.migrationPersistence

  "DeployMutation" should "succeed for valid input" in {
    val project      = setupProject(basicTypesGql)
    val nameAndStage = ProjectId.fromEncodedString(project.id)

    val schema = basicTypesGql +
      """
        |type TestModel2 @model {
        |  id: ID! @isUnique
        |  someField: String
        |}
      """.stripMargin

    val result = server.query(s"""
       |mutation {
       |  deploy(input:{name: "${nameAndStage.name}", stage: "${nameAndStage.stage}", types: "${schema.replaceAll("\n", " ")}"}){
       |    project {
       |      name
       |      stage
       |    }
       |    errors {
       |      description
       |    }
       |  }
       |}
      """.stripMargin)

    result.pathAsString("data.deploy.project.name") shouldEqual nameAndStage.name
    result.pathAsString("data.deploy.project.stage") shouldEqual nameAndStage.stage

    val migrations = migrationPersistence.loadAll(project.id).await
    migrations should have(size(3))
    migrations.exists(!_.hasBeenApplied) shouldEqual false
    migrations.head.revision shouldEqual 3 // order is DESC
  }
}
