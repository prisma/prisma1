package com.prisma.api.mutactions

import com.prisma.api.connector._
import com.prisma.api.database.mutactions.validation.InputValueValidation
import com.prisma.api.mutations.CoolArgs
import com.prisma.api.schema.{APIErrors, GeneralError}
import com.prisma.shared.models.Model

import scala.util.{Failure, Success, Try}

trait DatabaseMutactionVerifier {
  def verify(mutactions: Vector[DatabaseMutaction]): Vector[GeneralError]
}

object DatabaseMutactionVerifierImpl extends DatabaseMutactionVerifier {
  private val success = Success(())

  override def verify(mutactions: Vector[DatabaseMutaction]): Vector[GeneralError] = {
    val verifications = mutactions.map {
      case m: CreateDataItem                 => verify(m)
      case m: UpdateDataItem                 => verify(m)
      case m: UpsertDataItem                 => verify(m)
      case m: UpsertDataItemIfInRelationWith => verify(m)
      case _                                 => success
    }
    verifications.collect { case Failure(x: GeneralError) => x }
  }

  def verify(mutaction: CreateDataItem): Try[Unit] = {
    val (check, _) = InputValueValidation.validateDataItemInputs(mutaction.model, mutaction.args)
    check.map(_ => ())
  }

  def verify(mutaction: UpdateDataItem): Try[Unit] = {
    val model                                            = mutaction.model
    lazy val (dataItemInputValidation, fieldsWithValues) = InputValueValidation.validateDataItemInputs(model, mutaction.args)

    lazy val readonlyFields = fieldsWithValues.filter(_.isReadonly)

    if (dataItemInputValidation.isFailure) dataItemInputValidation.map(_ => ())
    else if (readonlyFields.nonEmpty) Failure(APIErrors.ReadonlyField(readonlyFields.mkString(",")))
    else success
  }

  def verify(mutaction: UpsertDataItem): Try[Unit] = {
    val model      = mutaction.path.lastModel
    val createArgs = mutaction.allArgs.createArgumentsAsCoolArgs.generateNonListCreateArgs(model, mutaction.createWhere.fieldValueAsString)
    val updateArgs = mutaction.allArgs.updateArgumentsAsCoolArgs.generateNonListUpdateArgs(model)
    verifyUpsert(model, createArgs, updateArgs)
  }

  def verify(mutaction: UpsertDataItemIfInRelationWith): Try[Unit] = {
    verifyUpsert(mutaction.path.lastModel, mutaction.createArgs, mutaction.updateArgs)
  }

  def verifyUpsert(model: Model, createArgs: CoolArgs, updateArgs: CoolArgs): Try[Unit] = {
    val (createCheck, _) = InputValueValidation.validateDataItemInputs(model, createArgs)
    val (updateCheck, _) = InputValueValidation.validateDataItemInputs(model, updateArgs)

    (createCheck.isFailure, updateCheck.isFailure) match {
      case (true, _)      => createCheck.map(_ => ())
      case (_, true)      => updateCheck.map(_ => ())
      case (false, false) => success
    }
  }
}
