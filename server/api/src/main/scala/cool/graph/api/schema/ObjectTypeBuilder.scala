package cool.graph.api.schema

import cool.graph.api.schema.CustomScalarTypes.{DateTimeType, JsonType}
import cool.graph.api.database._
import cool.graph.api.database.DeferredTypes.{CountManyModelDeferred, CountToManyDeferred, ToManyDeferred, ToOneDeferred}
import cool.graph.api.database.Types.DataItemFilterCollection
import cool.graph.shared.models
import cool.graph.shared.models.{Field, Model, TypeIdentifier}
import org.joda.time.{DateTime, DateTimeZone}
import org.joda.time.format.DateTimeFormat
import sangria.schema.{Field => SangriaField, _}
import spray.json.DefaultJsonProtocol._
import spray.json.{JsValue, _}

import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

class ObjectTypeBuilder(
    project: models.Project,
    nodeInterface: Option[InterfaceType[ApiUserContext, DataItem]] = None,
    modelPrefix: String = "",
    withRelations: Boolean = true,
    onlyId: Boolean = false
) {
  val modelObjectTypes: Map[String, ObjectType[ApiUserContext, DataItem]] =
    project.models
      .map(model => (model.name, modelToObjectType(model)))
      .toMap

  val modelConnectionTypes = project.models
    .map(model => (model.name, modelToConnectionType(model).connectionType))
    .toMap

  def modelToConnectionType(model: Model): IdBasedConnectionDefinition[ApiUserContext, IdBasedConnection[DataItem], DataItem] = {
    IdBasedConnection.definition[ApiUserContext, IdBasedConnection, DataItem](
      name = modelPrefix + model.name,
      nodeType = modelObjectTypes(model.name),
      connectionFields = List(
        // todo: add aggregate fields

//        sangria.schema.Field(
//          "count",
//          IntType,
//          Some("Count of filtered result set without considering pagination arguments"),
//          resolve = ctx => {
//            val countArgs = ctx.value.parent.args.map(args => SangriaQueryArguments.createSimpleQueryArguments(None, None, None, None, None, args.filter, None))
//
//            ctx.value.parent match {
//              case ConnectionParentElement(Some(nodeId), Some(field), _) =>
//                CountToManyDeferred(field, nodeId, countArgs)
//              case _ =>
//                CountManyModelDeferred(model, countArgs)
//            }
//          }
//        )
      )
    )
  }

  protected def modelToObjectType(model: models.Model): ObjectType[ApiUserContext, DataItem] = {
    new ObjectType(
      name = modelPrefix + model.name,
      description = model.description,
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
          case DataItem(_, _, Some(tpe.name)) => true
          case DataItem(_, _, Some(_))        => false
          case _                              => valClass.isAssignableFrom(value.getClass)
      },
      astDirectives = Vector.empty
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
      case TypeIdentifier.Relation  => resolveConnection(field)
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
      case true  => ListType(modelObjectTypes(field.relatedModel(project).get.name))
      case false => modelObjectTypes(field.relatedModel_!(project).name)
    }
  }

  def mapToListConnectionArguments(model: models.Model, field: models.Field): List[Argument[Option[Any]]] = {
    (field.isHidden, field.isScalar, field.isList) match {
      case (true, _, _)      => List()
      case (_, true, _)      => List()
      case (_, false, true)  => mapToListConnectionArguments(field.relatedModel(project).get)
      case (_, false, false) => mapToSingleConnectionArguments(field.relatedModel(project).get)
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
    import cool.graph.util.coolSangria.FromInputImplicit.DefaultScalaResultMarshaller

    model.fields
      .filter(!_.isList)
      .filter(_.isUnique)
      .map(field => Argument(field.name, SchemaBuilderUtils.mapToOptionalInputType(field), description = field.description.getOrElse("")))
  }

  def mapToSingleConnectionArguments(model: Model): List[Argument[Option[Any]]] = {
    import SangriaQueryArguments._

    List(whereArgument(model, project))
  }

  def generateFilterElement(input: Map[String, Any], model: Model, isSubscriptionFilter: Boolean = false): DataItemFilterCollection = {
    val filterArguments = new FilterArguments(model, isSubscriptionFilter)

    input
      .map {
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

            case value: Seq[Any] =>
              FilterElement(key, value, field, filter.name)

            case _ =>
              FilterElement(key, value, field, filter.name)
          }
      }
      .toList
      .asInstanceOf[DataItemFilterCollection]
  }

  def extractQueryArgumentsFromContext(model: Model, ctx: Context[ApiUserContext, Unit]): Option[QueryArguments] = {
    val skipOpt = ctx.argOpt[Int]("skip")

    val rawFilterOpt: Option[Map[String, Any]] = ctx.argOpt[Map[String, Any]]("where")
    val filterOpt = rawFilterOpt.map(
      generateFilterElement(_,
                            model
                            //ctx.ctx.isSubscription
      ))

//    if (filterOpt.isDefined) {
//      ctx.ctx.addFeatureMetric(FeatureMetric.Filter)
//    }

    val orderByOpt = ctx.argOpt[OrderBy]("orderBy")
    val afterOpt   = ctx.argOpt[String](IdBasedConnection.Args.After.name)
    val beforeOpt  = ctx.argOpt[String](IdBasedConnection.Args.Before.name)
    val firstOpt   = ctx.argOpt[Int](IdBasedConnection.Args.First.name)
    val lastOpt    = ctx.argOpt[Int](IdBasedConnection.Args.Last.name)

    Some(SangriaQueryArguments.createSimpleQueryArguments(skipOpt, afterOpt, firstOpt, beforeOpt, lastOpt, filterOpt, orderByOpt))
  }

  def extractUniqueArgument(model: models.Model, ctx: Context[ApiUserContext, Unit]): Argument[_] = {

    import cool.graph.util.coolSangria.FromInputImplicit.DefaultScalaResultMarshaller

    val args = model.fields
      .filter(!_.isList)
      .filter(_.isUnique)
      .map(field => Argument(field.name, SchemaBuilderUtils.mapToOptionalInputType(field), description = field.description.getOrElse("")))

    val arg = args.find(a => ctx.args.argOpt(a.name).isDefined) match {
      case Some(value) => value
      case None =>
        ??? //throw UserAPIErrors.GraphQLArgumentsException(s"None of the following arguments provided: ${args.map(_.name)}")
    }

    arg
  }

  def mapToOutputResolve[C <: ApiUserContext](model: Option[models.Model], field: models.Field)(
      ctx: Context[C, DataItem]): sangria.schema.Action[ApiUserContext, _] = {

    val item: DataItem = unwrapDataItemFromContext(ctx)

    if (!field.isScalar) {
      val arguments = extractQueryArgumentsFromContext(field.relatedModel(project).get, ctx.asInstanceOf[Context[ApiUserContext, Unit]])

      if (field.isList) {
        return DeferredValue(
          ToManyDeferred(
            field,
            item.id,
            arguments
          )).map(_.toNodes)
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
      case "id" if resolver && item.userData.contains("id") =>
        item.userData("id").getOrElse(None)

      case "id" =>
        item.id

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

              case TypeIdentifier.DateTime => mapTo(value, x => new DateTime(x.convertTo[String], DateTimeZone.UTC))
              case TypeIdentifier.Enum     => mapTo(value, x => x.convertTo[String])
              case TypeIdentifier.Json     => mapTo(value, x => x.convertTo[JsValue])
            }

          case (Some(value), false) =>
            def mapTo[T](value: Any) = value.asInstanceOf[T]

            field.typeIdentifier match {
              case TypeIdentifier.String    => mapTo[String](value)
              case TypeIdentifier.Int       => mapTo[Int](value)
              case TypeIdentifier.Float     => mapTo[Double](value)
              case TypeIdentifier.Boolean   => mapTo[Boolean](value)
              case TypeIdentifier.GraphQLID => mapTo[String](value)
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
