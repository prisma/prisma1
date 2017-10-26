package cool.graph.client.schema.simple

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph._
import cool.graph.authProviders.AuthProviderManager
import cool.graph.client._
import cool.graph.client.database.DeferredTypes.{CountManyModelDeferred, ManyModelDeferred, SimpleConnectionOutputType}
import cool.graph.client.database._
import cool.graph.client.schema.{OutputMapper, SchemaBuilder}
import cool.graph.shared.models
import sangria.schema._
import scaldi._

class SimpleSchemaBuilder(project: models.Project)(implicit inj: Injector, actorSystem: ActorSystem, materializer: ActorMaterializer)
    extends SchemaBuilder(project)(inj, actorSystem, materializer) {

  type ManyDataItemType = SimpleConnectionOutputType

  override val includeSubscription     = true
  override val modelObjectTypesBuilder = new SimpleSchemaModelObjectTypeBuilder(project, Some(nodeInterface))
  override val modelObjectTypes        = modelObjectTypesBuilder.modelObjectTypes

  override val argumentSchema             = SimpleArgumentSchema
  override val outputMapper: OutputMapper = SimpleOutputMapper(project, modelObjectTypes)
  override val deferredResolverProvider: DeferredResolverProvider[_, UserContext] =
    new DeferredResolverProvider(new SimpleToManyDeferredResolver, new SimpleManyModelDeferredResolver)

  override def getConnectionArguments(model: models.Model): List[Argument[Option[Any]]] =
    modelObjectTypesBuilder.mapToListConnectionArguments(model)

  override def resolveGetAllItemsQuery(model: models.Model, ctx: Context[UserContext, Unit]): sangria.schema.Action[UserContext, SimpleConnectionOutputType] = {
    val arguments = modelObjectTypesBuilder.extractQueryArgumentsFromContext(model, ctx)

    ManyModelDeferred[SimpleConnectionOutputType](model, arguments)
  }

  override def createManyFieldTypeForModel(model: models.Model) =
    ListType(modelObjectTypes(model.name))

  override def getIntegrationFields: List[Field[UserContext, Unit]] = {
    includedModels.find(_.name == "User") match {
      case Some(userModel) =>
        AuthProviderManager.simpleMutationFields(project,
                                                 userModel,
                                                 modelObjectTypes("User"),
                                                 modelObjectTypesBuilder,
                                                 argumentSchema,
                                                 deferredResolverProvider)
      case None => List()
    }
  }

  override def getAllItemsMetaField(model: models.Model): Option[Field[UserContext, Unit]] = {
    Some(
      Field(
        s"_all${pluralsCache.pluralName(model)}Meta",
        fieldType = modelObjectTypesBuilder.metaObjectType,
        arguments = getConnectionArguments(model),
        resolve = (ctx) => {
          val queryArguments =
            modelObjectTypesBuilder.extractQueryArgumentsFromContext(model, ctx)

          val countArgs = queryArguments.map(args => SangriaQueryArguments.createSimpleQueryArguments(None, None, None, None, None, args.filter, None))

          val countDeferred = CountManyModelDeferred(model, countArgs)

          DataItem(id = "meta", userData = Map[String, Option[Any]]("count" -> Some(countDeferred)))
        }
      ))
  }
}
