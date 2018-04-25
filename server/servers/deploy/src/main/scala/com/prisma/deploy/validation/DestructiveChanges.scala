package com.prisma.deploy.validation

import com.prisma.deploy.connector.DeployConnector
import com.prisma.deploy.migration.validation.{SchemaError, SchemaWarning, SchemaWarnings}
import com.prisma.shared.errors.SchemaCheckResult
import com.prisma.shared.models._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class DestructiveChanges(deployConnector: DeployConnector, project: Project, nextSchema: Schema, steps: Vector[MigrationStep]) {
  val clientDataResolver = deployConnector.clientDBQueries(project)
  val previousSchema     = project.schema

  def checkAgainstExistingData: Future[Vector[SchemaCheckResult]] = {
    val checkResults = steps.map {
      case x: CreateModel    => validationSuccessful
      case x: DeleteModel    => deleteModelValidation(x)
      case x: UpdateModel    => validationSuccessful
      case x: CreateField    => createFieldValidation(x)
      case x: DeleteField    => deleteFieldValidation(x)
      case x: UpdateField    => updateFieldValidation(x)
      case x: CreateEnum     => validationSuccessful
      case x: DeleteEnum     => deleteEnumValidation(x)
      case x: UpdateEnum     => updateEnumValidation(x)
      case x: CreateRelation => createRelationValidation(x)
      case x: DeleteRelation => deleteRelationValidation(x)
      case x: UpdateRelation => updateRelationValidation(x)
      case x: UpdateSecrets  => validationSuccessful
    }

    Future.sequence(checkResults).map(_.flatten)
  }

  private def deleteModelValidation(x: DeleteModel) = {
    clientDataResolver.existsByModel(x.name).map {
      case true  => Vector(SchemaWarnings.dataLossModel(x.name))
      case false => Vector.empty
    }
  }

  private def createFieldValidation(x: CreateField) = {
    previousSchema.getModelByName(x.model) match {
      case Some(existingModel) =>
        x.relation.isEmpty && x.isRequired match {
          case true =>
            clientDataResolver.existsByModel(existingModel.name).map {
              case true =>
                Vector(
                  SchemaError(`type` = existingModel.name,
                              description = s"You are creating a required field but there are already nodes present that would violate that constraint."))
              case false => Vector.empty
            }

          case false =>
            validationSuccessful
        }

      case None =>
        validationSuccessful
    }
  }

  private def deleteFieldValidation(x: DeleteField) = {
    val model    = previousSchema.getModelByName_!(x.model)
    val isScalar = model.fields.find(_.name == x.name).get.isScalar

    if (isScalar) {
      clientDataResolver.existsByModel(model.name).map {
        case true  => Vector(SchemaWarnings.dataLossField(x.name, x.name))
        case false => Vector.empty
      }
    } else {
      validationSuccessful
    }
  }

  private def updateFieldValidation(x: UpdateField) = {
    val model                               = previousSchema.getModelByName_!(x.model)
    val oldField                            = model.fields.find(_.name == x.name).get
    val cardinalityChanges                  = x.isList.isDefined
    val typeChanges                         = x.typeName.isDefined
    val goesFromRelationToScalarOrViceVersa = x.relation.isDefined

    val becomesRequired = x.isRequired.contains(true)

    def warnings: Future[Vector[SchemaWarning]] = cardinalityChanges || typeChanges || goesFromRelationToScalarOrViceVersa match {
      case true =>
        clientDataResolver.existsByModel(model.name).map {
          case true  => Vector(SchemaWarnings.dataLossField(x.name, x.name))
          case false => Vector.empty
        }
      case false =>
        validationSuccessful
    }

    def errors: Future[Vector[SchemaError]] = becomesRequired match {
      case true =>
        clientDataResolver.existsNullByModelAndField(model, oldField).map {
          case true =>
            Vector(
              SchemaError(`type` = model.name,
                          field = oldField.name,
                          "You are making a field required, but there are already nodes that would violate that constraint."))
          case false => Vector.empty
        }

      case false =>
        validationSuccessful
    }

    for {
      warnings: Vector[SchemaWarning] <- warnings
      errors: Vector[SchemaError]     <- errors
    } yield {
      warnings ++ errors
    }
  }

  private def deleteEnumValidation(x: DeleteEnum) = {
    //already covered by deleteField
    validationSuccessful
  }

  private def updateEnumValidation(x: UpdateEnum) = {
    val oldEnum = previousSchema.enums.find(_.name == x.name)
    val deletedValues: Vector[String] = x.values match {
      case None            => Vector.empty
      case Some(newValues) => oldEnum.get.values.filter(value => !newValues.contains(value))
    }

    if (deletedValues.nonEmpty) {
      val modelsWithFieldsThatUseEnum = previousSchema.models.filter(m => m.fields.exists(f => f.enum.isDefined && f.enum.get.name == x.name)).toVector
      val res = deletedValues.map { deletedValue =>
        clientDataResolver.enumValueIsInUse(modelsWithFieldsThatUseEnum, x.name, deletedValue).map {
          case true  => Vector(SchemaError.global(s"You are deleting the value '$deletedValue' of the enum '${x.name}', but that value is in use."))
          case false => Vector.empty
        }
      }
      Future.sequence(res).map(_.flatten)

    } else {
      validationSuccessful
    }
  }

  private def createRelationValidation(x: CreateRelation) = {

    val nextRelation = nextSchema.relations.find(_.name == x.name).get

    def checkRelationSide(modelName: String) = {
      val nextModelA      = nextSchema.models.find(_.name == modelName).get
      val nextModelAField = nextModelA.fields.find(field => field.relation.contains(nextRelation))

      val modelARequired = nextModelAField match {
        case None        => false
        case Some(field) => field.isRequired
      }

      if (modelARequired) previousSchema.models.find(_.name == modelName) match {
        case Some(model) =>
          clientDataResolver.existsByModel(model.name).map {
            case true =>
              Vector(SchemaError(`type` = model.name, s"You are creating a required relation, but there are already nodes that would violate that constraint."))
            case false => Vector.empty
          }

        case None => validationSuccessful
      } else {
        validationSuccessful
      }
    }

    val checks = Vector(checkRelationSide(x.modelAName), checkRelationSide(x.modelBName))

    Future.sequence(checks).map(_.flatten)
  }

  private def deleteRelationValidation(x: DeleteRelation) = {
    val previousRelation = previousSchema.relations.find(_.name == x.name).get

    clientDataResolver.existsByRelation(previousRelation.relationTableName).map {
      case true  => Vector(SchemaWarnings.dataLossRelation(x.name))
      case false => Vector.empty
    }
  }

  private def updateRelationValidation(x: UpdateRelation) = {
    // becomes required is handled by the change on the updateField
    // todo cardinality change

    validationSuccessful
  }

  private def validationSuccessful = Future.successful(Vector.empty)
}
