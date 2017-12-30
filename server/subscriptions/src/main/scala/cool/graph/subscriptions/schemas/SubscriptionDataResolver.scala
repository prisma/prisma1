package cool.graph.subscriptions.schemas

import cool.graph.api.database.DataResolver
import cool.graph.api.schema.{ApiUserContext, ObjectTypeBuilder, SimpleResolveOutput}
import cool.graph.shared.models.Model
import cool.graph.subscriptions.resolving.{FilteredResolver, SubscriptionUserContext}
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
