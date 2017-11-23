package cool.graph.deploy.schema.mutations

import cool.graph.shared.models.Region.Value
import cool.graph.shared.models._

import scala.concurrent.Future

class AddProjectMutation extends Mutation[AddProjectMutationPayload] {
  override def execute: Future[MutationResult[AddProjectMutationPayload]] = {
    ???
  }
}

case class AddProjectMutationPayload(
    clientMutationId: Option[String],
    client: Client,
    project: Project
) extends sangria.relay.Mutation

case class AddProjectInput(
    clientMutationId: Option[String],
    name: String,
    alias: Option[String]
) extends sangria.relay.Mutation
