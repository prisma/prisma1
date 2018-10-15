package com.prisma.deploy.schema.mutations

import com.prisma.deploy.DeployDependencies

import scala.concurrent.{ExecutionContext, Future}

case class SetCloudSecretMutation(
    args: SetCloudSecretMutationInput
)(
    implicit ec: ExecutionContext,
    dependencies: DeployDependencies
) extends Mutation[SetCloudSecretMutationPayload] {
  override def execute: Future[MutationResult[SetCloudSecretMutationPayload]] = {
    dependencies.deployConnector.cloudSecretPersistence.update(args.cloudSecret).flatMap { _ =>
      if (args.cloudSecret.isEmpty) {
        println("No Prisma Cloud secret is set. Metrics collection is disabled.")
      } else {
        println("New Prisma Cloud secret has been set. Metrics collection is enabled.")
      }
      Future.successful(MutationSuccess(SetCloudSecretMutationPayload(args.clientMutationId)))
    }
  }
}

case class SetCloudSecretMutationPayload(
    clientMutationId: Option[String]
) extends sangria.relay.Mutation

case class SetCloudSecretMutationInput(
    clientMutationId: Option[String],
    cloudSecret: Option[String]
) extends sangria.relay.Mutation
