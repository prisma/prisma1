package com.prisma.subscriptions.schema

import com.prisma.api.connector.{DataResolver, PrismaNode}
import com.prisma.api.schema.{ObjectTypeBuilder, SimpleResolveOutput}
import com.prisma.gc_values.IdGCValue
import com.prisma.shared.models.Model
import com.prisma.subscriptions.SubscriptionUserContext
import com.prisma.subscriptions.resolving.FilteredResolver
import com.prisma.util.gc_value.GCCreateReallyCoolArgsConverter
import sangria.schema.{Args, Context}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object SubscriptionDataResolver {

  def resolve(dataResolver: DataResolver,
              modelObjectTypes: ObjectTypeBuilder,
              model: Model,
              ctx: Context[SubscriptionUserContext, Unit]): Future[Option[SimpleResolveOutput]] = {
    FilteredResolver
      .resolve(modelObjectTypes, model, ctx.ctx.nodeId, ctx, dataResolver)
      .map(_.map { dataItem =>
        val converter      = GCCreateReallyCoolArgsConverter(model)
        val reallyCoolArgs = converter.toReallyCoolArgs(dataItem.userData)

        val node = PrismaNode(IdGCValue(dataItem.id), reallyCoolArgs.raw.asRoot, dataItem.typeName)

        SimpleResolveOutput(node, Args.empty)
      })
  }
}
