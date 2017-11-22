package cool.graph.migration

import cool.graph.deploy.migration.{MigrationStepError, MigrationStepsExecutor, ModelAlreadyExists}
import cool.graph.shared.models.{CreateModel, MigrationStep, MigrationSteps, Project}
import cool.graph.shared.project_dsl.SchemaDsl
import org.scalactic.{Bad, Good, Or}
import org.scalatest.{FlatSpec, Matchers}

class MigrationStepsExecutorSpec extends FlatSpec with Matchers {
  val executor: MigrationStepsExecutor = MigrationStepsExecutor

  "Adding a model to a project" should "succeed if there's no model with name yet" in {
    val project = SchemaDsl().buildProject()
    val result  = executeStep(project, CreateModel("MyModel"))
    val expectedProject = {
      val schema = SchemaDsl()
      schema.model("MyModel")
      schema.buildProject()
    }
    result should equal(Good(expectedProject))
  }

  "Adding a model to a project" should "fail if there's a model with that name already" in {
    val modelName = "MyModel"
    val project = {
      val schema = SchemaDsl()
      schema.model(modelName)
      schema.buildProject()
    }
    val result = executeStep(project, CreateModel(modelName))
    result should equal(Bad(ModelAlreadyExists(modelName)))
  }

  def executeStep(project: Project, migrationStep: MigrationStep): Or[Project, MigrationStepError] = {
    executor.execute(project, MigrationSteps(Vector(migrationStep)))
  }
}
