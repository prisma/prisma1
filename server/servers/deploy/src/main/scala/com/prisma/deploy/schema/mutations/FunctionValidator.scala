package com.prisma.deploy.schema.mutations

import com.prisma.deploy.migration.validation.DeployError
import com.prisma.shared.models.Project

trait FunctionValidator {

  def validateFunctionInput(project: Project, fn: FunctionInput): Vector[DeployError]
}
