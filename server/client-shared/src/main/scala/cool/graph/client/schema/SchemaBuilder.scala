package cool.graph.client.schema

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph._
import cool.graph.client._
import cool.graph.client.adapters.GraphcoolDataTypes
import cool.graph.client.database.DeferredResolverProvider
import cool.graph.client.mutations._
import cool.graph.client.mutations.definitions._
import cool.graph.client.requestPipeline._
import cool.graph.deprecated.packageMocks.AppliedFunction
import cool.graph.metrics.ClientSharedMetrics
import cool.graph.shared.errors.UserAPIErrors
import cool.graph.shared.errors.UserInputErrors.InvalidSchema
import cool.graph.shared.functions.EndpointResolver
import cool.graph.shared.models.{Field => GCField, _}
import cool.graph.shared.{DefaultApiMatrix, ApiMatrixFactory, models}
import cool.graph.util.coolSangria.FromInputImplicit
import cool.graph.util.performance.TimeHelper
import org.atteo.evo.inflector.English
import sangria.relay._
import sangria.schema.{Field, _}
import scaldi.{Injectable, Injector}
import spray.json.{DefaultJsonProtocol, JsArray, JsBoolean, JsFalse, JsNull, JsNumber, JsObject, JsString, JsTrue, JsValue, JsonFormat}

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

