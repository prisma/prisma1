package cool.graph.client.schema.simple

import cool.graph.DataItem
import cool.graph.client.database.DeferredTypes.{CountToManyDeferred, SimpleConnectionOutputType}
import cool.graph.client.database.QueryArguments
import cool.graph.client.schema.SchemaModelObjectTypesBuilder
import cool.graph.client.{SangriaQueryArguments, UserContext}
import cool.graph.shared.models
import cool.graph.shared.models.Field
import sangria.schema._
import scaldi.Injector

class SimpleSchemaModelObjectTypeBuilder(project: models.Project,
                                         nodeInterface: Option[InterfaceType[UserContext, DataItem]] = None,
                                         modelPrefix: String = "",
                                         withRelations: Boolean = true,
                                         onlyId: Boolean = false)(implicit inj: Injector)
    extends SchemaModelObjectTypesBuilder[SimpleConnectionOutputType](
      project,
      nodeInterface,
      modelPrefix = modelPrefix,
      withRelations = withRelations,
      onlyId = onlyId
    ) {

  val metaObjectType = sangria.schema.ObjectType(
    "_QueryMeta",
    description = "Meta information about the query.",
    fields = sangria.schema.fields[UserContext, DataItem](
      sangria.schema
        .Field(name = "count", fieldType = sangria.schema.IntType, resolve = _.value.get[CountToManyDeferred]("count"))
    )
  )

  override def resolveConnection(field: Field): OutputType[Any] = {
    field.isList match {
      case true =>
        ListType(modelObjectTypes.get(field.relatedModel(project).get.name).get)
      case false =>
        modelObjectTypes.get(field.relatedModel(project).get.name).get
    }
  }

  override def mapMetaRelationField(model: models.Model)(field: models.Field): Option[sangria.schema.Field[UserContext, DataItem]] = {

    (field.relation, field.isList) match {
      case (Some(_), true) =>
        val inputArguments = mapToListConnectionArguments(model, field)

        Some(
          sangria.schema.Field(
            s"_${field.name}Meta",
            fieldType = metaObjectType,
            description = Some("Meta information about the query."),
            arguments = mapToListConnectionArguments(model, field),
            resolve = (ctx: Context[UserContext, DataItem]) => {

              val item: DataItem = unwrapDataItemFromContext(ctx)

              val queryArguments: Option[QueryArguments] =
                extractQueryArgumentsFromContext(field.relatedModel(project).get, ctx.asInstanceOf[Context[UserContext, Unit]])

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
}
