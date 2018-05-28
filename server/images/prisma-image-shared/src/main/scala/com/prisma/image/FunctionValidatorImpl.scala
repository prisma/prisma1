package com.prisma.image

import com.prisma.api.ApiDependencies
import com.prisma.deploy.migration.validation.DeployError
import com.prisma.deploy.schema.mutations.{FunctionInput, FunctionValidator}
import com.prisma.shared.models.{Model, Project, ServerSideSubscriptionFunction, WebhookDelivery}
import com.prisma.subscriptions.schema.{SubscriptionQueryError, SubscriptionQueryValidator}
import com.prisma.utils.or.OrExtensions
import org.scalactic.{Bad, Good, Or}

import scala.concurrent.ExecutionContext

case class FunctionValidatorImpl()(implicit ec: ExecutionContext, dependencies: ApiDependencies) extends FunctionValidator {

  override def validateFunctionInputs(
      project: Project,
      functionInputs: Vector[FunctionInput]
  ): Vector[ServerSideSubscriptionFunction] Or Vector[DeployError] = {
    val result = functionInputs.map(input => validateFunctionInput(project, input))
    OrExtensions.sequence(result)
  }

  private def validateFunctionInput(project: Project, fn: FunctionInput): ServerSideSubscriptionFunction Or Vector[DeployError] = {
    val validator                                      = SubscriptionQueryValidator(project)
    val result: Or[Model, Seq[SubscriptionQueryError]] = validator.validate(fn.query)
    result match {
      case Bad(errors) => Bad(errors.toVector.map(error => DeployError(`type` = "Subscription", field = fn.name, description = error.errorMessage)))
      case Good(_)     => Good(convertFunctionInput(fn))
    }
  }

  private def convertFunctionInput(fnInput: FunctionInput): ServerSideSubscriptionFunction = {
    ServerSideSubscriptionFunction(
      name = fnInput.name,
      isActive = true,
      delivery = WebhookDelivery(
        url = fnInput.url,
        headers = fnInput.headers.map(header => header.name -> header.value)
      ),
      query = fnInput.query
    )
  }
}
