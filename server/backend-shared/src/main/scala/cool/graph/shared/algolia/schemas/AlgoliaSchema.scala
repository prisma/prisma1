package cool.graph.shared.algolia.schemas

import cool.graph.client.SangriaQueryArguments
import cool.graph.client.schema.SchemaModelObjectTypesBuilder
import cool.graph.shared.algolia.AlgoliaContext
import cool.graph.shared.models.{Model, Project}
import cool.graph.{DataItem, FilteredResolver}
import sangria.schema.{Context, Field, ObjectType, OptionType, Schema}
import scaldi.{Injectable, Injector}

import scala.concurrent.{ExecutionContextExecutor, Future}

class AlgoliaSchema[ManyDataItemType](project: Project, model: Model, modelObjectTypes: SchemaModelObjectTypesBuilder[ManyDataItemType])(
    implicit injector: Injector)
    extends Injectable {

  implicit val dispatcher =
    inject[ExecutionContextExecutor](identified by "dispatcher")

  def resolve[ManyDataItemType](ctx: Context[AlgoliaContext, Unit]): Future[Option[DataItem]] = {
    FilteredResolver.resolve(modelObjectTypes, model, ctx.ctx.nodeId, ctx, ctx.ctx.dataResolver)
  }

  val algoliaSyncField: Field[AlgoliaContext, Unit] = Field(
    "node",
    description = Some("The model to synchronize with Algolia."),
    arguments = List(SangriaQueryArguments.filterArgument(model = model, project = project)),
    fieldType = OptionType(modelObjectTypes.modelObjectTypes.get(model.name).get),
    resolve = (ctx) => resolve(ctx)
  )

  def build(): Schema[AlgoliaContext, Unit] = {
    val Query = ObjectType(
      "Query",
      List(algoliaSyncField)
    )

    Schema(Query)
  }
}
