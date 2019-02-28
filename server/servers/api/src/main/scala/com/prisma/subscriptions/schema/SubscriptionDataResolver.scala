package com.prisma.subscriptions.schema

import com.prisma.api.connector.{DataResolver, PrismaNode}
import com.prisma.api.schema.ObjectTypeBuilder
import com.prisma.shared.models.Model
import com.prisma.subscriptions.SubscriptionUserContext
import com.prisma.subscriptions.resolving.FilteredResolver
import sangria.schema.Context

import scala.concurrent.Future

object SubscriptionDataResolver {

  def resolve(dataResolver: DataResolver,
              modelObjectTypes: ObjectTypeBuilder,
              model: Model,
              ctx: Context[SubscriptionUserContext, Unit]): Future[Option[PrismaNode]] = {
    FilteredResolver.resolve(modelObjectTypes, model, ctx.ctx.nodeId, ctx, dataResolver)
  }
}
