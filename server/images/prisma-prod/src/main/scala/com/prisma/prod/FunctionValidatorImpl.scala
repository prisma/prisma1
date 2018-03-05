package com.prisma.prod

import com.prisma.deploy.migration.validation.SchemaError
import com.prisma.deploy.schema.mutations.{FunctionInput, FunctionValidator}
import com.prisma.shared.models.{Model, Project}
import com.prisma.subscriptions.schema.{SubscriptionQueryError, SubscriptionQueryValidator}
import org.scalactic.{Bad, Good, Or}

import scala.concurrent.{ExecutionContext, Future}

case class FunctionValidatorImpl()(implicit ec: ExecutionContext, dependencies: PrismaProdDependencies) extends FunctionValidator {

  override def validateFunctionInput(project: Project, fn: FunctionInput): Future[Vector[SchemaError]] = {
    val validator                                      = SubscriptionQueryValidator(project)
    val result: Or[Model, Seq[SubscriptionQueryError]] = validator.validate(fn.query)
    result match {
      case Bad(errors) =>
        Future.successful(errors.map(error => SchemaError(`type` = "Subscription", field = fn.name, description = error.errorMessage)).toVector)
      case Good(_) => Future.successful(Vector.empty)
    }
  }
}
