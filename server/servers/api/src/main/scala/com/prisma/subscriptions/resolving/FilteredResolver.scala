package com.prisma.subscriptions.resolving

import com.prisma.api.connector._
import com.prisma.api.schema.{ObjectTypeBuilder, SangriaExtensions}
import com.prisma.gc_values.IdGCValue
import com.prisma.shared.models.Model
import sangria.schema.Context

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object FilteredResolver extends SangriaExtensions {
  def resolve(
      modelObjectTypes: ObjectTypeBuilder,
      model: Model,
      id: IdGCValue,
      ctx: Context[_, Unit],
      dataResolver: DataResolver
  ): Future[Option[PrismaNode]] = {

    val filterInput: AndFilter = modelObjectTypes
      .extractQueryArgumentsFromContextForSubscription(model = model, ctx = ctx)
      .filter
      .map(_.asInstanceOf[AndFilter])
      .getOrElse(AndFilter(Vector.empty))

    def removeTopLevelIdFilter(element: Filter) =
      element match {
        case e: ScalarFilter => e.field.name != "id"
        case _               => true
      }

    val filterValues = filterInput.filters.filter(removeTopLevelIdFilter) ++ Vector(ScalarFilter(model.idField_!, Equals(id)))
    val filter       = AndFilter(filterValues)
    dataResolver.getNodes(model, QueryArguments.withFilter(filter = filter), ctx.getSelectedFields(model)).map(_.nodes.headOption)
  }
}
