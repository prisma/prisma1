package cool.graph.deploy.migration

import cool.graph.shared.models._
import org.scalactic.{Bad, Good, Or}

trait MigrationStepError
case class ModelAlreadyExists(name: String)                extends MigrationStepError
case class ModelDoesNotExist(name: String)                 extends MigrationStepError
case class FieldDoesNotExist(model: String, name: String)  extends MigrationStepError
case class FieldAlreadyExists(model: String, name: String) extends MigrationStepError

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
    case x: DeleteModel => deleteModel(project, x)
    case x: CreateField => createField(project, x)
    case x: DeleteField => deleteField(project, x)
    case x              => sys.error(s"The migration step is $x is not implemented yet.")
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

  private def deleteModel(project: Project, deleteModel: DeleteModel): Project Or MigrationStepError = {
    getModel(project, deleteModel.name).flatMap { _ =>
      val newModels  = project.models.filter(_.name != deleteModel.name)
      val newProject = project.copy(models = newModels)
      Good(newProject)
    }
  }

  private def createField(project: Project, createField: CreateField): Project Or MigrationStepError = {
    getModel(project, createField.model).flatMap { model =>
      model.getFieldByName(createField.name) match {
        case None =>
          val newField = Field(
            id = createField.name,
            name = createField.name,
            typeIdentifier = typeIdentifierForTypename(project, createField.typeName),
            isRequired = createField.isRequired,
            isList = createField.isList,
            isUnique = createField.isUnique,
            isSystem = false,
            isReadonly = false
          )
          val newModel = model.copy(fields = model.fields :+ newField)
          Good(replaceModelInProject(project, newModel))
        case Some(_) =>
          Bad(FieldAlreadyExists(createField.model, createField.name))
      }
    }
  }

  private def deleteField(project: Project, deleteField: DeleteField): Project Or MigrationStepError = {
    getModel(project, deleteField.model).flatMap { model =>
      model.getFieldByName(deleteField.name) match {
        case None =>
          Bad(FieldDoesNotExist(deleteField.model, deleteField.name))
        case Some(_) =>
          val newModel = model.copy(fields = model.fields.filter(_.name != deleteField.name))
          Good(replaceModelInProject(project, newModel))
      }
    }
  }

  private def typeIdentifierForTypename(project: Project, typeName: String): TypeIdentifier.Value = {
    if (project.getModelByName(typeName).isDefined) {
      TypeIdentifier.Relation
    } else if (project.getEnumByName(typeName).isDefined) {
      TypeIdentifier.Enum
    } else {
      TypeIdentifier.withName(typeName)
    }
  }

  private def replaceModelInProject(project: Project, model: Model): Project = {
    val newModels = project.models.filter(_.name != model.name) :+ model
    project.copy(models = newModels)
  }

  private def getModel(project: Project, name: String): Model Or MigrationStepError = finder(project.getModelByName(name), ModelDoesNotExist(name))

  private def getField(project: Project, model: String, name: String): Field Or MigrationStepError = getModel(project, model).flatMap(getField(_, name))
  private def getField(model: Model, name: String): Field Or MigrationStepError                    = finder(model.getFieldByName(name), FieldDoesNotExist(model.name, name))

  private def finder[T](fn: => Option[T], error: MigrationStepError): T Or MigrationStepError = {
    fn match {
      case Some(x) => Good(x)
      case None    => Bad(error)
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
