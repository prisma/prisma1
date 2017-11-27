package cool.graph.api.schema

//import cool.graph.DataItem
//import cool.graph.client.database.DeferredTypes.{CountToManyDeferred, SimpleConnectionOutputType}
//import cool.graph.client.database.QueryArguments
//import cool.graph.client.schema.SchemaModelObjectTypesBuilder
//import cool.graph.client.{SangriaQueryArguments, UserContext}
import cool.graph.api.schema.CustomScalarTypes.{DateTimeType, JsonType}
import cool.graph.api.database._
import cool.graph.api.database.DeferredTypes.{CountToManyDeferred, ToManyDeferred, ToOneDeferred}
import cool.graph.api.database.Types.DataItemFilterCollection
import cool.graph.shared.models
import cool.graph.shared.models.{Field, Model, TypeIdentifier}
import org.joda.time.{DateTime, DateTimeZone}
import org.joda.time.format.DateTimeFormat
import sangria.schema.{Field => SangriaField, _}
import scaldi.Injector
import spray.json.DefaultJsonProtocol._
import spray.json.{JsValue, _}

import scala.util.{Failure, Success, Try}

class ObjectTypeBuilder(project: models.Project,
                        nodeInterface: Option[InterfaceType[ApiUserContext, DataItem]] = None,
                        modelPrefix: String = "",
                        withRelations: Boolean = true,
                        onlyId: Boolean = false) {

  val metaObjectType = sangria.schema.ObjectType(
    "_QueryMeta",
    description = "Meta information about the query.",
    fields = sangria.schema.fields[ApiUserContext, DataItem](
      sangria.schema
        .Field(name = "count", fieldType = sangria.schema.IntType, resolve = _.value.get[CountToManyDeferred]("count"))
    )
  )

  val modelObjectTypes: Map[String, ObjectType[ApiUserContext, DataItem]] =
    project.models
      .map(model => (model.name, modelToObjectType(model)))
      .toMap

  protected def modelToObjectType(model: models.Model): ObjectType[ApiUserContext, DataItem] = {

    new ObjectType(
      name = modelPrefix + model.name,
      description = model.description,
      fieldsFn = () => {
        model.fields
          .filter(field => if (onlyId) field.name == "id" else true)
          .filter(field =>
            field.isScalar match {
              case true  => true
              case false => withRelations
          })
          .map(mapClientField(model)) ++
          (withRelations match {
            case true  => model.relationFields.flatMap(mapMetaRelationField(model))
            case false => List()
          })
      },
      interfaces = nodeInterface.toList,
      instanceCheck = (value: Any, valClass: Class[_], tpe: ObjectType[ApiUserContext, _]) =>
        value match {
          case DataItem(_, _, Some(tpe.name)) => true
          case DataItem(_, _, Some(_))        => false
          case _                              => valClass.isAssignableFrom(value.getClass)
      },
      astDirectives = Vector.empty
    )
  }

  def mapCustomMutationField(field: models.Field): SangriaField[ApiUserContext, DataItem] = {

    SangriaField(
      field.name,
      fieldType = mapToOutputType(None, field),
      description = field.description,
      arguments = List(),
      resolve = (ctx: Context[ApiUserContext, DataItem]) => {
        mapToOutputResolve(None, field)(ctx)
      },
      tags = List()
    )
  }

  def mapClientField(model: models.Model)(field: models.Field): SangriaField[ApiUserContext, DataItem] = SangriaField(
    field.name,
    fieldType = mapToOutputType(Some(model), field),
    description = field.description,
    arguments = mapToListConnectionArguments(model, field),
    resolve = (ctx: Context[ApiUserContext, DataItem]) => {
      mapToOutputResolve(Some(model), field)(ctx)
    },
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
      case _                        => resolveConnection(field)
    }

    if (field.isScalar && field.isList) {
      outputType = ListType(outputType)
    }

    if (!field.isRequired) {
      outputType = OptionType(outputType)
    }

    outputType
  }

  def resolveConnection(field: Field): OutputType[Any] = {
    field.isList match {
      case true =>
        ListType(modelObjectTypes.get(field.relatedModel(project).get.name).get)
      case false =>
        modelObjectTypes.get(field.relatedModel(project).get.name).get
    }
  }

  def mapMetaRelationField(model: models.Model)(field: models.Field): Option[sangria.schema.Field[ApiUserContext, DataItem]] = {

    (field.relation, field.isList) match {
      case (Some(_), true) =>
        val inputArguments = mapToListConnectionArguments(model, field)

        Some(
          sangria.schema.Field(
            s"_${field.name}Meta",
            fieldType = metaObjectType,
            description = Some("Meta information about the query."),
            arguments = mapToListConnectionArguments(model, field),
            resolve = (ctx: Context[ApiUserContext, DataItem]) => {

              val item: DataItem = unwrapDataItemFromContext(ctx)

              val queryArguments: Option[QueryArguments] =
                extractQueryArgumentsFromContext(field.relatedModel(project).get, ctx.asInstanceOf[Context[ApiUserContext, Unit]])

              val countArgs: Option[QueryArguments] =
                queryArguments.map(args => SangriaQueryArguments.createSimpleQueryArguments(None, None, None, None, None, args.filter, None))

              val countDeferred: CountToManyDeferred = CountToManyDeferred(field, item.id, countArgs)

              DataItem(id = "meta", userData = Map[String, Option[Any]]("count" -> Some(countDeferred)))
            },
            tags = List()
          ))
      case _ => None
    }

  }

  def mapToListConnectionArguments(model: models.Model, field: models.Field): List[Argument[Option[Any]]] = {

    (field.isScalar, field.isList) match {
      case (true, _) => List()
      case (false, true) =>
        mapToListConnectionArguments(field.relatedModel(project).get)
      case (false, false) =>
        mapToSingleConnectionArguments(field.relatedModel(project).get)
    }
  }

  def mapToListConnectionArguments(model: Model): List[Argument[Option[Any]]] = {
    import SangriaQueryArguments._
    val skipArgument = Argument("skip", OptionInputType(IntType))

    List(
      filterArgument(model, project),
      orderByArgument(model).asInstanceOf[Argument[Option[Any]]],
      skipArgument.asInstanceOf[Argument[Option[Any]]],
      IdBasedConnection.Args.After.asInstanceOf[Argument[Option[Any]]],
      IdBasedConnection.Args.Before.asInstanceOf[Argument[Option[Any]]],
      IdBasedConnection.Args.First.asInstanceOf[Argument[Option[Any]]],
      IdBasedConnection.Args.Last.asInstanceOf[Argument[Option[Any]]]
    )
  }

  def mapToSingleConnectionArguments(model: Model): List[Argument[Option[Any]]] = {
    import SangriaQueryArguments._

    List(filterArgument(model, project))
  }

  def generateFilterElement(input: Map[String, Any], model: Model, isSubscriptionFilter: Boolean = false): DataItemFilterCollection = {
    val filterArguments = new FilterArguments(model, isSubscriptionFilter)

    input
      .map({
        case (key, value) =>
          val FieldFilterTuple(field, filter) = filterArguments.lookup(key)
          value match {
            case value: Map[_, _] =>
              val typedValue = value.asInstanceOf[Map[String, Any]]
              if (List("AND", "OR").contains(key) || (isSubscriptionFilter && key == "node")) {
                generateFilterElement(typedValue, model, isSubscriptionFilter)
              } else {
                // this must be a relation filter
                FilterElement(
                  key,
                  null,
                  field,
                  filter.name,
                  Some(
                    FilterElementRelation(
                      fromModel = model,
                      toModel = field.get.relatedModel(project).get,
                      relation = field.get.relation.get,
                      filter = generateFilterElement(typedValue, field.get.relatedModel(project).get, isSubscriptionFilter)
                    ))
                )
              }
            case value: Seq[Any] if value.nonEmpty && value.head.isInstanceOf[Map[_, _]] => {
              FilterElement(key,
                            value
                              .asInstanceOf[Seq[Map[String, Any]]]
                              .map(generateFilterElement(_, model, isSubscriptionFilter)),
                            None,
                            filter.name)
            }
            case value: Seq[Any] => FilterElement(key, value, field, filter.name)
            case _               => FilterElement(key, value, field, filter.name)
          }
      })
      .toList
      .asInstanceOf[DataItemFilterCollection]
  }

  def extractQueryArgumentsFromContext[C <: ApiUserContext](model: Model, ctx: Context[C, Unit]): Option[QueryArguments] = {
    val skipOpt = ctx.argOpt[Int]("skip")

    val rawFilterOpt: Option[Map[String, Any]] = ctx.argOpt[Map[String, Any]]("filter")
    val filterOpt = rawFilterOpt.map(
      generateFilterElement(_,
                            model,
                            //ctx.ctx.isSubscription
                            false))

//    if (filterOpt.isDefined) {
//      ctx.ctx.addFeatureMetric(FeatureMetric.Filter)
//    }

    val orderByOpt = ctx.argOpt[OrderBy]("orderBy")
    val afterOpt   = ctx.argOpt[String](IdBasedConnection.Args.After.name)
    val beforeOpt  = ctx.argOpt[String](IdBasedConnection.Args.Before.name)
    val firstOpt   = ctx.argOpt[Int](IdBasedConnection.Args.First.name)
    val lastOpt    = ctx.argOpt[Int](IdBasedConnection.Args.Last.name)

    Some(
      SangriaQueryArguments
        .createSimpleQueryArguments(skipOpt, afterOpt, firstOpt, beforeOpt, lastOpt, filterOpt, orderByOpt))
  }

  def mapToOutputResolve[C <: ApiUserContext](model: Option[models.Model], field: models.Field)(
      ctx: Context[C, DataItem]): sangria.schema.Action[ApiUserContext, _] = {

    val item: DataItem = unwrapDataItemFromContext(ctx)

    if (!field.isScalar) {
      val arguments = extractQueryArgumentsFromContext(field.relatedModel(project).get, ctx.asInstanceOf[Context[ApiUserContext, Unit]])

      if (field.isList) {
        return ToManyDeferred(
          field,
          item.id,
          arguments
        )
      }
      return ToOneDeferred(field, item.id, arguments)
    }

    // If model is None this is a custom mutation. We currently don't check permissions on custom mutation payloads
    model match {
      case None =>
        val value = ObjectTypeBuilder.convertScalarFieldValueFromDatabase(field, item, resolver = true)
        value

      case Some(model) =>
        // note: UserContext is currently used in many places where we should use the higher level RequestContextTrait
        // until that is cleaned up we have to explicitly check the type here. This is okay as we don't check Permission
        // for ActionUserContext and AlgoliaSyncContext
        // If you need to touch this it's probably better to spend the 5 hours to clean up the Context hierarchy
        val value = ObjectTypeBuilder.convertScalarFieldValueFromDatabase(field, item)

        value
    }
  }

  def unwrapDataItemFromContext[C <: ApiUserContext](ctx: Context[C, DataItem]) = {
    // note: ctx.value is sometimes of type Some[DataItem] at runtime even though the type is DataItem
    //metacounts of relations being required or not is one cause see RequiredRelationMetaQueriesSpec
    // todo: figure out why and fix issue at source
    ctx.value.asInstanceOf[Any] match {
      case Some(x: DataItem) => x
      case x: DataItem       => x
      case None              => throw new Exception("Resolved DataItem was None. This is unexpected - please investigate why and fix.")
    }
  }
}

