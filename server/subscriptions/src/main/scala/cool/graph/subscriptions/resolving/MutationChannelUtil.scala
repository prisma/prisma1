package cool.graph.subscriptions.resolving

import cool.graph.shared.models.ModelMutationType.ModelMutationType
import cool.graph.shared.models.{Model, ModelMutationType}

trait MutationChannelUtil {
  protected def mutationChannelsForModel(projectId: String, model: Model): Vector[String] = {
    Vector(createChannelName(model), updateChannelName(model), deleteChannelName(model)).map { mutationChannelName =>
      s"subscription:event:$projectId:$mutationChannelName"
    }
  }

  protected def extractMutationTypeFromChannel(channel: String, model: Model): ModelMutationType = {
    val elements = channel.split(':')
    require(elements.length == 4, "A channel name must consist of exactly 4 parts separated by colons")
    val createChannelName = this.createChannelName(model)
    val updateChannelName = this.updateChannelName(model)
    val deleteChannelName = this.deleteChannelName(model)
    elements.last match {
      case `createChannelName` => ModelMutationType.Created
      case `updateChannelName` => ModelMutationType.Updated
      case `deleteChannelName` => ModelMutationType.Deleted
    }
  }

  private def createChannelName(model: Model) = "create" + model.name
  private def updateChannelName(model: Model) = "update" + model.name
  private def deleteChannelName(model: Model) = "delete" + model.name
}
