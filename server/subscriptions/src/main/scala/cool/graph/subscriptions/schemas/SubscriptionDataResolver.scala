package cool.graph.subscriptions.schemas

import cool.graph.FilteredResolver
import cool.graph.api.schema.{ApiUserContext, SimpleResolveOutput}
import cool.graph.client.schema.SchemaModelObjectTypesBuilder
import cool.graph.client.schema.simple.SimpleResolveOutput
import cool.graph.shared.models.Model
import cool.graph.subscriptions.SubscriptionUserContext
import sangria.schema.{Args, Context}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object SubscriptionDataResolver {

  def resolve[ManyDataItemType](modelObjectTypes: SchemaModelObjectTypesBuilder[ManyDataItemType],
                                model: Model,
                                ctx: Context[ApiUserContext, Unit]): Future[Option[SimpleResolveOutput]] = {
    FilteredResolver
      .resolve(modelObjectTypes, model, ctx.ctx.nodeId, ctx, ctx.ctx.dataResolver)
      .map(_.map(dataItem => SimpleResolveOutput(dataItem, Args.empty)))
  }
}
