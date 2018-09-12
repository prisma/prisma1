package com.prisma.deploy.schema.mutations

import com.prisma.deploy.migration.validation.DeployError
import com.prisma.shared.models.{Schema, ServerSideSubscriptionFunction, WebhookDelivery}
import org.scalactic.Or

trait FunctionValidator {
  def validateFunctionInputs(schema: Schema, functionInputs: Vector[FunctionInput]): Vector[ServerSideSubscriptionFunction] Or Vector[DeployError]

  protected def convertFunctionInput(fnInput: FunctionInput): ServerSideSubscriptionFunction = {
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
