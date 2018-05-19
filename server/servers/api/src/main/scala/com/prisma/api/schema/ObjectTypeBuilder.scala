package com.prisma.api.schema

import com.prisma.api.connector.LogicalKeyWords._
import com.prisma.api.connector._
import com.prisma.api.mutations.BatchPayload
import com.prisma.api.resolver.DeferredTypes._
import com.prisma.api.resolver.{IdBasedConnection, IdBasedConnectionDefinition}
import com.prisma.api.schema.CustomScalarTypes.{DateTimeType, JsonType}
import com.prisma.gc_values._
import com.prisma.shared.models
import com.prisma.shared.models.{Field, Model, TypeIdentifier}
import com.prisma.util.coolArgs.GCAnyConverter
import sangria.schema.{Field => SangriaField, _}

import scala.concurrent.ExecutionContext

class ObjectTypeBuilder(
    project: models.Project,
    nodeInterface: Option[InterfaceType[ApiUserContext, PrismaNode]] = None,
    withRelations: Boolean = true,
    onlyId: Boolean = false
)(implicit ec: ExecutionContext) {
  val batchPayloadType: ObjectType[ApiUserContext, BatchPayload] = ObjectType(
    name = "BatchPayload",
    description = "",
    fieldsFn = () => {
      List(
        SangriaField(
          "count",
          fieldType = LongType,
          description = Some("The number of nodes that have been affected by the Batch operation."),
          resolve = (ctx: Context[ApiUserContext, BatchPayload]) => { ctx.value.count }
        )
      )
    }
  )

  val modelObjectTypes: Map[String, ObjectType[ApiUserContext, PrismaNode]] = project.models.map(model => (model.name, modelToObjectType(model))).toMap

  val modelConnectionTypes: Map[String, ObjectType[ApiUserContext, IdBasedConnection[PrismaNode]]] =
    project.models.map(model => (model.name, modelToConnectionType(model).connectionType)).toMap

  def modelToConnectionType(model: Model): IdBasedConnectionDefinition[ApiUserContext, IdBasedConnection[PrismaNode], PrismaNode] = {
    IdBasedConnection.definition[ApiUserContext, IdBasedConnection, PrismaNode](
      name = model.name,
      nodeType = modelObjectTypes(model.name),
      connectionFields = {

        List(
          SangriaField(
            "aggregate",
            aggregateTypeForModel(model),
            resolve = (ctx: Context[ApiUserContext, IdBasedConnection[PrismaNode]]) => ctx.value.parent.args.getOrElse(QueryArguments.empty)
          )
        )
      }
    )
  }

  def aggregateTypeForModel(model: models.Model): ObjectType[ApiUserContext, QueryArguments] = {
    ObjectType(
      name = s"Aggregate${model.name}",
      fields = List(
        SangriaField(
          "count",
          IntType,
          resolve = (ctx: Context[ApiUserContext, QueryArguments]) => CountManyModelDeferred(model, Some(ctx.value))
        )
      )
    )
  }

  protected def modelToObjectType(model: models.Model): ObjectType[ApiUserContext, PrismaNode] = {
    new ObjectType(
      name = model.name,
      description = None,
      fieldsFn = () => {
        model.fields
          .filter(_.isVisible)
          .filter(field =>
            field.isScalar match {
              case true  => true
              case false => withRelations
          })
          .map(mapClientField(model))
      },
      interfaces = {
        if (model.hasVisibleIdField) {
          nodeInterface.toList
        } else {
          List.empty
        }
      },
      instanceCheck = (value: Any, valClass: Class[_], tpe: ObjectType[ApiUserContext, _]) =>
        value match {
          case PrismaNode(_, _, Some(tpe.name)) => true
          case PrismaNode(_, _, Some(_))        => false
          case _                                => valClass.isAssignableFrom(value.getClass)
      },
      astDirectives = Vector.empty
    )
  }

  def mapClientField(model: models.Model)(field: models.Field): SangriaField[ApiUserContext, PrismaNode] = SangriaField(
    field.name,
    fieldType = mapToOutputType(Some(model), field),
    description = field.description,
    arguments = mapToListConnectionArguments(model, field),
    resolve = (ctx: Context[ApiUserContext, PrismaNode]) => mapToOutputResolve(model, field)(ctx),
    tags = List()
  )

  def mapToOutputType(model: Option[models.Model], field: models.Field): OutputType[Any] = {
    var outputType: OutputType[Any] = field.typeIdentifier match {
      case TypeIdentifier.String    => StringType
      case TypeIdentifier.Int       => IntType
      case TypeIdentifier.Float     => FloatType
      case TypeIdentifier.Boolean   => BooleanType
      case TypeIdentifier.GraphQLID => IDType
      case TypeIdentifier.DateTime  => DateTimeType
      case TypeIdentifier.Json      => JsonType
      case TypeIdentifier.Enum      => SchemaBuilderUtils.mapEnumFieldToInputType(field)
      case TypeIdentifier.Relation  => resolveConnection(field)
    }

    if (field.isScalar && field.isList) outputType = ListType(outputType)

    if (!field.isRequired) outputType = OptionType(outputType)

    outputType
  }

  def resolveConnection(field: Field): OutputType[Any] = {
    field.isList match {
      case true  => ListType(modelObjectTypes(field.relatedModel(project.schema).get.name))
      case false => modelObjectTypes(field.relatedModel_!(project.schema).name)
    }
  }

  def mapToListConnectionArguments(model: models.Model, field: models.Field): List[Argument[Option[Any]]] = {
    (field.isHidden, field.isScalar, field.isList) match {
      case (true, _, _)      => List()
      case (_, true, _)      => List()
      case (_, false, true)  => mapToListConnectionArguments(field.relatedModel(project.schema).get)
      case (_, false, false) => mapToSingleConnectionArguments(field.relatedModel(project.schema).get)
    }
  }

  def mapToListConnectionArguments(model: Model): List[Argument[Option[Any]]] = {
    import SangriaQueryArguments._
    val skipArgument = Argument("skip", OptionInputType(IntType))

    List(
      whereArgument(model, project),
      orderByArgument(model).asInstanceOf[Argument[Option[Any]]],
      skipArgument.asInstanceOf[Argument[Option[Any]]],
      IdBasedConnection.Args.After.asInstanceOf[Argument[Option[Any]]],
      IdBasedConnection.Args.Before.asInstanceOf[Argument[Option[Any]]],
      IdBasedConnection.Args.First.asInstanceOf[Argument[Option[Any]]],
      IdBasedConnection.Args.Last.asInstanceOf[Argument[Option[Any]]]
    )
  }

  def mapToUniqueArguments(model: models.Model): List[Argument[_]] = {
    import com.prisma.util.coolSangria.FromInputImplicit.DefaultScalaResultMarshaller

    model.scalarNonListFields
      .filter(_.isUnique)
      .map(field => Argument(field.name, SchemaBuilderUtils.mapToOptionalInputType(field), description = field.description.getOrElse("")))
  }

  def mapToSingleConnectionArguments(model: Model): List[Argument[Option[Any]]] = {
    import SangriaQueryArguments._

    List(whereArgument(model, project))
  }

  def generateFilterElement(input: Map[String, Any], model: Model, isSubscriptionFilter: Boolean = false): Filter = {
    val filterArguments = new FilterArguments(model, isSubscriptionFilter)

    val filters = input.map {
      case (key, value) =>
        val FieldFilterTuple(field, filter)                    = filterArguments.lookup(key)
        def isScalarNonListFilter(filterName: String): Boolean = field.isDefined && field.get.isScalar && !field.get.isList && filter.name == filterName
        def isScalarListFilter(filterName: String): Boolean    = field.isDefined && field.get.isScalar && field.get.isList && filter.name == filterName
        def isRelationFilter(filterName: String): Boolean      = field.isDefined && field.get.isRelation && filter.name == filterName
        def isOneRelationFilter(filterName: String): Boolean   = isRelationFilter(filterName) && !field.get.isList
        def isManyRelationFilter(filterName: String): Boolean  = isRelationFilter(filterName) && field.get.isList
        def isCollectionOfFilters(value: Seq[Any], filterName: String): Boolean =
          value.nonEmpty && value.head.isInstanceOf[Map[_, _]] && filter.name == filterName
        def getGCValue(value: Any): GCValue                                    = GCAnyConverter(field.get.typeIdentifier, isList = false).toGCValue(unwrapSome(value)).get
        def scalarFilter(condition: ScalarCondition): ScalarFilter             = ScalarFilter(field.get, condition)
        def scalarListFilter(condition: ScalarListCondition): ScalarListFilter = ScalarListFilter(key, field.get, condition)
        def generateSubFilters(value: Seq[Any]) =
          value.asInstanceOf[Seq[Map[String, Any]]].map(x => generateFilterElement(x, model, isSubscriptionFilter)).toVector

        def relationFilter(value: Map[_, _], condition: RelationCondition): RelationFilter =
          RelationFilter(
            project.schema,
            field.get,
            model,
            field.get.relatedModel(project.schema).get,
            field.get.relation.get,
            generateFilterElement(value.asInstanceOf[Map[String, Any]], field.get.relatedModel(project.schema).get, isSubscriptionFilter),
            condition
          )

        value match {
          //-------------------------RECURSION-----------------------------
          case value: Map[_, _] if isLogicFilter(key) || (isSubscriptionFilter && key == "node") =>
            generateFilterElement(value.asInstanceOf[Map[String, Any]], model, isSubscriptionFilter)
          case value: Map[_, _] if isManyRelationFilter("_every")      => relationFilter(value, EveryRelatedNode)
          case value: Map[_, _] if isManyRelationFilter("_some")       => relationFilter(value, AtLeastOneRelatedNode)
          case value: Map[_, _] if isManyRelationFilter("_none")       => relationFilter(value, NoRelatedNode)
          case value: Map[_, _] if isRelationFilter("")                => relationFilter(value, NoRelationCondition)
          case value: Seq[Any] if isCollectionOfFilters(value, "AND")  => AndFilter(generateSubFilters(value))
          case value: Seq[Any] if isCollectionOfFilters(value, "OR")   => OrFilter(generateSubFilters(value))
          case value: Seq[Any] if isCollectionOfFilters(value, "NOT")  => NotFilter(generateSubFilters(value))
          case value: Seq[Any] if isCollectionOfFilters(value, "node") => NodeFilter(generateSubFilters(value))
          //--------------------------ANCHORS------------------------------
          case values: Seq[Any] if isScalarListFilter("_contains_every")   => scalarListFilter(ListContainsEvery(values.map(getGCValue).toVector))
          case values: Seq[Any] if isScalarListFilter("_contains_some")    => scalarListFilter(ListContainsSome(values.map(getGCValue).toVector))
          case value if isScalarListFilter("_contains")                    => scalarListFilter(ListContains(getGCValue(value)))
          case values: Seq[Any] if isScalarNonListFilter("_in")            => scalarFilter(In(values.map(getGCValue).toVector))
          case null if isScalarNonListFilter("_in")                        => scalarFilter(In(Vector(NullGCValue)))
          case values: Seq[Any] if isScalarNonListFilter("_not_in")        => scalarFilter(NotIn(values.map(getGCValue).toVector))
          case null if isScalarNonListFilter("_not_in")                    => scalarFilter(NotIn(Vector(NullGCValue)))
          case value if isScalarNonListFilter("")                          => scalarFilter(Equals(getGCValue(value)))
          case value if isScalarNonListFilter("_not")                      => scalarFilter(NotEquals(getGCValue(value)))
          case value if isScalarNonListFilter("_contains")                 => scalarFilter(Contains(getGCValue(value)))
          case value if isScalarNonListFilter("_not_contains")             => scalarFilter(NotContains(getGCValue(value)))
          case value if isScalarNonListFilter("_starts_with")              => scalarFilter(StartsWith(getGCValue(value)))
          case value if isScalarNonListFilter("_not_starts_with")          => scalarFilter(NotStartsWith(getGCValue(value)))
          case value if isScalarNonListFilter("_ends_with")                => scalarFilter(EndsWith(getGCValue(value)))
          case value if isScalarNonListFilter("_not_ends_with")            => scalarFilter(NotEndsWith(getGCValue(value)))
          case value if isScalarNonListFilter("_lt")                       => scalarFilter(LessThan(getGCValue(value)))
          case value if isScalarNonListFilter("_lte")                      => scalarFilter(LessThanOrEquals(getGCValue(value)))
          case value if isScalarNonListFilter("_gt")                       => scalarFilter(GreaterThan(getGCValue(value)))
          case value if isScalarNonListFilter("_gte")                      => scalarFilter(GreaterThanOrEquals(getGCValue(value)))
          case _ if isOneRelationFilter("")                                => OneRelationIsNullFilter(project.schema, field.get)
          case value: Boolean if field.isEmpty && filter.name == "boolean" => PreComputedSubscriptionFilter(value)
          case None if field.isDefined                                     => NodeSubscriptionFilter()
          case x                                                           => sys.error("Missing case " + x)
        }
    }
    AndFilter(filters.toVector)
  }

  def unwrapSome(value: Any): Any = value match {
    case Some(x) => x
    case x       => x
  }

  def extractQueryArgumentsFromContext(model: Model, ctx: Context[ApiUserContext, Unit]): Option[QueryArguments] = {
    extractQueryArgumentsFromContext(model, ctx, isSubscriptionFilter = false)
  }

  def extractQueryArgumentsFromContextForSubscription(model: Model, ctx: Context[_, Unit]): Option[QueryArguments] = {
    extractQueryArgumentsFromContext(model, ctx, isSubscriptionFilter = true)
  }

  private def extractQueryArgumentsFromContext(model: Model, ctx: Context[_, Unit], isSubscriptionFilter: Boolean): Option[QueryArguments] = {
    val rawFilterOpt: Option[Map[String, Any]] = ctx.argOpt[Map[String, Any]]("where")
    val filterOpt                              = rawFilterOpt.map(generateFilterElement(_, model, isSubscriptionFilter))
    val skipOpt                                = ctx.argOpt[Int]("skip")
    val orderByOpt                             = ctx.argOpt[OrderBy]("orderBy")
    val afterOpt                               = ctx.argOpt[String](IdBasedConnection.Args.After.name)
    val beforeOpt                              = ctx.argOpt[String](IdBasedConnection.Args.Before.name)
    val firstOpt                               = ctx.argOpt[Int](IdBasedConnection.Args.First.name)
    val lastOpt                                = ctx.argOpt[Int](IdBasedConnection.Args.Last.name)

    Some(SangriaQueryArguments.createSimpleQueryArguments(skipOpt, afterOpt, firstOpt, beforeOpt, lastOpt, filterOpt, orderByOpt))
  }

  def mapToOutputResolve[C <: ApiUserContext](model: models.Model, field: models.Field)(
      ctx: Context[C, PrismaNode]): sangria.schema.Action[ApiUserContext, _] = {

    val item: PrismaNode = unwrapDataItemFromContext(ctx)
    lazy val arguments   = extractQueryArgumentsFromContext(field.relatedModel(project.schema).get, ctx.asInstanceOf[Context[ApiUserContext, Unit]])

    (field.isScalar, field.isList) match {
      case (true, true)   => ScalarListDeferred(model, field, item.id)
      case (true, false)  => GCValueExtractor.fromGCValue(item.data.map(field.name))
      case (false, true)  => DeferredValue(ToManyDeferred(field, item.id, arguments)).map(_.toNodes)
      case (false, false) => ToOneDeferred(field, item.id, arguments)
    }
  }

  def unwrapDataItemFromContext[C <: ApiUserContext](ctx: Context[C, PrismaNode]) = {
    // note: ctx.value is sometimes of type Some[DataItem] at runtime even though the type is DataItem
    //metacounts of relations being required or not is one cause see RequiredRelationMetaQueriesSpec
    // todo: figure out why and fix issue at source
    ctx.value.asInstanceOf[Any] match {
      case Some(x: PrismaNode) => x
      case x: PrismaNode       => x
      case None                => throw new Exception("Resolved DataItem was None. This is unexpected - please investigate why and fix.")
    }
  }
}
