package com.prisma.image

import com.prisma.api.ApiDependencies
import com.prisma.deploy.migration.validation.SchemaError
import com.prisma.deploy.schema.mutations.{FunctionInput, FunctionValidator}
import com.prisma.shared.models.{Model, Project}
import com.prisma.subscriptions.schema.{SubscriptionQueryError, SubscriptionQueryValidator}
import org.scalactic.{Bad, Good, Or}

import scala.concurrent.ExecutionContext

case class FunctionValidatorImpl()(implicit ec: ExecutionContext, dependencies: ApiDependencies) extends FunctionValidator {

  override def validateFunctionInput(project: Project, fn: FunctionInput): Vector[SchemaError] = {
    val validator                                      = SubscriptionQueryValidator(project)
    val result: Or[Model, Seq[SubscriptionQueryError]] = validator.validate(fn.query)
    result match {
      case Bad(errors) => errors.map(error => SchemaError(`type` = "Subscription", field = fn.name, description = error.errorMessage)).toVector
      case Good(_)     => Vector.empty
    }
  }
}
