package cool.graph

import cool.graph.Types.DataItemFilterCollection
import cool.graph.client.database.{DataResolver, QueryArguments}
import cool.graph.client.schema.SchemaModelObjectTypesBuilder
import cool.graph.shared.models.Model
import sangria.schema.Context

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object FilteredResolver {
  def resolve[ManyDataItemType, C <: RequestContextTrait](modelObjectTypes: SchemaModelObjectTypesBuilder[ManyDataItemType],
                                                          model: Model,
                                                          id: String,
                                                          ctx: Context[C, Unit],
                                                          dataResolver: DataResolver): Future[Option[DataItem]] = {

    val filterInput: DataItemFilterCollection = modelObjectTypes
      .extractQueryArgumentsFromContext(model = model, ctx = ctx)
      .flatMap(_.filter)
      .getOrElse(List())

    def removeTopLevelIdFilter(element: Any) =
      element match {
        case e: FilterElement => e.key != "id"
        case _                => true
      }

    val filter = filterInput.filter(removeTopLevelIdFilter(_)) ++ List(FilterElement(key = "id", value = id, field = Some(model.getFieldByName_!("id"))))

    dataResolver
      .resolveByModel(
        model,
        Some(QueryArguments(filter = Some(filter), skip = None, after = None, first = None, before = None, last = None, orderBy = None))
      )
      .map(_.items.headOption)
  }
}
