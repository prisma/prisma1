package com.prisma.api.mutactions

import com.prisma.api.connector._
import com.prisma.api.mutactions.validation.InputValueValidation
import com.prisma.api.schema.APIErrors.ClientApiError
import com.prisma.api.schema.UserFacingError
import com.prisma.shared.models.Model

trait DatabaseMutactionVerifier {
  def verify(mutactions: Vector[DatabaseMutaction]): Vector[UserFacingError]
}

object DatabaseMutactionVerifierImpl extends DatabaseMutactionVerifier {
  override def verify(mutactions: Vector[DatabaseMutaction]): Vector[UserFacingError] = {
    mutactions.flatMap {
      case m: TopLevelCreateNode => verify(m)
      case m: TopLevelUpdateNode => verify(m)
      case m: TopLevelUpsertNode => verify(m)
      case m: NestedUpsertNode   => verify(m)
      case _                     => None
    }
  }

  def verify(mutaction: TopLevelCreateNode): Option[ClientApiError] = InputValueValidation.validateDataItemInputs(mutaction.model, mutaction.nonListArgs)
  def verify(mutaction: TopLevelUpdateNode): Option[ClientApiError] = InputValueValidation.validateDataItemInputs(mutaction.where.model, mutaction.nonListArgs)

  def verify(mutaction: TopLevelUpsertNode): Iterable[ClientApiError] = {
    val model      = mutaction.where.model
    val createArgs = mutaction.create.nonListArgs
    val updateArgs = mutaction.update.nonListArgs
    verifyUpsert(model, createArgs, updateArgs)
  }

  def verify(mutaction: NestedUpsertNode): Iterable[ClientApiError] = {
    verifyUpsert(mutaction.relationField.relatedModel_!, mutaction.create.nonListArgs, mutaction.update.nonListArgs)
  }

  def verifyUpsert(model: Model, createArgs: PrismaArgs, updateArgs: PrismaArgs): Iterable[ClientApiError] = {
    val createCheck = InputValueValidation.validateDataItemInputs(model, createArgs)
    val updateCheck = InputValueValidation.validateDataItemInputs(model, updateArgs)
    createCheck ++ updateCheck
  }
}