object ObjectTypeBuilder {

  // todo: this entire thing should rely on GraphcoolDataTypes instead
  def convertScalarFieldValueFromDatabase(field: models.Field, item: DataItem, resolver: Boolean = false): Any = {
    field.name match {
      case "id" if resolver && item.userData.contains("id") => item.userData("id").getOrElse(None)
      case "id"                                             => item.id
      case _ =>
        (item(field.name), field.isList) match {
          case (None, _) =>
            if (field.isRequired) {
              // todo: handle this case
            }
            None
          case (Some(value), true) =>
            def mapTo[T](value: Any, convert: JsValue => T): Seq[T] = {
              value match {
                case x: String =>
                  Try {
                    x.parseJson.asInstanceOf[JsArray].elements.map(convert)
                  } match {
                    case Success(x) => x
                    case Failure(e) => e.printStackTrace(); Vector.empty
                  }

                case x: Vector[_] =>
                  x.map(_.asInstanceOf[T])
              }
            }

            field.typeIdentifier match {
              case TypeIdentifier.String    => mapTo(value, x => x.convertTo[String])
              case TypeIdentifier.Int       => mapTo(value, x => x.convertTo[Int])
              case TypeIdentifier.Float     => mapTo(value, x => x.convertTo[Double])
              case TypeIdentifier.Boolean   => mapTo(value, x => x.convertTo[Boolean])
              case TypeIdentifier.GraphQLID => mapTo(value, x => x.convertTo[String])
              case TypeIdentifier.Password  => mapTo(value, x => x.convertTo[String])
              case TypeIdentifier.DateTime  => mapTo(value, x => new DateTime(x.convertTo[String], DateTimeZone.UTC))
              case TypeIdentifier.Enum      => mapTo(value, x => x.convertTo[String])
              case TypeIdentifier.Json      => mapTo(value, x => x.convertTo[JsValue])
            }
          case (Some(value), false) =>
            def mapTo[T](value: Any) = value.asInstanceOf[T]

            field.typeIdentifier match {
              case TypeIdentifier.String    => mapTo[String](value)
              case TypeIdentifier.Int       => mapTo[Int](value)
              case TypeIdentifier.Float     => mapTo[Double](value)
              case TypeIdentifier.Boolean   => mapTo[Boolean](value)
              case TypeIdentifier.GraphQLID => mapTo[String](value)
              case TypeIdentifier.Password  => mapTo[String](value)
              case TypeIdentifier.DateTime =>
                value.isInstanceOf[DateTime] match {
                  case true => value
                  case false =>
                    value.isInstanceOf[java.sql.Timestamp] match {
                      case true =>
                        DateTime.parse(value.asInstanceOf[java.sql.Timestamp].toString,
                                       DateTimeFormat
                                         .forPattern("yyyy-MM-dd HH:mm:ss.SSS")
                                         .withZoneUTC())
                      case false => new DateTime(value.asInstanceOf[String], DateTimeZone.UTC)
                    }
                }
              case TypeIdentifier.Enum => mapTo[String](value)
              case TypeIdentifier.Json => mapTo[JsValue](value)
            }
        }
    }
  }
}
