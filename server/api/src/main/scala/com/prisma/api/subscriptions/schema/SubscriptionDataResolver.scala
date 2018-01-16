package cool.graph.api.subscriptions.schema

import cool.graph.api.database.DataResolver
import cool.graph.api.schema.{ObjectTypeBuilder, SimpleResolveOutput}
import cool.graph.api.subscriptions.SubscriptionUserContext
import cool.graph.api.subscriptions.resolving.FilteredResolver
import cool.graph.shared.models.Model
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
      .map(_.map(dataItem => SimpleResolveOutput(dataItem, Args.empty)))
  }
}
