package com.prisma.deploy.schema.mutations

import com.prisma.deploy.migration.validation.SchemaError
import com.prisma.shared.models.Project

import scala.concurrent.Future

trait FunctionValidator {

  def validateFunctionInput(project: Project, fn: FunctionInput): Future[Vector[SchemaError]]
}
