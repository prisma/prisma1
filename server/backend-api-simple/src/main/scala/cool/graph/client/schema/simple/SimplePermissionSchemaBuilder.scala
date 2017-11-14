package cool.graph.client.schema.simple

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.client.ClientInjector
import cool.graph.shared.models

class SimplePermissionSchemaBuilder(project: models.Project)(implicit injector: ClientInjector, actorSystem: ActorSystem, materializer: ActorMaterializer)
    extends SimpleSchemaBuilder(project)(injector, actorSystem, materializer) {

  override val generateCreate             = false
  override val generateUpdate             = false
  override val generateDelete             = false
  override val generateAddToRelation      = false
  override val generateRemoveFromRelation = false
  override val generateSetRelation        = false
  override val generateUnsetRelation      = false
  override val generateIntegrationFields  = false
}
