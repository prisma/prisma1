package cool.graph.relay.schema

import cool.graph.DataItem
import cool.graph.client.database.{DefaultEdge, Edge}
import cool.graph.client.schema.OutputMapper
import cool.graph.client.schema.relay.RelayResolveOutput
import cool.graph.client.UserContext
import cool.graph.shared.{ApiMatrixFactory}
import cool.graph.shared.models.ModelMutationType.ModelMutationType
import cool.graph.shared.models.{Model, Project, Relation}
import sangria.schema.{Args, Field, ObjectType, OptionType, fields}
import scaldi.{Injectable, Injector}

import scala.concurrent.ExecutionContext.Implicits.global

class RelayOutputMapper(
    viewerType: ObjectType[UserContext, Unit],
    edgeObjectTypes: => Map[String, ObjectType[UserContext, Edge[DataItem]]],
    modelObjectTypes: Map[String, ObjectType[UserContext, DataItem]],
    project: Project
)(implicit inj: Injector)
    extends OutputMapper
    with Injectable {

  type R = RelayResolveOutput
  type C = UserContext

  val apiMatrix = inject[ApiMatrixFactory].create(project)

  def nodePaths(model: Model) = List(List(model.getCamelCasedName))

  def createUpdateDeleteFields[C](model: Model, objectType: ObjectType[C, DataItem]): List[Field[C, RelayResolveOutput]] =
    List(
      Field[C, RelayResolveOutput, Any, Any](name = "viewer", fieldType = viewerType, description = None, arguments = List(), resolve = ctx => ()),
      Field[C, RelayResolveOutput, Any, Any](name = "clientMutationId",
                                             fieldType = sangria.schema.StringType,
                                             description = None,
                                             arguments = List(),
                                             resolve = ctx => {
                                               ctx.value.clientMutationId
                                             }),
      Field[C, RelayResolveOutput, Any, Any](name = model.getCamelCasedName,
                                             fieldType = OptionType(objectType),
                                             description = None,
                                             arguments = List(),
                                             resolve = ctx => {
                                               ctx.value.item
                                             }),
      Field[C, RelayResolveOutput, Any, Any](
        name = "edge",
        fieldType = OptionType(edgeObjectTypes(model.name)),
        description = None,
        arguments = List(),
        resolve = ctx => DefaultEdge(ctx.value.item, ctx.value.item.id)
      )
    ) ++
      model.relationFields
        .filter(apiMatrix.includeField)
        .filter(!_.isList)
        .map(oneConnectionField =>
          Field[C, RelayResolveOutput, Any, Any](
            name = model.getCamelCasedName match {
              case oneConnectionField.name =>
                s"${oneConnectionField.name}_"
              case _ =>
                oneConnectionField.name
            },
            fieldType = OptionType(
              modelObjectTypes(oneConnectionField
                .relatedModel(project)
                .get
                .name)),
            description = None,
            arguments = List(),
            resolve = ctx =>
              ctx.ctx
                .asInstanceOf[UserContext]
                .mutationDataresolver
                .resolveByRelation(oneConnectionField, ctx.value.item.id, None)
                .map(_.items.headOption)
        )): List[Field[C, RelayResolveOutput]]

  def connectionFields[C](relation: Relation,
                          fromModel: Model,
                          fromField: cool.graph.shared.models.Field,
                          toModel: Model,
                          objectType: ObjectType[C, DataItem]): List[Field[C, RelayResolveOutput]] =
    List(
      Field[C, RelayResolveOutput, Any, Any](name = "viewer", fieldType = viewerType, description = None, arguments = List(), resolve = ctx => ()),
      Field[C, RelayResolveOutput, Any, Any](name = "clientMutationId",
                                             fieldType = sangria.schema.StringType,
                                             description = None,
                                             arguments = List(),
                                             resolve = ctx => {
                                               ctx.value.clientMutationId
                                             }),
      Field[C, RelayResolveOutput, Any, Any](name = relation.bName(project),
                                             fieldType = OptionType(objectType),
                                             description = None,
                                             arguments = List(),
                                             resolve = ctx => {
                                               ctx.value.item
                                             }),
      Field[C, RelayResolveOutput, Any, Any](
        name = relation.aName(project),
        fieldType = OptionType(
          modelObjectTypes(
            fromField
              .relatedModel(project)
              .get
              .name)),
        description = None,
        arguments = List(),
        resolve = ctx => {
          val mutationKey =
            s"${fromField.relation.get.aName(project = project)}Id"
          val input = ctx.value.args
            .arg[Map[String, String]]("input")
          val id =
            input(mutationKey)
          ctx.ctx
            .asInstanceOf[UserContext]
            .mutationDataresolver
            .resolveByUnique(toModel, "id", id)
            .map(_.get)
        }
      ),
      Field[C, RelayResolveOutput, Any, Any](
        name = s"${relation.bName(project)}Edge",
        fieldType = OptionType(edgeObjectTypes(fromModel.name)),
        description = None,
        arguments = List(),
        resolve = ctx => {
          DefaultEdge(ctx.value.item, ctx.value.item.id)
        }
      ),
      Field[C, RelayResolveOutput, Any, Any](
        name = s"${relation.aName(project)}Edge",
        fieldType = OptionType(edgeObjectTypes(fromField.relatedModel(project).get.name)),
        description = None,
        arguments = List(),
        resolve = ctx => {
          val mutationKey =
            s"${fromField.relation.get.aName(project = project)}Id"
          val input = ctx.value.args.arg[Map[String, String]]("input")
          val id    = input(mutationKey)

          ctx.ctx
            .asInstanceOf[UserContext]
            .mutationDataresolver
            .resolveByUnique(toModel, "id", id)
            .map(item => DefaultEdge(item.get, id))
        }
      )
    )

  def deletedIdField[C]() =
    Field[C, RelayResolveOutput, Any, Any](name = "deletedId",
                                           fieldType = OptionType(sangria.schema.IDType),
                                           description = None,
                                           arguments = List(),
                                           resolve = ctx => ctx.value.item.id)

  override def mapCreateOutputType[C](model: Model, objectType: ObjectType[C, DataItem]): ObjectType[C, RelayResolveOutput] = {
    ObjectType[C, RelayResolveOutput](
      name = s"Create${model.name}Payload",
      () => fields[C, RelayResolveOutput](createUpdateDeleteFields(model, objectType): _*)
    )
  }

  // this is just a dummy method which isn't used right now, as the subscriptions are only available for the simple schema now
  override def mapSubscriptionOutputType[C](
      model: Model,
      objectType: ObjectType[C, DataItem],
      updatedFields: Option[List[String]] = None,
      mutation: ModelMutationType = cool.graph.shared.models.ModelMutationType.Created,
      previousValues: Option[DataItem] = None,
      dataItem: Option[RelayResolveOutput]
  ): ObjectType[C, RelayResolveOutput] = {
    ObjectType[C, RelayResolveOutput](
      name = s"Create${model.name}Payload",
      () => List()
    )
  }

  override def mapUpdateOutputType[C](model: Model, objectType: ObjectType[C, DataItem]): ObjectType[C, RelayResolveOutput] = {
    ObjectType[C, RelayResolveOutput](
      name = s"Update${model.name}Payload",
      () => fields[C, RelayResolveOutput](createUpdateDeleteFields(model, objectType): _*)
    )
  }

  override def mapUpdateOrCreateOutputType[C](model: Model, objectType: ObjectType[C, DataItem]): ObjectType[C, RelayResolveOutput] = {
    ObjectType[C, RelayResolveOutput](
      name = s"UpdateOrCreate${model.name}Payload",
      () => fields[C, RelayResolveOutput](createUpdateDeleteFields(model, objectType): _*)
    )
  }

  override def mapDeleteOutputType[C](model: Model, objectType: ObjectType[C, DataItem], onlyId: Boolean = false): ObjectType[C, RelayResolveOutput] = {
    ObjectType[C, RelayResolveOutput](
      name = s"Delete${model.name}Payload",
      () => fields[C, RelayResolveOutput](createUpdateDeleteFields(model, objectType) :+ deletedIdField(): _*)
    )
  }

  override def mapAddToRelationOutputType[C](relation: Relation,
                                             fromModel: Model,
                                             fromField: cool.graph.shared.models.Field,
                                             toModel: Model,
                                             objectType: ObjectType[C, DataItem],
                                             payloadName: String): ObjectType[C, RelayResolveOutput] = {
    ObjectType[C, RelayResolveOutput](
      name = s"${payloadName}Payload",
      () => fields[C, RelayResolveOutput](connectionFields(relation, fromModel, fromField, toModel, objectType): _*)
    )
  }

  override def mapRemoveFromRelationOutputType[C](relation: Relation,
                                                  fromModel: Model,
                                                  fromField: cool.graph.shared.models.Field,
                                                  toModel: Model,
                                                  objectType: ObjectType[C, DataItem],
                                                  payloadName: String): ObjectType[C, RelayResolveOutput] = {
    ObjectType[C, RelayResolveOutput](
      name = s"${payloadName}Payload",
      () => fields[C, RelayResolveOutput](connectionFields(relation, fromModel, fromField, toModel, objectType): _*)
    )
  }

  override def mapResolve(item: DataItem, args: Args): RelayResolveOutput =
    RelayResolveOutput(args
                         .arg[Map[String, Any]]("input")("clientMutationId")
                         .asInstanceOf[String],
                       item,
                       args)

}
