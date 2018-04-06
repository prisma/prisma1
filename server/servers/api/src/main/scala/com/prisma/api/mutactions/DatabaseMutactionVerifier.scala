package com.prisma.api.mutactions

import com.prisma.api.connector._
import com.prisma.api.mutactions.validation.InputValueValidation
import com.prisma.api.schema.APIErrors.ClientApiError
import com.prisma.api.schema.GeneralError
import com.prisma.shared.models.Model

trait DatabaseMutactionVerifier {
  def verify(mutactions: Vector[DatabaseMutaction]): Vector[GeneralError]
}

object DatabaseMutactionVerifierImpl extends DatabaseMutactionVerifier {
  override def verify(mutactions: Vector[DatabaseMutaction]): Vector[GeneralError] = {
    mutactions.flatMap {
      case m: CreateDataItem                 => verify(m)
      case m: UpdateDataItem                 => verify(m)
      case m: UpsertDataItem                 => verify(m)
      case m: UpsertDataItemIfInRelationWith => verify(m)
      case _                                 => None
    }
  }

  def verify(mutaction: CreateDataItem): Option[ClientApiError] = InputValueValidation.validateDataItemInputsGC(mutaction.model, mutaction.args)
  def verify(mutaction: UpdateDataItem): Option[ClientApiError] = InputValueValidation.validateDataItemInputs(mutaction.model, mutaction.args)

  def verify(mutaction: UpsertDataItem): Iterable[ClientApiError] = {
    val model      = mutaction.path.lastModel
    val createArgs = mutaction.allArgs.createArgumentsAsCoolArgs.generateNonListCreateArgs(model, mutaction.createWhere.fieldValueAsString)
    val updateArgs = mutaction.allArgs.updateArgumentsAsCoolArgs.generateNonListUpdateArgs(model)
    verifyUpsert(model, createArgs, updateArgs)
  }

  def verify(mutaction: UpsertDataItemIfInRelationWith): Iterable[ClientApiError] = {
    verifyUpsert(mutaction.path.lastModel, mutaction.createArgs, mutaction.updateArgs)
  }

  def verifyUpsert(model: Model, createArgs: CoolArgs, updateArgs: CoolArgs): Iterable[ClientApiError] = {
    val createCheck = InputValueValidation.validateDataItemInputs(model, createArgs)
    val updateCheck = InputValueValidation.validateDataItemInputs(model, updateArgs)
    createCheck ++ updateCheck
  }
}
