package cool.graph.subscriptions.resolving

import cool.graph.api.database.{DataItem, DataResolver, FilterElement, QueryArguments}
import cool.graph.api.database.Types.DataItemFilterCollection
import cool.graph.api.schema.{ApiUserContext, ObjectTypeBuilder}
import cool.graph.shared.models.Model
import sangria.schema.Context

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object FilteredResolver {
  def resolve(
      modelObjectTypes: ObjectTypeBuilder,
      model: Model,
      id: String,
      ctx: Context[ApiUserContext, Unit],
      dataResolver: DataResolver
  ): Future[Option[DataItem]] = {

    val filterInput: DataItemFilterCollection = modelObjectTypes
      .extractQueryArgumentsFromContext(model = model, ctx = ctx)
      .flatMap(_.filter)
      .getOrElse(List.empty)

    def removeTopLevelIdFilter(element: Any) =
      element match {
        case e: FilterElement => e.key != "id"
        case _                => true
      }

    val filter = filterInput.filter(removeTopLevelIdFilter) ++ List(FilterElement(key = "id", value = id, field = Some(model.getFieldByName_!("id"))))

    dataResolver
      .resolveByModel(
        model,
        Some(QueryArguments(filter = Some(filter), skip = None, after = None, first = None, before = None, last = None, orderBy = None))
      )
      .map(_.items.headOption)
  }
}
