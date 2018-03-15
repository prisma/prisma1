package com.prisma.api.schema

import com.prisma.api.connector.{DataItem, OrderBy, QueryArguments}
import com.prisma.api.connector.mysql.database.Types.DataItemFilterCollection
import com.prisma.api.connector.mysql.database._
import com.prisma.api.mutations.BatchPayload
import com.prisma.api.resolver.DeferredTypes._
import com.prisma.api.resolver.{IdBasedConnection, IdBasedConnectionDefinition}
import com.prisma.api.schema.CustomScalarTypes.{DateTimeType, JsonType}
import com.prisma.shared.models
import com.prisma.shared.models.{Field, Model, TypeIdentifier}
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}
import sangria.schema.{Field => SangriaField, _}
import spray.json.DefaultJsonProtocol._
import spray.json.{JsValue, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

class ObjectTypeBuilder(
    project: models.Project,
    nodeInterface: Option[InterfaceType[ApiUserContext, DataItem]] = None,
    withRelations: Boolean = true,
    onlyId: Boolean = false
) {
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

  val modelObjectTypes: Map[String, ObjectType[ApiUserContext, DataItem]] = project.models.map(model => (model.name, modelToObjectType(model))).toMap

  val modelConnectionTypes: Map[String, ObjectType[ApiUserContext, IdBasedConnection[DataItem]]] =
    project.models.map(model => (model.name, modelToConnectionType(model).connectionType)).toMap

  def modelToConnectionType(model: Model): IdBasedConnectionDefinition[ApiUserContext, IdBasedConnection[DataItem], DataItem] = {
    IdBasedConnection.definition[ApiUserContext, IdBasedConnection, DataItem](
      name = model.name,
      nodeType = modelObjectTypes(model.name),
      connectionFields = {

        List(
          SangriaField(
            "aggregate",
            aggregateTypeForModel(model),
            resolve = (ctx: Context[ApiUserContext, IdBasedConnection[DataItem]]) => {
              val emptyQueryArguments = QueryArguments(None, None, None, None, None, None, None)
              ctx.value.parent.args.getOrElse(emptyQueryArguments)
            }
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

  protected def modelToObjectType(model: models.Model): ObjectType[ApiUserContext, DataItem] = {
    new ObjectType(
      name = model.name,
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
    resolve = (ctx: Context[ApiUserContext, DataItem]) => { mapToOutputResolve(model, field)(ctx) },
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
                      toModel = field.get.relatedModel(project.schema).get,
                      relation = field.get.relation.get,
                      filter = generateFilterElement(typedValue, field.get.relatedModel(project.schema).get, isSubscriptionFilter)
                    ))
                )
              }

            case value: Seq[Any] if value.nonEmpty && value.head.isInstanceOf[Map[_, _]] =>
              FilterElement(key, value.asInstanceOf[Seq[Map[String, Any]]].map(generateFilterElement(_, model, isSubscriptionFilter)), None, filter.name)

            case value: Seq[Any] =>
              FilterElement(key, value, field, filter.name)

            case Some(filterValue) =>
              FilterElement(key, filterValue, field, filter.name)

            case _ =>
              FilterElement(key, value, field, filter.name)
          }
      }
      .toList
      .asInstanceOf[DataItemFilterCollection]
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

  def extractRequiredFilterFromContext(model: Model, ctx: Context[ApiUserContext, Unit]): Types.DataItemFilterCollection = {
    val rawFilter: Map[String, Any] = ctx.arg[Map[String, Any]]("where")
    val unwrappedValues = rawFilter.map {
      case (k, Some(x)) => (k, x)
      case (k, v)       => (k, v)
    }
    generateFilterElement(unwrappedValues, model, isSubscriptionFilter = false)
  }

  def extractUniqueArgument(model: models.Model, ctx: Context[ApiUserContext, Unit]): Argument[_] = {

    import com.prisma.util.coolSangria.FromInputImplicit.DefaultScalaResultMarshaller

    val args = model.scalarNonListFields
      .filter(_.isUnique)
      .map(field => Argument(field.name, SchemaBuilderUtils.mapToOptionalInputType(field), description = field.description.getOrElse("")))

    val arg = args.find(a => ctx.args.argOpt(a.name).isDefined) match {
      case Some(value) => value
      case None        => ??? //throw UserAPIErrors.GraphQLArgumentsException(s"None of the following arguments provided: ${args.map(_.name)}")
    }

    arg
  }

  def mapToOutputResolve[C <: ApiUserContext](model: models.Model, field: models.Field)(ctx: Context[C, DataItem]): sangria.schema.Action[ApiUserContext, _] = {

    val item: DataItem = unwrapDataItemFromContext(ctx)

    if (!field.isScalar) {
      val arguments = extractQueryArgumentsFromContext(field.relatedModel(project.schema).get, ctx.asInstanceOf[Context[ApiUserContext, Unit]])

      if (field.isList) {
        DeferredValue(
          ToManyDeferred(
            field,
            item.id,
            arguments
          )).map(_.toNodes)
      } else {
        ToOneDeferred(field, item.id, arguments)
      }

    } else {
      if (field.isList) {
        ScalarListDeferred(model, field, item.id)
      } else {
        ObjectTypeBuilder.convertScalarFieldValueFromDatabase(field, item)
      }
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
  def convertScalarFieldValueFromDatabase(field: models.Field, item: DataItem): Any = {
    field.name match {
      case "id" =>
        item.id.trim

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
