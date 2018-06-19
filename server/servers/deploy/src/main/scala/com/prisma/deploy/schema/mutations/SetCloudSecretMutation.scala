package com.prisma.deploy.schema.mutations

import scala.concurrent.Future

case class SetCloudSecretMutation(
    args: SetCloudSecretMutationInput
) extends Mutation[SetCloudSecretMutationPayload] {
  override def execute: Future[MutationResult[SetCloudSecretMutationPayload]] = {
    Future.successful(MutationSuccess(SetCloudSecretMutationPayload(args.clientMutationId)))
  }
}

case class SetCloudSecretMutationPayload(
    clientMutationId: Option[String]
) extends sangria.relay.Mutation

case class SetCloudSecretMutationInput(
    clientMutationId: Option[String],
    cloudSecret: Option[String]
) extends sangria.relay.Mutation
