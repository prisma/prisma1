package com.prisma.api.schema

import com.prisma.api.connector.{DataResolver, PrismaNode}
import com.prisma.shared.models.ModelMutationType.ModelMutationType
import com.prisma.shared.models.{Model, Project}
import sangria.schema
import sangria.schema._

case class OutputTypesBuilder(project: Project, objectTypes: Map[String, ObjectType[ApiUserContext, PrismaNode]], masterDataResolver: DataResolver) {
  import com.prisma.utils.boolean.BooleanUtils._

  private def previousValuesObjectType[C](model: Model, objectType: ObjectType[C, PrismaNode]): Option[ObjectType[C, PrismaNode]] = {
    def isIncluded(outputType: OutputType[_]): Boolean = {
      outputType match {
        case _: ScalarType[_] | _: EnumType[_] => true
        case ListType(x)                       => isIncluded(x)
        case OptionType(x)                     => isIncluded(x)
        case _                                 => false
      }
    }
    val fields = objectType.ownFields.toList.collect {
      case field if isIncluded(field.fieldType) => field.copy(resolve = (outerCtx: Context[C, PrismaNode]) => field.resolve(outerCtx))
    }

    fields.nonEmpty.toOption {
      ObjectType[C, PrismaNode](
        name = s"${objectType.name}PreviousValues",
        fieldsFn = () => fields
      )
    }
  }

  def mapSubscriptionOutputType[C](
      model: Model,
      objectType: ObjectType[C, PrismaNode],
      updatedFields: Option[List[String]] = None,
      mutation: ModelMutationType = com.prisma.shared.models.ModelMutationType.Created,
      previousValues: Option[PrismaNode] = None,
      dataItem: Option[PrismaNode] = None
  ): ObjectType[C, PrismaNode] = {
    ObjectType[C, PrismaNode](
      name = s"${model.name}SubscriptionPayload",
      fieldsFn = () =>
        List(
          schema.Field(
            name = "mutation",
            fieldType = ModelMutationType.Type,
            description = None,
            arguments = List(),
            resolve = (outerCtx: Context[C, PrismaNode]) => mutation
          ),
          schema.Field(
            name = "node",
            fieldType = OptionType(objectType),
            description = None,
            arguments = List(),
            resolve = (parentCtx: Context[C, PrismaNode]) =>
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
            resolve = (outerCtx: Context[C, PrismaNode]) => updatedFields
          )
        ) ++
          previousValuesObjectType(model, objectType).map { objectType =>
            schema.Field(
              name = "previousValues",
              fieldType = OptionType(objectType),
              description = None,
              arguments = List(),
              resolve = (outerCtx: Context[C, PrismaNode]) => previousValues
            )
        }
    )
  }
}
