package com.prisma.api.schema

import com.prisma.api.connector.{DataItem, DataResolver, PrismaNode}
import com.prisma.shared.models.ModelMutationType.ModelMutationType
import com.prisma.shared.models.{Model, Project}
import sangria.schema
import sangria.schema._

case class OutputTypesBuilder(project: Project, objectTypes: Map[String, ObjectType[ApiUserContext, DataItem]], masterDataResolver: DataResolver) {

  def mapOutputType[C](model: Model, objectType: ObjectType[C, DataItem], onlyId: Boolean): ObjectType[C, SimpleResolveOutput] = {
    ObjectType[C, SimpleResolveOutput](
      name = objectType.name,
      fieldsFn = () => {
        objectType.ownFields.toList
//          .filterNot( => field.isHidden)
          .filter(field => if (onlyId) field.name == "id" else true)
          .map { field =>
            field.copy(
              resolve = { outerCtx: Context[C, SimpleResolveOutput] =>
                val castedCtx = outerCtx.asInstanceOf[Context[C, DataItem]]
                field.resolve(castedCtx.copy(value = outerCtx.value.node.toDataItem))
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

  def mapCreateOutputType[C](model: Model, objectType: ObjectType[C, DataItem]): ObjectType[C, SimpleResolveOutput] = {
    mapOutputType(model, objectType, onlyId = false)
  }

  def mapUpdateOutputType[C](model: Model, objectType: ObjectType[C, DataItem]): ObjectType[C, SimpleResolveOutput] = {
    mapOutputType(model, objectType, onlyId = false)
  }

  def mapUpsertOutputType[C](model: Model, objectType: ObjectType[C, DataItem]): ObjectType[C, SimpleResolveOutput] = {
    mapOutputType(model, objectType, onlyId = false)
  }

  def mapSubscriptionOutputType[C](
      model: Model,
      objectType: ObjectType[C, DataItem],
      updatedFields: Option[List[String]] = None,
      mutation: ModelMutationType = com.prisma.shared.models.ModelMutationType.Created,
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
                case None    => Some(parentCtx.value)
                case Some(_) => None
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

  def mapDeleteOutputType[C](model: Model, objectType: ObjectType[C, DataItem], onlyId: Boolean): ObjectType[C, SimpleResolveOutput] =
    mapOutputType(model, objectType, onlyId)

  type R = SimpleResolveOutput

  def mapResolve(node: PrismaNode, args: Args): SimpleResolveOutput = SimpleResolveOutput(node, args)
}

case class SimpleResolveOutput(node: PrismaNode, args: Args)