abstract class SchemaBuilder(project: models.Project, modelPrefix: String = "")(implicit inj: Injector,
                                                                                actorSystem: ActorSystem,
                                                                                materializer: ActorMaterializer)
    extends Injectable
    with TimeHelper {

  type ManyDataItemType

  // TODO - Don't use inheritance here. Maybe we can inject the params from the outside?
  val generateGetAll               = true
  val generateGetAllMeta           = true
  val generateGetSingle            = true
  val generateCreate               = true
  val generateUpdate               = true
  val generateUpdateOrCreate       = true
  val generateDelete               = true
  val generateAddToRelation        = true
  val generateRemoveFromRelation   = true
  val generateSetRelation          = true
  val generateUnsetRelation        = true
  val generateIntegrationFields    = true
  val generateCustomMutationFields = true
  val generateCustomQueryFields    = true
  val includeSubscription: Boolean

  val modelObjectTypesBuilder: SchemaModelObjectTypesBuilder[ManyDataItemType]
  val argumentSchema: ArgumentSchema
  val outputMapper: OutputMapper
  val modelObjectTypes: Map[String, ObjectType[UserContext, DataItem]]
  val deferredResolverProvider: DeferredResolverProvider[_, UserContext]

  val apiMatrix: DefaultApiMatrix = inject[ApiMatrixFactory].create(project)
  val includedModels: List[Model] = project.models.filter(model => apiMatrix.includeModel(model.name))

  lazy val inputTypesBuilder = InputTypesBuilder(project, argumentSchema)
  val pluralsCache           = new PluralsCache

  def ifFeatureFlag(predicate: Boolean, fields: => List[Field[UserContext, Unit]], measurementName: String = ""): List[Field[UserContext, Unit]] = {
    if (predicate) {
//      if(measurementName != ""){
//        time(measurementName)(fields)
//      } else {
//        fields
//      }
      fields
    } else {
      List.empty
    }
  }

  def build(): Schema[UserContext, Unit] = ClientSharedMetrics.schemaBuilderBuildTimerMetric.time(project.id) {
    val query    = buildQuery()
    val mutation = buildMutation()

    includeSubscription match {
      case true =>
        val subscription = buildSubscription()
        Schema(
          query = query,
          mutation = mutation,
          subscription = subscription,
          validationRules = SchemaValidationRule.empty
        )
      case false =>
        Schema(
          query = query,
          mutation = mutation,
          validationRules = SchemaValidationRule.empty
        )
    }
  }

  def buildQuery(): ObjectType[UserContext, Unit] = {
    val fields = {
      ifFeatureFlag(generateGetAll, includedModels.map(getAllItemsField)) ++
        ifFeatureFlag(generateGetAllMeta, includedModels.flatMap(getAllItemsMetaField)) ++
        ifFeatureFlag(generateGetSingle, includedModels.map(getSingleItemField)) ++
        ifFeatureFlag(generateCustomQueryFields, project.activeCustomQueryFunctions.map(getCustomResolverField)) ++
        userField.toList :+ nodeField
    }

    ObjectType("Query", fields)
  }

  def buildMutation(): Option[ObjectType[UserContext, Unit]] = {
    val oneRelations                     = apiMatrix.filterRelations(project.getOneRelations)
    val oneRelationsWithoutRequiredField = apiMatrix.filterNonRequiredRelations(oneRelations)

    val manyRelations                     = apiMatrix.filterRelations(project.getManyRelations)
    val manyRelationsWithoutRequiredField = apiMatrix.filterNonRequiredRelations(manyRelations)

    val mutationFields: List[Field[UserContext, Unit]] = {
      ifFeatureFlag(generateCreate, includedModels.filter(_.name != "User").map(getCreateItemField), measurementName = "CREATE") ++
        ifFeatureFlag(generateUpdate, includedModels.map(getUpdateItemField), measurementName = "UPDATE") ++
        ifFeatureFlag(generateUpdateOrCreate, includedModels.map(getUpdateOrCreateItemField), measurementName = "UPDATE_OR_CREATE") ++
        ifFeatureFlag(generateDelete, includedModels.map(getDeleteItemField)) ++
        ifFeatureFlag(generateSetRelation, oneRelations.map(getSetRelationField)) ++
        ifFeatureFlag(generateUnsetRelation, oneRelationsWithoutRequiredField.map(getUnsetRelationField)) ++
        ifFeatureFlag(generateAddToRelation, manyRelations.map(getAddToRelationField)) ++
        ifFeatureFlag(generateRemoveFromRelation, manyRelationsWithoutRequiredField.map(getRemoveFromRelationField)) ++
        ifFeatureFlag(generateIntegrationFields, getIntegrationFields) ++
        ifFeatureFlag(generateCustomMutationFields, project.activeCustomMutationFunctions.map(getCustomResolverField))
    }

    if (mutationFields.isEmpty) None
    else Some(ObjectType("Mutation", mutationFields))
  }

  def buildSubscription(): Option[ObjectType[UserContext, Unit]] = {
    val subscriptionFields = { ifFeatureFlag(generateCreate, includedModels.map(getSubscriptionField)) }

    if (subscriptionFields.isEmpty) None
    else Some(ObjectType("Subscription", subscriptionFields))
  }

  def getAllItemsField(model: models.Model): Field[UserContext, Unit] = {
    Field(
      s"all${pluralsCache.pluralName(model)}",
      fieldType = createManyFieldTypeForModel(model),
      arguments = getConnectionArguments(model),
      resolve = (ctx) => {
        resolveGetAllItemsQuery(model, ctx)
      }
    )
  }

  def getAllItemsMetaField(model: models.Model): Option[Field[UserContext, Unit]] = None

  def getSingleItemField(model: models.Model): Field[UserContext, Unit] = {
    Field(
      model.name,
      fieldType = createSingleFieldTypeForModel(model),
      arguments = extractUniqueArguments(model),
      resolve = (ctx) => {
        resolveGetSingleItemQuery(model, ctx)
      }
    )
  }

  def getCustomResolverField(function: SchemaExtensionFunction): Field[UserContext, Unit] = {

    def getResolve(payloadType: FreeType,
                   raw: Map[String, Any],
                   ctx: UserContext,
                   expPackageMutation: Option[AppliedFunction] = None): Future[FunctionDataItems] = {

      val args             = GraphcoolDataTypes.convertToJson(GraphcoolDataTypes.wrapSomes(raw))
      val endpointResolver = inject[EndpointResolver](identified by "endpointResolver")
      val context          = FunctionExecutor.createEventContext(project, ctx.requestIp, headers = Map.empty, ctx.authenticatedRequest, endpointResolver)

      val argsAndContext = expPackageMutation match {
        case None =>
          Map(
            "data"    -> args,
            "context" -> context
          )
        case Some(exp) =>
          Map(
            "data"    -> args,
            "context" -> (context + ("package" -> exp.context))
          )
      }

      val event = AnyJsonFormat.write(argsAndContext).compactPrint

      val functionExecutor = new FunctionExecutor()

      val functionExecutionResult: Future[FunctionSuccess] = functionExecutor.syncWithLoggingAndErrorHandling_!(function, event, project, ctx.requestId)

      functionExecutionResult.map { res =>
        res.values.isNull match {
          case true =>
            FunctionDataItems(isNull = true, Vector.empty)

          case false =>
            FunctionDataItems(
              isNull = false,
              res.values.values.map(jsObject =>
                DataItem.fromMap(GraphcoolDataTypes.fromJson(data = jsObject, fields = payloadType.fields, addNoneValuesForMissingFields = true)))
            )
        }
      }
    }

    def getQueryArguments(arguments: List[GCField]) = {
      arguments.map(arg => {

        // NOTE needed for Argument types
        import FromInputImplicit.DefaultScalaResultMarshaller

        val inputType: InputType[Any] = (arg.isRequired, arg.isList) match {
          case (_, _) if arg.typeIdentifier == TypeIdentifier.Relation => throw InvalidSchema(s"argument '${arg.name}' is invalid. Must be a scalar type.")
          case (true, false)                                           => TypeIdentifier.toSangriaScalarType(arg.typeIdentifier)
          case (false, false)                                          => OptionInputType(TypeIdentifier.toSangriaScalarType(arg.typeIdentifier))
          case (true, true)                                            => ListInputType(TypeIdentifier.toSangriaScalarType(arg.typeIdentifier))
          case (false, true)                                           => OptionInputType(ListInputType(TypeIdentifier.toSangriaScalarType(arg.typeIdentifier)))
        }

        Argument(arg.name, inputType)
      })
    }

    val field: Field[UserContext, Unit] = function match {
      case customMutation: CustomMutationFunction =>
        val expPackageMutation = project.experimentalAuthProvidersCustomMutations.find(_.name == function.name)
        val payloadType        = customMutation.payloadType

        Field(
          customMutation.mutationName,
          fieldType = payloadType.getFieldType(modelObjectTypesBuilder),
          description = Some(customMutation.name),
          arguments = getQueryArguments(customMutation.arguments),
          resolve = (ctx) => getResolve(payloadType, ctx.args.raw, ctx.ctx, expPackageMutation).map((x: FunctionDataItems) => payloadType.adjustResolveType(x))
        )
      case customQuery: CustomQueryFunction =>
        val payloadType = customQuery.payloadType

        Field(
          customQuery.queryName,
          fieldType = payloadType.getFieldType(modelObjectTypesBuilder),
          description = Some(customQuery.name),
          arguments = getQueryArguments(customQuery.arguments),
          resolve = (ctx) => getResolve(payloadType, ctx.args.raw, ctx.ctx).map((x: FunctionDataItems) => payloadType.adjustResolveType(x))
        )
    }
    field
  }

  lazy val NodeDefinition(nodeInterface, nodeField, nodeRes) = Node.definitionById(
    resolve = (id: String, ctx: Context[UserContext, Unit]) => {
      ctx.ctx.dataResolver.resolveByGlobalId(id)
    },
    possibleTypes = {
      modelObjectTypes.values.map(o => PossibleNodeObject(o)).toList
    }
  )

  def getConnectionArguments(model: models.Model): List[Argument[Option[Any]]]

  def resolveGetAllItemsQuery(model: models.Model, ctx: Context[UserContext, Unit]): sangria.schema.Action[UserContext, ManyDataItemType]

  def createManyFieldTypeForModel(model: models.Model): OutputType[ManyDataItemType]

  def userField: Option[Field[UserContext, Unit]] = {
    includedModels
      .find(_.name == "User")
      .map(userModel => {
        Field(
          "user",
          fieldType = OptionType(modelObjectTypesBuilder.modelObjectTypes(userModel.name)),
          arguments = List(),
          resolve = (ctx) => {
            ctx.ctx.userId
              .map(userId => ctx.ctx.dataResolver.resolveByUnique(userModel, "id", userId))
              .getOrElse(Future.successful(None))
          }
        )
      })
  }

  def resolveGetSingleItemQuery(model: models.Model, ctx: Context[UserContext, Unit]): sangria.schema.Action[UserContext, Option[DataItem]] = {
    val arguments = extractUniqueArguments(model)
    val arg = arguments.find(a => ctx.args.argOpt(a.name).isDefined) match {
      case Some(value) => value
      case None =>
        throw UserAPIErrors.GraphQLArgumentsException(s"None of the following arguments provided: ${arguments.map(_.name)}")
    }

    ctx.ctx.dataResolver
      .batchResolveByUnique(model, arg.name, List(ctx.arg(arg).asInstanceOf[Option[_]].get))
      .map(_.headOption)
    // todo: Make OneDeferredResolver.dataItemsToToOneDeferredResultType work with Timestamps
//    OneDeferred(model, arg.name, ctx.arg(arg).asInstanceOf[Option[_]].get)
  }

  def createSingleFieldTypeForModel(model: models.Model) =
    OptionType(modelObjectTypes(model.name))

  def extractUniqueArguments(model: models.Model): List[Argument[_]] = {

    import FromInputImplicit.DefaultScalaResultMarshaller

    apiMatrix
      .filterFields(model.fields)
      .filter(!_.isList)
      .filter(_.isUnique)
      .map(field => Argument(field.name, SchemaBuilderUtils.mapToOptionalInputType(field), description = field.description.getOrElse("")))
  }

  def getCreateItemField(model: models.Model): Field[UserContext, Unit] = {

    val definition = CreateDefinition(argumentSchema, project, inputTypesBuilder)
    val arguments  = definition.getSangriaArguments(model = model)

    Field(
      s"create${model.name}",
      fieldType = OptionType(outputMapper.mapCreateOutputType(model, modelObjectTypes(model.name))),
      arguments = arguments,
      resolve = (ctx) => {
        ctx.ctx.mutationQueryWhitelist.registerWhitelist(s"create${model.name}", outputMapper.nodePaths(model), argumentSchema.inputWrapper, ctx)
        val mutation = new Create(model = model, project = project, args = ctx.args, dataResolver = ctx.ctx.dataResolver, argumentSchema = argumentSchema)
        mutation
          .run(ctx.ctx.authenticatedRequest, ctx.ctx)
          .map(outputMapper.mapResolve(_, ctx.args))
      }
    )
  }

  def getSubscriptionField(model: models.Model): Field[UserContext, Unit] = {

    val objectType = modelObjectTypes(model.name)
    Field(
      s"${model.name}",
      fieldType = OptionType(outputMapper.mapSubscriptionOutputType(model, objectType)),
      arguments = List(SangriaQueryArguments.filterSubscriptionArgument(model = model, project = project)),
      resolve = _ => None
    )

  }

  def getSetRelationField(relation: models.Relation): Field[UserContext, Unit] = {

    val fromModel  = project.getModelById_!(relation.modelAId)
    val fromField  = relation.getModelAField_!(project)
    val toModel    = project.getModelById_!(relation.modelBId)
    val definition = AddToRelationDefinition(relation, project, argumentSchema)
    val arguments  = definition.getSangriaArguments(model = fromModel)

    Field(
      name = s"set${relation.name}",
      fieldType =
        OptionType(outputMapper.mapAddToRelationOutputType(relation, fromModel, fromField, toModel, modelObjectTypes(fromModel.name), s"Set${relation.name}")),
      arguments = arguments,
      resolve = (ctx) =>
        new SetRelation(relation = relation,
                        fromModel = fromModel,
                        project = project,
                        args = ctx.args,
                        dataResolver = ctx.ctx.dataResolver,
                        argumentSchema = argumentSchema)
          .run(ctx.ctx.authenticatedRequest, ctx.ctx)
          .map(outputMapper.mapResolve(_, ctx.args))
    )
  }

  def getAddToRelationField(relation: models.Relation): Field[UserContext, Unit] = {

    val fromModel  = project.getModelById_!(relation.modelAId)
    val fromField  = relation.getModelAField_!(project)
    val toModel    = project.getModelById_!(relation.modelBId)
    val definition = AddToRelationDefinition(relation, project, argumentSchema)
    val arguments  = definition.getSangriaArguments(model = fromModel)

    Field(
      name = s"addTo${relation.name}",
      fieldType = OptionType(
        outputMapper.mapAddToRelationOutputType(relation, fromModel, fromField, toModel, modelObjectTypes(fromModel.name), s"AddTo${relation.name}")),
      arguments = arguments,
      resolve = (ctx) =>
        new AddToRelation(relation = relation,
                          fromModel = fromModel,
                          project = project,
                          args = ctx.args,
                          dataResolver = ctx.ctx.dataResolver,
                          argumentSchema = argumentSchema)
          .run(ctx.ctx.authenticatedRequest, ctx.ctx)
          .map(outputMapper.mapResolve(_, ctx.args))
    )
  }

  def getRemoveFromRelationField(relation: models.Relation): Field[UserContext, Unit] = {

    val fromModel = project.getModelById_!(relation.modelAId)
    val fromField = relation.getModelAField_!(project)
    val toModel   = project.getModelById_!(relation.modelBId)

    val arguments = RemoveFromRelationDefinition(relation, project, argumentSchema)
      .getSangriaArguments(model = fromModel)

    Field(
      name = s"removeFrom${relation.name}",
      fieldType = OptionType(
        outputMapper
          .mapRemoveFromRelationOutputType(relation, fromModel, fromField, toModel, modelObjectTypes(fromModel.name), s"RemoveFrom${relation.name}")),
      arguments = arguments,
      resolve = (ctx) =>
        new RemoveFromRelation(relation = relation,
                               fromModel = fromModel,
                               project = project,
                               args = ctx.args,
                               dataResolver = ctx.ctx.dataResolver,
                               argumentSchema = argumentSchema)
          .run(ctx.ctx.authenticatedRequest, ctx.ctx)
          .map(outputMapper.mapResolve(_, ctx.args))
    )
  }

  def getUnsetRelationField(relation: models.Relation): Field[UserContext, Unit] = {

    val fromModel = project.getModelById_!(relation.modelAId)
    val fromField = relation.getModelAField_!(project)
    val toModel   = project.getModelById_!(relation.modelBId)

    val arguments = UnsetRelationDefinition(relation, project, argumentSchema).getSangriaArguments(model = fromModel)

    Field(
      name = s"unset${relation.name}",
      fieldType = OptionType(
        outputMapper
          .mapRemoveFromRelationOutputType(relation, fromModel, fromField, toModel, modelObjectTypes(fromModel.name), s"Unset${relation.name}")),
      arguments = arguments,
      resolve = (ctx) =>
        new UnsetRelation(relation = relation,
                          fromModel = fromModel,
                          project = project,
                          args = ctx.args,
                          dataResolver = ctx.ctx.dataResolver,
                          argumentSchema = argumentSchema)
          .run(ctx.ctx.authenticatedRequest, ctx.ctx)
          .map(outputMapper.mapResolve(_, ctx.args))
    )
  }

  val idArgument = Argument("id", IDType)

  def getUpdateItemField(model: models.Model): Field[UserContext, Unit] = {
    val arguments = UpdateDefinition(argumentSchema, project, inputTypesBuilder).getSangriaArguments(model = model)

    Field(
      s"update${model.name}",
      fieldType = OptionType(
        outputMapper
          .mapUpdateOutputType(model, modelObjectTypes(model.name))),
      arguments = arguments,
      resolve = (ctx) => {
        ctx.ctx.mutationQueryWhitelist
          .registerWhitelist(s"update${model.name}", outputMapper.nodePaths(model), argumentSchema.inputWrapper, ctx)
        new Update(model = model, project = project, args = ctx.args, dataResolver = ctx.ctx.dataResolver, argumentSchema = argumentSchema)
          .run(ctx.ctx.authenticatedRequest, ctx.ctx)
          .map(outputMapper.mapResolve(_, ctx.args))
      }
    )
  }

  def getUpdateOrCreateItemField(model: models.Model): Field[UserContext, Unit] = {
    val arguments = UpdateOrCreateDefinition(argumentSchema, project, inputTypesBuilder).getSangriaArguments(model = model)

    Field(
      s"updateOrCreate${model.name}",
      fieldType = OptionType(outputMapper.mapUpdateOrCreateOutputType(model, modelObjectTypes(model.name))),
      arguments = arguments,
      resolve = (ctx) => {
        ctx.ctx.mutationQueryWhitelist.registerWhitelist(s"updateOrCreate${model.name}", outputMapper.nodePaths(model), argumentSchema.inputWrapper, ctx)
        new UpdateOrCreate(model = model, project = project, args = ctx.args, dataResolver = ctx.ctx.dataResolver, argumentSchema = argumentSchema)
          .run(ctx.ctx.authenticatedRequest, ctx.ctx)
          .map(outputMapper.mapResolve(_, ctx.args))
      }
    )
  }

  def getDeleteItemField(model: models.Model): Field[UserContext, Unit] = {

    val arguments = DeleteDefinition(argumentSchema, project).getSangriaArguments(model = model)

    Field(
      s"delete${model.name}",
      fieldType = OptionType(outputMapper.mapDeleteOutputType(model, modelObjectTypes(model.name))),
      arguments = arguments,
      resolve = (ctx) => {
        ctx.ctx.mutationQueryWhitelist.registerWhitelist(s"delete${model.name}", outputMapper.nodePaths(model), argumentSchema.inputWrapper, ctx)
        new Delete(model = model,
                   modelObjectTypes = modelObjectTypesBuilder,
                   project = project,
                   args = ctx.args,
                   dataResolver = ctx.ctx.dataResolver,
                   argumentSchema = argumentSchema)
          .run(ctx.ctx.authenticatedRequest, ctx.ctx)
          .map(outputMapper.mapResolve(_, ctx.args))
      }
    )
  }

  def getIntegrationFields: List[Field[UserContext, Unit]]

  implicit object AnyJsonFormat extends JsonFormat[Any] {
    def write(x: Any): JsValue = x match {
      case m: Map[_, _] => JsObject(m.asInstanceOf[Map[String, Any]].mapValues(write))
      case l: List[Any] => JsArray(l.map(write).toVector)
      case n: Int       => JsNumber(n)
      case n: Long      => JsNumber(n)
      case s: String    => JsString(s)
      case true         => JsTrue
      case false        => JsFalse
      case v: JsValue   => v
      case null         => JsNull
      case r            => JsString(r.toString)
    }

    def read(x: JsValue): Any = {
      x match {
        case l: JsArray   => l.elements.map(read).toList
        case m: JsObject  => m.fields.mapValues(write)
        case s: JsString  => s.value
        case n: JsNumber  => n.value
        case b: JsBoolean => b.value
        case JsNull       => null
        case _            => sys.error("implement all scalar types!")
      }
    }
  }

  lazy val myMapFormat: JsonFormat[Map[String, Any]] = {
    import DefaultJsonProtocol._
    mapFormat[String, Any]
  }
}

class PluralsCache {
  private val cache = mutable.Map.empty[Model, String]

  def pluralName(model: Model): String = cache.getOrElseUpdate(
    key = model,
    op = English.plural(model.name).capitalize
  )
}
