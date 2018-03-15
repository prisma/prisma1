package com.prisma.subscriptions.resolving

import com.prisma.api.connector.Types.DataItemFilterCollection
import com.prisma.api.connector.mysql.database.FilterElement
import com.prisma.api.connector.{DataItem, DataResolver, QueryArguments}
import com.prisma.api.schema.ObjectTypeBuilder
import com.prisma.shared.models.Model
import sangria.schema.Context

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object FilteredResolver {
  def resolve(
      modelObjectTypes: ObjectTypeBuilder,
      model: Model,
      id: String,
      ctx: Context[_, Unit],
      dataResolver: DataResolver
  ): Future[Option[DataItem]] = {

    val filterInput: DataItemFilterCollection = modelObjectTypes
      .extractQueryArgumentsFromContextForSubscription(model = model, ctx = ctx)
      .flatMap(_.filter)
      .getOrElse(List.empty)

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
