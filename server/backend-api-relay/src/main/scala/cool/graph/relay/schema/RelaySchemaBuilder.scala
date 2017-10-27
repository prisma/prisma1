package cool.graph.relay.schema

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph._
import cool.graph.authProviders._
import cool.graph.client._
import cool.graph.client.database.DeferredTypes._
import cool.graph.client.database.{DeferredResolverProvider, IdBasedConnection, RelayManyModelDeferredResolver, RelayToManyDeferredResolver}
import cool.graph.client.schema.SchemaBuilder
import cool.graph.client.schema.relay.RelaySchemaModelObjectTypeBuilder
import cool.graph.shared.models
import cool.graph.shared.models.Model
import sangria.schema._
import scaldi._

// Todo: Decide if we really need UserContext instead of SimpleUserContext here.
// Or if we could use UserContext in the superclass.
class RelaySchemaBuilder(project: models.Project, modelPrefix: String = "")(implicit inj: Injector, actorSystem: ActorSystem, materializer: ActorMaterializer)
    extends SchemaBuilder(project, modelPrefix)(inj, actorSystem, materializer) {

  type ManyDataItemType = RelayConnectionOutputType

  lazy val ViewerType: ObjectType[UserContext, Unit] = {
    ObjectType(
      "Viewer",
      "This is the famous Relay viewer object",
      fields[UserContext, Unit](
        includedModels.map(getAllItemsField) ++ userField.toList ++ includedModels
          .map(getSingleItemField) ++ project.activeCustomQueryFunctions
          .map(getCustomResolverField) :+ Field[UserContext, Unit, String, String](name = "id",
                                                                                   fieldType = IDType,
                                                                                   arguments = List(),
                                                                                   resolve = _ => s"viewer-fixed"): _*
      )
    )
  }

  override val includeSubscription     = false
  override val modelObjectTypesBuilder = new RelaySchemaModelObjectTypeBuilder(project, Some(nodeInterface), modelPrefix)
  override val modelObjectTypes        = modelObjectTypesBuilder.modelObjectTypes
  override val argumentSchema          = RelayArgumentSchema
  override val outputMapper            = new RelayOutputMapper(ViewerType, edgeObjectTypes, modelObjectTypes, project)
  override val deferredResolverProvider: DeferredResolverProvider[_, UserContext] =
    new DeferredResolverProvider(new RelayToManyDeferredResolver, new RelayManyModelDeferredResolver)

  lazy val connectionObjectTypes = modelObjectTypesBuilder.modelConnectionTypes
  lazy val edgeObjectTypes       = modelObjectTypesBuilder.modelEdgeTypes

  override def getConnectionArguments(model: Model): List[Argument[Option[Any]]] = {
    modelObjectTypesBuilder.mapToListConnectionArguments(model)
  }

  override def resolveGetAllItemsQuery(model: Model, ctx: Context[UserContext, Unit]): sangria.schema.Action[UserContext, RelayConnectionOutputType] = {
    val arguments = modelObjectTypesBuilder.extractQueryArgumentsFromContext(model, ctx)

    ManyModelDeferred[RelayConnectionOutputType](model, arguments)
  }

  override def createManyFieldTypeForModel(model: Model): OutputType[IdBasedConnection[DataItem]] = {
    connectionObjectTypes(model.name)
  }

  def viewerField: Field[UserContext, Unit] = Field(
    "viewer",
    fieldType = ViewerType,
    resolve = _ => ()
  )

  override def buildQuery(): ObjectType[UserContext, Unit] = {
    ObjectType(
      "Query",
      List(viewerField, nodeField) ++ Nil
    )
  }

  override def getIntegrationFields: List[Field[UserContext, Unit]] = {
    includedModels.find(_.name == "User") match {
      case Some(_) =>
        AuthProviderManager.relayMutationFields(project,
                                                includedModels.find(_.name == "User").get,
                                                ViewerType,
                                                modelObjectTypes("User"),
                                                modelObjectTypesBuilder,
                                                argumentSchema,
                                                deferredResolverProvider)
      case None => List()
    }
  }
}
