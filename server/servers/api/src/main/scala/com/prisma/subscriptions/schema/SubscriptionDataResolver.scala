package com.prisma.subscriptions.schema

import com.prisma.api.connector.DataResolver
import com.prisma.api.schema.{ObjectTypeBuilder, SimpleResolveOutput}
import com.prisma.shared.models.Model
import com.prisma.subscriptions.SubscriptionUserContext
import com.prisma.subscriptions.resolving.FilteredResolver
import sangria.schema.{Args, Context}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object SubscriptionDataResolver {

  def resolve(dataResolver: DataResolver,
              modelObjectTypes: ObjectTypeBuilder,
              model: Model,
              ctx: Context[SubscriptionUserContext, Unit]): Future[Option[SimpleResolveOutput]] = {
    FilteredResolver.resolve(modelObjectTypes, model, ctx.ctx.nodeId, ctx, dataResolver).map(_.map(SimpleResolveOutput(_, Args.empty)))
  }
}
