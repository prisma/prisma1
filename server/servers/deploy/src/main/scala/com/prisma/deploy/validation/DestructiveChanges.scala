package com.prisma.deploy.validation

import com.prisma.deploy.connector.ClientDbQueries
import com.prisma.deploy.migration.validation.{DeployError, DeployResult, DeployWarning, DeployWarnings}
import com.prisma.shared.models._
import org.scalactic.{Bad, Good, Or}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class DestructiveChanges(clientDbQueries: ClientDbQueries, project: Project, nextSchema: Schema, steps: Vector[MigrationStep]) {
  val previousSchema = project.schema

  def check: Future[Vector[DeployWarning] Or Vector[DeployError]] = {
    checkAgainstExistingData.map { results =>
      val destructiveWarnings: Vector[DeployWarning] = results.collect { case warning: DeployWarning => warning }
      val inconsistencyErrors: Vector[DeployError]   = results.collect { case error: DeployError     => error }
      if (inconsistencyErrors.isEmpty) {
        Good(destructiveWarnings)
      } else {
        Bad(inconsistencyErrors)
      }
    }
  }

  private def checkAgainstExistingData: Future[Vector[DeployResult]] = {
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
    val model = previousSchema.getModelByName_!(x.name)
    clientDbQueries.existsByModel(model).map {
      case true  => Vector(DeployWarnings.dataLossModel(x.name))
      case false => Vector.empty
    }
  }

  private def createFieldValidation(x: CreateField) = {
    val field = nextSchema.getFieldByName_!(x.model, x.name)

    def newRequiredScalarField(model: Model) = field.isScalar && field.isRequired match {
      case true =>
        clientDbQueries.existsByModel(model).map {
          case true =>
            Vector(
              DeployError(`type` = model.name,
                          description = s"You are creating a required field but there are already nodes present that would violate that constraint."))
          case false => Vector.empty
        }

      case false =>
        validationSuccessful
    }
    def newToOneBackRelationField(model: Model) = {
      field match {
        case rf: RelationField if !rf.isList && previousSchema.relations.exists(rel => rel.name == rf.relation.name) =>
          val previousRelation                   = previousSchema.getRelationByName_!(rf.relation.name)
          val relationSideThatCantHaveDuplicates = if (previousRelation.modelAName == model.name) RelationSide.A else RelationSide.B

          clientDbQueries.existsDuplicateByRelationAndSide(previousRelation, relationSideThatCantHaveDuplicates).map {
            case true =>
              Vector(
                DeployError(
                  `type` = model.name,
                  description =
                    s"You are adding a singular backrelation field to a type but there are already pairs in the relation that would violate that constraint."
                ))
            case false => Vector.empty
          }
        case _ => validationSuccessful
      }
    }

    previousSchema.getModelByName(x.model) match {
      case Some(existingModel) =>
        for {
          required     <- newRequiredScalarField(existingModel)
          backRelation <- newToOneBackRelationField(existingModel)
        } yield {
          required ++ backRelation
        }

      case None =>
        validationSuccessful
    }
  }

  private def deleteFieldValidation(x: DeleteField) = {
    val model    = previousSchema.getModelByName_!(x.model)
    val isScalar = model.getFieldByName_!(x.name).isScalar

    if (isScalar) {
      clientDbQueries.existsByModel(model).map {
        case true  => Vector(DeployWarnings.dataLossField(x.name, x.name))
        case false => Vector.empty
      }
    } else {
      validationSuccessful
    }
  }

  private def updateFieldValidation(x: UpdateField) = {
    val model                    = previousSchema.getModelByName_!(x.model)
    val oldField                 = model.getFieldByName_!(x.name)
    val newField                 = nextSchema.getModelByName_!(x.newModel).getFieldByName_!(x.finalName)
    val cardinalityChanges       = oldField.isList != newField.isList
    val typeChanges              = oldField.typeIdentifier != newField.typeIdentifier
    val goesFromScalarToRelation = oldField.isScalar && newField.isRelation
    val goesFromRelationToScalar = oldField.isRelation && newField.isScalar
    val becomesRequired          = !oldField.isRequired && newField.isRequired
    val becomesUnique            = !oldField.isUnique && newField.isUnique

    def warnings: Future[Vector[DeployWarning]] = cardinalityChanges || typeChanges || goesFromRelationToScalar || goesFromScalarToRelation match {
      case true =>
        clientDbQueries.existsByModel(model).map {
          case true  => Vector(DeployWarnings.dataLossField(x.name, x.name))
          case false => Vector.empty
        }
      case false =>
        validationSuccessful
    }

    def requiredErrors: Future[Vector[DeployError]] = {
      if (becomesRequired) {
        clientDbQueries.existsNullByModelAndField(model, oldField).map {
          case true =>
            Vector(
              DeployError(
                `type` = model.name,
                field = oldField.name,
                "You are making a field required, but there are already nodes that would violate that constraint."
              ))
          case false => Vector.empty
        }
      } else if (newField.isRequired && typeChanges) {
        clientDbQueries.existsByModel(model).map {
          case true =>
            Vector(
              DeployError(
                `type` = model.name,
                field = oldField.name,
                "You are changing the type of a required field and there are nodes for that type. Consider making the field optional, then set values for all nodes and then making it required."
              ))
          case false => Vector.empty
        }
      } else {
        validationSuccessful
      }
    }

    def uniqueErrors: Future[Vector[DeployError]] = becomesUnique match {
      case true =>
        clientDbQueries.existsDuplicateValueByModelAndField(model, oldField.asInstanceOf[ScalarField]).map {
          case true =>
            Vector(
              DeployError(`type` = model.name,
                          field = oldField.name,
                          "You are making a field unique, but there are already nodes that would violate that constraint."))
          case false => Vector.empty
        }

      case false =>
        validationSuccessful
    }

    for {
      warnings: Vector[DeployWarning]    <- warnings
      requiredError: Vector[DeployError] <- requiredErrors
      uniqueError: Vector[DeployError]   <- uniqueErrors
    } yield {
      warnings ++ requiredError ++ uniqueError
    }
  }

  private def deleteEnumValidation(x: DeleteEnum) = {
    //already covered by deleteField
    validationSuccessful
  }

  private def updateEnumValidation(x: UpdateEnum) = {
    val oldEnum                       = previousSchema.getEnumByName_!(x.name)
    val newEnum                       = nextSchema.getEnumByName_!(x.finalName)
    val deletedValues: Vector[String] = oldEnum.values.filter(value => !newEnum.values.contains(value))

    if (deletedValues.nonEmpty) {
      val modelsWithFieldsThatUseEnum = previousSchema.models.filter(m => m.fields.exists(f => f.enum.isDefined && f.enum.get.name == x.name)).toVector
      val res = deletedValues.map { deletedValue =>
        clientDbQueries.enumValueIsInUse(modelsWithFieldsThatUseEnum, x.name, deletedValue).map {
          case true  => Vector(DeployError.global(s"You are deleting the value '$deletedValue' of the enum '${x.name}', but that value is in use."))
          case false => Vector.empty
        }
      }
      Future.sequence(res).map(_.flatten)

    } else {
      validationSuccessful
    }
  }

  private def createRelationValidation(x: CreateRelation) = {

    val nextRelation = nextSchema.getRelationByName_!(x.name)

    def checkRelationSide(modelName: String) = {
      val nextModelA      = nextSchema.getModelByName_!(modelName)
      val nextModelAField = nextModelA.relationFields.find(field => field.relation == nextRelation)

      val modelARequired = nextModelAField match {
        case None        => false
        case Some(field) => field.isRequired
      }

      if (modelARequired) previousSchema.getModelByName(modelName) match {
        case Some(model) =>
          clientDbQueries.existsByModel(model).map {
            case true =>
              Vector(DeployError(`type` = model.name, s"You are creating a required relation, but there are already nodes that would violate that constraint."))
            case false => Vector.empty
          }

        case None => validationSuccessful
      } else {
        validationSuccessful
      }
    }

    val checks = Vector(checkRelationSide(nextRelation.modelAName), checkRelationSide(nextRelation.modelBName))

    Future.sequence(checks).map(_.flatten)
  }

  private def deleteRelationValidation(x: DeleteRelation) = {
    val previousRelation = previousSchema.getRelationByName_!(x.name)

    clientDbQueries.existsByRelation(previousRelation).map {
      case true  => Vector(DeployWarnings.dataLossRelation(x.name))
      case false => Vector.empty
    }
  }

  private def updateRelationValidation(x: UpdateRelation) = {
    // becomes required is handled by the change on the updateField
    // todo cardinality change
    val previousRelation = previousSchema.getRelationByName_!(x.name)
    val nextRelation     = nextSchema.getRelationByName_!(x.finalName)
    if (previousRelation.isRelationTable && nextRelation.isRelationTable) {
      (previousRelation.idColumn, nextRelation.idColumn) match {
        case (None, Some(idColumn)) =>
          val error = DeployError(previousRelation.name, "Adding an id field to an existing link table is forbidden.", Some(idColumn))
          Future.successful(Vector(error))
        case _ =>
          validationSuccessful
      }
    } else {
      validationSuccessful
    }
  }

  private def validationSuccessful = Future.successful(Vector.empty)
}
