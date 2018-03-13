package com.prisma.deploy.validation

import com.prisma.deploy.connector.DeployConnector
import com.prisma.deploy.migration.validation.SchemaWarning
import com.prisma.shared.errors.{SchemaCheckResult, SchemaError}
import com.prisma.shared.models._

import scala.concurrent.Future

case class DestructiveChanges(persistencePlugin: DeployConnector, project: Project, nextSchema: Schema, steps: Vector[MigrationStep]) {
  val clientDataResolver = persistencePlugin.clientDBQueries(project)
  val previousSchema     = project.schema

  def checkAgainstExistingData: Future[Vector[SchemaCheckResult]] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val res = steps.map {
      case x: CreateModel =>
        Future.successful(Vector.empty)

      case x: DeleteModel =>
        val model: Model              = project.schema.getModelByName_!(x.name)
        val fieldsWhereThisIsRequired = model.fields.flatMap(field => field.otherRelationField(project.schema))
        val modelsWhereThisIsRequired = fieldsWhereThisIsRequired.flatMap(f => f.model(project.schema))

        val checkRequired: Seq[Future[Vector[SchemaCheckResult]]] = modelsWhereThisIsRequired.map(model =>
          clientDataResolver.existsByModel(model.name).map {
            case true  => Vector(SchemaError.global(""))
            case false => Vector.empty
        })

        val checkData: Future[Vector[SchemaCheckResult]] = clientDataResolver.existsByModel(x.name).map {
          case true  => Vector(SchemaWarning(x.name, ""))
          case false => Vector.empty
        }

        Future.sequence(checkRequired :+ checkData).map(_.flatten)

      case x: UpdateModel =>
        Future.successful(Vector.empty)

      case x: CreateField =>
        val model = project.schema.getModelByName_!(x.model)

        x.isRequired && x.defaultValue.isEmpty match {
          case true =>
            clientDataResolver.existsByModel(model.name).map {
              case true  => Vector(SchemaError.global(""))
              case false => Vector.empty
            }

          case false =>
            Future.successful(Vector.empty)
        }

      case x: DeleteField =>
        val model = project.schema.getModelByName_!(x.model)

        clientDataResolver.existsByModel(model.name).map {
          case true  => Vector(SchemaWarning(x.name, ""))
          case false => Vector.empty
        }

      case x: UpdateField => //todo
        //data loss
        // to relation -> warning
        // to from list -> warning
        // typechange -> warning

        //changing to required and no defValue
        // existing data -> error
        // relations -> maybe error

        //existing unchanged required is also dangerous
        // to/from  relation change -> error

        //cardinality change -> warning

        Future.successful(Vector.empty)

      case x: CreateEnum =>
        Future.successful(Vector.empty)

      case x: DeleteEnum => //todo

        //error if in use

        Future.successful(Vector.empty)

      case x: UpdateEnum => //todo

        //error if deleted case in use

        Future.successful(Vector.empty)

      case x: DeleteRelation =>
        clientDataResolver.existsByRelation(x.name).map {
          case true  => Vector(SchemaWarning(x.name, ""))
          case false => Vector.empty
        }

      case x: CreateRelation => //todo

        //probably caught by the relationfields checks

        Future.successful(Vector.empty)

      case x: UpdateRelation => // todo cardinality change? how is that handled?
        Future.successful(Vector.empty)

      case x: UpdateSecrets =>
        Future.successful(Vector.empty)
    }

    Future.sequence(res).map(_.flatten)
  }

}
