package com.prisma.subscriptions.resolving

import com.prisma.api.connector._
import com.prisma.api.schema.ObjectTypeBuilder
import com.prisma.gc_values.IdGCValue
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
  ): Future[Option[PrismaNode]] = {

    val filterInput: AndFilter = modelObjectTypes
      .extractQueryArgumentsFromContextForSubscription(model = model, ctx = ctx)
      .flatMap(_.filter.map(_.asInstanceOf[AndFilter]))
      .getOrElse(AndFilter(Vector.empty))

    def removeTopLevelIdFilter(element: Filter) =
      element match {
        case e: ScalarFilter => e.field.name != "id"
        case _               => true
      }

    val filterValues = filterInput.filters.filter(removeTopLevelIdFilter) ++ Vector(ScalarFilter(model.idField_!, Equals(IdGCValue(id))))
    val filter       = AndFilter(filterValues)
    dataResolver.resolveByModel(model, Some(QueryArguments.withFilter(filter = filter))).map(_.nodes.headOption)
  }
}
