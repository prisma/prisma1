package com.prisma.deploy.validation

import com.prisma.deploy.connector.DeployConnector
import com.prisma.deploy.migration.validation.{SchemaError, SchemaWarning}
import com.prisma.shared.errors.SchemaCheckResult
import com.prisma.shared.models._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class DestructiveChanges(persistencePlugin: DeployConnector, project: Project, nextSchema: Schema, steps: Vector[MigrationStep]) {
  val clientDataResolver = persistencePlugin.clientDBQueries(project)
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
      case x: DeleteRelation => deleteRelationValidation(x)
      case x: CreateRelation => createRelationValidation(x)
      case x: UpdateRelation => updateRelationValidation
      case x: UpdateSecrets  => validationSuccessful
    }

    Future.sequence(checkResults).map(_.flatten)
  }

  private def deleteModelValidation(x: DeleteModel) = {
    clientDataResolver.existsByModel(x.name).map {
      case true  => Vector(SchemaWarning.dataLossModel(x.name))
      case false => Vector.empty
    }
  }

  private def createFieldValidation(x: CreateField) = {
    project.schema.getModelByName(x.model) match {
      case Some(existingModel) =>
        x.relation.isEmpty && x.isRequired && x.defaultValue.isEmpty match {
          case true =>
            clientDataResolver.existsByModel(existingModel.name).map {
              case true  => Vector(SchemaError.global("You are creating a required field without a defaultValue but there are already nodes present."))
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
    val model = project.schema.getModelByName_!(x.model)

    clientDataResolver.existsByModel(model.name).map {
      case true  => Vector(SchemaWarning.dataLossField(x.name, x.name))
      case false => Vector.empty
    }
  }

  private def updateFieldValidation(x: UpdateField) = {
    val model                               = project.schema.getModelByName_!(x.model)
    val oldField                            = model.fields.find(_.name == x.name).get
    val cardinalityChanges                  = x.isList.isDefined
    val typeChanges                         = x.typeName.isDefined
    val goesFromRelationToScalarOrViceVersa = x.relation.isDefined

    val becomesRequired = x.isRequired.contains(true)

    def warnings: Future[Vector[SchemaWarning]] = cardinalityChanges || typeChanges || goesFromRelationToScalarOrViceVersa match {
      case true =>
        clientDataResolver.existsByModel(model.name).map {
          case true  => Vector(SchemaWarning.dataLossField(x.name, x.name))
          case false => Vector.empty
        }
      case false =>
        validationSuccessful
    }

    def errors: Future[Vector[SchemaError]] = becomesRequired match {
      case true =>
        clientDataResolver.existsNullByModelAndField(model, oldField).map {
          case true  => Vector(SchemaError.global("You are making a field required, but there are already nodes that would violate that constraint."))
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

//    val modelsWithFieldsThatUseEnum = project.models.filter(m => m.fields.exists(f => f.enum.isDefined && f.enum.get.name == x.name))
//    val res = modelsWithFieldsThatUseEnum.map(model =>
//      clientDataResolver.existsByModel(model.name).map {
//        case true  => Vector(SchemaWarning.dataLossModel(model.name))
//        case false => Vector.empty
//    })
//
//    Future.sequence(res).map(_.flatten.toVector)

    validationSuccessful
  }

  private def updateEnumValidation(x: UpdateEnum) = {
    val oldEnum = project.enums.find(_.name == x.name)
    val deletedValues: Vector[String] = x.values match {
      case None            => Vector.empty
      case Some(newValues) => oldEnum.get.values.filter(value => !newValues.contains(value))
    }

    if (deletedValues.nonEmpty) {
      val modelsWithFieldsThatUseEnum = project.models.filter(m => m.fields.exists(f => f.enum.isDefined && f.enum.get.name == x.name)).toVector
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

    //check which models already existed

//    for {
//      model
//
//
//    }
//
//
//
//
//    project.schema.getModelByName(x.) match {
//      case Some(existingModel) =>
//        x.relation.isEmpty && x.isRequired && x.defaultValue.isEmpty match {
//          case true =>
//            clientDataResolver.existsByModel(existingModel.name).map {
//              case true  => Vector(SchemaError.global("You are creating a required field without a defaultValue but there are already nodes present."))
//              case false => Vector.empty
//            }
//
//          case false =>
//            validationSuccessful
//        }
//
//      case None =>
//        validationSuccessful
//    }

    // required relation and already nodes exist -> error

    validationSuccessful
  }

  private def deleteRelationValidation(x: DeleteRelation) = {
    clientDataResolver.existsByRelation(x.name).map {
      case true  => Vector(SchemaWarning.dataLossModel(x.name)) //todo could also warn on the field level
      case false => Vector.empty
    }
  }

  private def updateRelationValidation = {
    // becomes required -> nodes exist without relation -> error
    // becomes to one -> possibly to many entries

    // todo cardinality change? how is that handled?
    validationSuccessful
  }

  private def validationSuccessful = {
    Future.successful(Vector.empty)
  }
}
