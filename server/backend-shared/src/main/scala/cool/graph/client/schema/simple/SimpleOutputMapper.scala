package cool.graph.client.schema.simple

import cool.graph.DataItem
import cool.graph.client.UserContext
import cool.graph.client.schema.{ModelMutationType, OutputMapper}
import cool.graph.shared.models.ModelMutationType.ModelMutationType
import cool.graph.shared.models.{Field, Model, Project, Relation}
import sangria.schema
import sangria.schema._
import scaldi.{Injectable, Injector}

import scala.concurrent.ExecutionContext.Implicits.global

case class SimpleOutputMapper(project: Project, modelObjectTypes: Map[String, ObjectType[UserContext, DataItem]])(implicit inj: Injector)
    extends OutputMapper
    with Injectable {

  def nodePaths(model: Model) = List(List())

  def mapOutputType[C](model: Model, objectType: ObjectType[C, DataItem], onlyId: Boolean): ObjectType[C, SimpleResolveOutput] = {
    ObjectType[C, SimpleResolveOutput](
      name = objectType.name,
      fieldsFn = () => {
        objectType.ownFields.toList
          .filter(field => if (onlyId) field.name == "id" else true)
          .map { field =>
            field.copy(
              resolve = { outerCtx: Context[C, SimpleResolveOutput] =>
                val castedCtx = outerCtx.asInstanceOf[Context[C, DataItem]]
                field.resolve(castedCtx.copy(value = outerCtx.value.item))
              }
            )
          }
      }
    )
  }

  def mapPreviousValuesOutputType[C](model: Model, objectType: ObjectType[C, DataItem]): ObjectType[C, DataItem] = {
    def isIncluded(outputType: OutputType[_]): Boolean = {
      outputType match {
        case _: ScalarType[_] | _: EnumType[_] => true
        case ListType(x)                       => isIncluded(x)
        case OptionType(x)                     => isIncluded(x)
        case _                                 => false
      }
    }
    val fields = objectType.ownFields.toList.collect {
      case field if isIncluded(field.fieldType) =>
        field.copy(
          resolve = (outerCtx: Context[C, DataItem]) => field.resolve(outerCtx)
        )
    }

    ObjectType[C, DataItem](
      name = s"${objectType.name}PreviousValues",
      fieldsFn = () => fields
    )
  }

  override def mapCreateOutputType[C](model: Model, objectType: ObjectType[C, DataItem]): ObjectType[C, SimpleResolveOutput] = {
    mapOutputType(model, objectType, false)
  }

  override def mapUpdateOutputType[C](model: Model, objectType: ObjectType[C, DataItem]): ObjectType[C, SimpleResolveOutput] = {
    mapOutputType(model, objectType, false)
  }

  override def mapUpdateOrCreateOutputType[C](model: Model, objectType: ObjectType[C, DataItem]): ObjectType[C, SimpleResolveOutput] = {
    mapOutputType(model, objectType, false)
  }

  override def mapSubscriptionOutputType[C](
      model: Model,
      objectType: ObjectType[C, DataItem],
      updatedFields: Option[List[String]] = None,
      mutation: ModelMutationType = cool.graph.shared.models.ModelMutationType.Created,
      previousValues: Option[DataItem] = None,
      dataItem: Option[SimpleResolveOutput] = None
  ): ObjectType[C, SimpleResolveOutput] = {
    ObjectType[C, SimpleResolveOutput](
      name = s"${model.name}SubscriptionPayload",
      fieldsFn = () =>
        List(
          schema.Field(
            name = "mutation",
            fieldType = ModelMutationType.Type,
            description = None,
            arguments = List(),
            resolve = (outerCtx: Context[C, SimpleResolveOutput]) => mutation
          ),
          schema.Field(
            name = "node",
            fieldType = OptionType(mapOutputType(model, objectType, false)),
            description = None,
            arguments = List(),
            resolve = (parentCtx: Context[C, SimpleResolveOutput]) =>
              dataItem match {
                case None =>
                  Some(parentCtx.value)
                case Some(x) =>
                  None
            }
          ),
          schema.Field(
            name = "updatedFields",
            fieldType = OptionType(ListType(StringType)),
            description = None,
            arguments = List(),
            resolve = (outerCtx: Context[C, SimpleResolveOutput]) => updatedFields
          ),
          schema.Field(
            name = "previousValues",
            fieldType = OptionType(mapPreviousValuesOutputType(model, objectType)),
            description = None,
            arguments = List(),
            resolve = (outerCtx: Context[C, SimpleResolveOutput]) => previousValues
          )
      )
    )
  }

  override def mapDeleteOutputType[C](model: Model, objectType: ObjectType[C, DataItem], onlyId: Boolean): ObjectType[C, SimpleResolveOutput] =
    mapOutputType(model, objectType, onlyId)

  override type R = SimpleResolveOutput

  override def mapResolve(item: DataItem, args: Args): SimpleResolveOutput =
    SimpleResolveOutput(item, args)

  override def mapAddToRelationOutputType[C](relation: Relation,
                                             fromModel: Model,
                                             fromField: Field,
                                             toModel: Model,
                                             objectType: ObjectType[C, DataItem],
                                             payloadName: String): ObjectType[C, SimpleResolveOutput] =
    ObjectType[C, SimpleResolveOutput](
      name = s"${payloadName}Payload",
      () => fields[C, SimpleResolveOutput](connectionFields(relation, fromModel, fromField, toModel, objectType): _*)
    )

  override def mapRemoveFromRelationOutputType[C](relation: Relation,
                                                  fromModel: Model,
                                                  fromField: Field,
                                                  toModel: Model,
                                                  objectType: ObjectType[C, DataItem],
                                                  payloadName: String): ObjectType[C, SimpleResolveOutput] =
    ObjectType[C, SimpleResolveOutput](
      name = s"${payloadName}Payload",
      () => fields[C, SimpleResolveOutput](connectionFields(relation, fromModel, fromField, toModel, objectType): _*)
    )

  def connectionFields[C](relation: Relation,
                          fromModel: Model,
                          fromField: Field,
                          toModel: Model,
                          objectType: ObjectType[C, DataItem]): List[sangria.schema.Field[C, SimpleResolveOutput]] =
    List(
      schema.Field[C, SimpleResolveOutput, Any, Any](name = relation.bName(project),
                                                     fieldType = OptionType(objectType),
                                                     description = None,
                                                     arguments = List(),
                                                     resolve = ctx => {
                                                       ctx.value.item
                                                     }),
      schema.Field[C, SimpleResolveOutput, Any, Any](
        name = relation.aName(project),
        fieldType = OptionType(modelObjectTypes(fromField.relatedModel(project).get.name)),
        description = None,
        arguments = List(),
        resolve = ctx => {
          val mutationKey = s"${fromField.relation.get.aName(project = project)}Id"
          ctx.ctx
            .asInstanceOf[UserContext]
            .mutationDataresolver
            .resolveByUnique(toModel, "id", ctx.value.args.arg[String](mutationKey))
            .map(_.get)
        }
      )
    )
}

case class SimpleResolveOutput(item: DataItem, args: Args)
