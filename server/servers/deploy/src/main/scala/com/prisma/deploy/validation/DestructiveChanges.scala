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
      case x: DeleteEnum     => deleteEnumValidation
      case x: UpdateEnum     => updateEnumValidation
      case x: DeleteRelation => deleteRelationValidation(x)
      case x: CreateRelation => createRelationValidation
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
        x.isRequired && x.defaultValue.isEmpty match {
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
    val model = project.schema.getModelByName_!(x.model)
    //data loss
    // to relation -> warning
    // to from list -> warning
    // typechange -> warning

    //changing to required and no defValue
    // existing data -> error
    // relations -> maybe error

    //existing unchanged required is also dangerous
    // to/from  relation change -> error

    val cardinalityChanges                  = x.isList.isDefined   // warning
    val typeChanges                         = x.typeName.isDefined // warning
    val goesFromRelationToScalarOrViceVersa = x.relation.isDefined // warning

    val becomesRequiredAndNoDefault = false //x.isRequired.contains(true) // todo error but needs to check defaultValue

    if (cardinalityChanges || typeChanges || goesFromRelationToScalarOrViceVersa || becomesRequiredAndNoDefault) {

      clientDataResolver.existsByModel(model.name).map {
        case true =>
          val warning =
            if (cardinalityChanges || typeChanges || goesFromRelationToScalarOrViceVersa) Vector(SchemaWarning.dataLossField(x.name, x.name)) else Vector.empty
          val error = Vector.empty
//            if (becomesRequiredAndNoDefault)
//              Vector(SchemaError.global("You are setting a field required without a defaultValue but there are already nodes present."))
//            else Vector.empty

          warning ++ error

        case false => Vector.empty
      }

    } else {
      validationSuccessful
    }
  }

  private def deleteEnumValidation = {
    //potential data loss on all models that use that enum -> warning

    //should be covered by SchemaValidation

    validationSuccessful
  }

  private def updateEnumValidation = {
    //todo

    //deleted values in use somewhere? -> error

    //error if deleted case in use

    validationSuccessful
  }

  private def createRelationValidation = {
    //todo

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
