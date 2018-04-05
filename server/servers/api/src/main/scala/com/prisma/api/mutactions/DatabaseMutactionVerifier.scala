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

  def verify(mutaction: CreateDataItem): Option[ClientApiError] = InputValueValidation.validateDataItemInputsGC(mutaction.model, mutaction.nonListArgs)
  def verify(mutaction: UpdateDataItem): Option[ClientApiError] = InputValueValidation.validateDataItemInputsGC(mutaction.path.lastModel, mutaction.nonListArgs)

  def verify(mutaction: UpsertDataItem): Iterable[ClientApiError] = {
    val model      = mutaction.createPath.lastModel
    val createArgs = mutaction.nonListCreateArgs
    val updateArgs = mutaction.nonListUpdateArgs
    verifyUpsert(model, createArgs, updateArgs)
  }

  def verify(mutaction: UpsertDataItemIfInRelationWith): Iterable[ClientApiError] = {
    verifyUpsert(mutaction.createPath.lastModel, mutaction.createNonListArgs, mutaction.updateNonListArgs)
  }

  def verifyUpsert(model: Model, createArgs: PrismaArgs, updateArgs: PrismaArgs): Iterable[ClientApiError] = {
    val createCheck = InputValueValidation.validateDataItemInputsGC(model, createArgs)
    val updateCheck = InputValueValidation.validateDataItemInputsGC(model, updateArgs)
    createCheck ++ updateCheck
  }
}
