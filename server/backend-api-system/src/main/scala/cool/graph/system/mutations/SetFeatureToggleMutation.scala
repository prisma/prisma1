package cool.graph.system.mutations

import cool.graph.cuid.Cuid
import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.models
import cool.graph.shared.models.FeatureToggle
import cool.graph.system.mutactions.internal.{InvalidateSchema, SetFeatureToggle}
import cool.graph.{InternalProjectMutation, Mutaction}
import sangria.relay.Mutation
import scaldi.Injector

case class SetFeatureToggleMutation(client: models.Client,
                                    project: models.Project,
                                    args: SetFeatureToggleInput,
                                    projectDbsFn: models.Project => InternalAndProjectDbs)(implicit inj: Injector)
    extends InternalProjectMutation[SetFeatureToggleMutationPayload] {

  val featureToggle = FeatureToggle(
    id = Cuid.createCuid(),
    name = args.name,
    isEnabled = args.isEnabled
  )

  override def prepareActions(): List[Mutaction] = {
    this.actions = List(SetFeatureToggle(project, featureToggle), InvalidateSchema(project))
    this.actions
  }

  override def getReturnValue: Option[SetFeatureToggleMutationPayload] = {
    Some(SetFeatureToggleMutationPayload(args.clientMutationId, project, featureToggle))
  }
}

case class SetFeatureToggleMutationPayload(clientMutationId: Option[String], project: models.Project, featureToggle: models.FeatureToggle) extends Mutation

case class SetFeatureToggleInput(clientMutationId: Option[String], projectId: String, name: String, isEnabled: Boolean)
