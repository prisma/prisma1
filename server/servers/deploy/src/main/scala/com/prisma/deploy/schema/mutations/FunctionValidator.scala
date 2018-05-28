package com.prisma.deploy.schema.mutations

import com.prisma.deploy.migration.validation.DeployError
import com.prisma.shared.models.{Project, ServerSideSubscriptionFunction}
import org.scalactic.Or

trait FunctionValidator {
  def validateFunctionInputs(project: Project, functionInputs: Vector[FunctionInput]): Vector[ServerSideSubscriptionFunction] Or Vector[DeployError]
}
