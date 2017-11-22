package cool.graph.deploy.migration

import cool.graph.shared.models._
import org.scalactic.{Bad, Good, Or}

trait MigrationStepError
case class ModelAlreadyExists(name: String) extends MigrationStepError

trait MigrationStepsExecutor {
  def execute(project: Project, migrationSteps: MigrationSteps): Project Or MigrationStepError
}

object MigrationStepsExecutor extends MigrationStepsExecutor {
  override def execute(project: Project, migrationSteps: MigrationSteps): Project Or MigrationStepError = {
    val initialResult: Project Or MigrationStepError = Good(project)
    migrationSteps.steps.foldLeft(initialResult) { (previousResult, step) =>
      previousResult match {
        case Good(project) => applyStep(project, step)
        case x @ Bad(_)    => x
      }
    }
  }

  private def applyStep(project: Project, step: MigrationStep): Project Or MigrationStepError = step match {
    case x: CreateModel => createModel(project, x)
    case _              => ???
  }

  private def createModel(project: Project, createModel: CreateModel): Project Or MigrationStepError = {
    project.getModelByName(createModel.name) match {
      case None =>
        val newModel = Model(
          id = createModel.name,
          name = createModel.name,
          description = None,
          isSystem = false,
          fields = List(idField),
          permissions = List.empty,
          fieldPositions = List.empty
        )
        Good(project.copy(models = project.models :+ newModel))
      case Some(_) =>
        Bad(ModelAlreadyExists(createModel.name))
    }
  }

  private val idField = Field(
    id = "id",
    name = "id",
    typeIdentifier = TypeIdentifier.GraphQLID,
    isRequired = true,
    isList = false,
    isUnique = true,
    isSystem = true,
    isReadonly = true
  )
}
