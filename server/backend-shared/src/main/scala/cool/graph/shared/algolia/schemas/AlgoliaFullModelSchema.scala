package cool.graph.shared.algolia.schemas

import cool.graph.Types.DataItemFilterCollection
import cool.graph.client.database.QueryArguments
import cool.graph.client.SangriaQueryArguments
import cool.graph.client.schema.SchemaModelObjectTypesBuilder
import cool.graph.shared.algolia.AlgoliaFullModelContext
import cool.graph.shared.models.{Model, Project}
import sangria.schema.{Field, ListType, ObjectType, Schema}
import scaldi.{Injectable, Injector}

import scala.concurrent.ExecutionContextExecutor

class AlgoliaFullModelSchema[ManyDataItemType](project: Project, model: Model, modelObjectTypes: SchemaModelObjectTypesBuilder[ManyDataItemType])(
    implicit injector: Injector)
    extends Injectable {

  implicit val dispatcher =
    inject[ExecutionContextExecutor](identified by "dispatcher")

  val algoliaSyncField: Field[AlgoliaFullModelContext, Unit] = Field(
    "node",
    description = Some("The table to synchronize with Algolia."),
    arguments = List(SangriaQueryArguments.filterArgument(model = model, project = project)),
    fieldType = ListType(modelObjectTypes.modelObjectTypes(model.name)),
    resolve = (ctx) => {

      val filter: DataItemFilterCollection = modelObjectTypes
        .extractQueryArgumentsFromContext(model = model, ctx = ctx)
        .flatMap(_.filter)
        .getOrElse(List())

      val arguments = Some(QueryArguments(filter = Some(filter), skip = None, after = None, first = None, before = None, last = None, orderBy = None))

      ctx.ctx.dataResolver
        .resolveByModel(model, arguments)
        .map(result => result.items)
    }
  )

  def build(): Schema[AlgoliaFullModelContext, Unit] = {
    val Query = ObjectType(
      "Query",
      List(algoliaSyncField)
    )

    Schema(Query)
  }
}
