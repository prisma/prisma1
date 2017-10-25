package cool.graph.system.schema.types

import com.typesafe.scalalogging.LazyLogging
import cool.graph.client.schema.simple.SimpleSchemaModelObjectTypeBuilder
import cool.graph.shared.algolia.schemas.AlgoliaSchema
import cool.graph.shared.algolia.AlgoliaContext
import cool.graph.shared.models
import cool.graph.system.SystemUserContext
import cool.graph.system.schema.types.AlgoliaSyncQuery.AlgoliaSyncQueryContext
import sangria.execution.Executor
import sangria.introspection.introspectionQuery
import sangria.marshalling.sprayJson._
import sangria.relay.{Connection, ConnectionArgs, Node}
import sangria.schema._
import scaldi.{Injectable, Injector}
import spray.json.JsObject

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object SearchProviderAlgolia {
  case class SearchProviderAlgoliaContext(project: models.Project, algolia: models.SearchProviderAlgolia) extends Node with models.Integration {
    override val id              = algolia.id
    override val subTableId      = algolia.subTableId
    override val isEnabled       = algolia.isEnabled
    override val name            = algolia.name
    override val integrationType = algolia.integrationType
  }
  lazy val Type: ObjectType[SystemUserContext, SearchProviderAlgoliaContext] =
    ObjectType(
      "SearchProviderAlgolia",
      "This is a SearchProviderAlgolia",
      interfaces[SystemUserContext, SearchProviderAlgoliaContext](nodeInterface, Integration.Type),
      () =>
        idField[SystemUserContext, SearchProviderAlgoliaContext] ::
          fields[SystemUserContext, SearchProviderAlgoliaContext](
          Field("applicationId", StringType, resolve = _.value.algolia.applicationId),
          Field("apiKey", StringType, resolve = _.value.algolia.apiKey),
          Field(
            "algoliaSyncQueries",
            algoliaSyncQueryConnection,
            arguments = Connection.Args.All,
            resolve = ctx =>
              Connection.connectionFromSeq(ctx.value.algolia.algoliaSyncQueries
                                             .sortBy(_.id.toString)
                                             .map(s => AlgoliaSyncQueryContext(ctx.value.project, s)),
                                           ConnectionArgs(ctx))
          ),
          Field(
            "algoliaSchema",
            StringType,
            arguments = List(Argument("modelId", IDType)),
            resolve = ctx => {
              val modelId =
                ctx.args.raw.get("modelId").get.asInstanceOf[String]
              ctx.ctx.getSearchProviderAlgoliaSchema(ctx.value.project, modelId)
            }
          )
      )
    )
}

class SearchProviderAlgoliaSchemaResolver(implicit inj: Injector) extends Injectable with LazyLogging {
  def resolve(project: models.Project, modelId: String): Future[String] = {
    val model = project.getModelById_!(modelId)
    Executor
      .execute(
        schema = new AlgoliaSchema(
          project = project,
          model = model,
          modelObjectTypes = new SimpleSchemaModelObjectTypeBuilder(project)
        ).build(),
        queryAst = introspectionQuery,
        userContext = AlgoliaContext(
          project = project,
          requestId = "",
          nodeId = "",
          log = (x: String) => logger.info(x)
        )
      )
      .map { response =>
        val JsObject(fields) = response
        fields("data").compactPrint
      }
  }
}
