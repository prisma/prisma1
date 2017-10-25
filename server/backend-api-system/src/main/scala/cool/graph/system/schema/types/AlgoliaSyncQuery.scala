package cool.graph.system.schema.types

import cool.graph.shared.models
import cool.graph.system.SystemUserContext
import cool.graph.system.schema.types.Model.ModelContext
import sangria.relay.Node
import sangria.schema._

object AlgoliaSyncQuery {
  case class AlgoliaSyncQueryContext(project: models.Project, algoliaSyncQuery: models.AlgoliaSyncQuery) extends Node {
    def id = algoliaSyncQuery.id
  }
  lazy val Type: ObjectType[SystemUserContext, AlgoliaSyncQueryContext] =
    ObjectType(
      "AlgoliaSyncQuery",
      "This is an AlgoliaSyncQuery",
      interfaces[SystemUserContext, AlgoliaSyncQueryContext](nodeInterface),
      idField[SystemUserContext, AlgoliaSyncQueryContext] ::
        fields[SystemUserContext, AlgoliaSyncQueryContext](
        Field("indexName", StringType, resolve = _.value.algoliaSyncQuery.indexName),
        Field("fragment", StringType, resolve = _.value.algoliaSyncQuery.fragment),
        Field("isEnabled", BooleanType, resolve = _.value.algoliaSyncQuery.isEnabled),
        Field("model", ModelType, resolve = ctx => {
          val project = ctx.value.project
          val model   = ctx.value.algoliaSyncQuery.model

          ModelContext(project, model)
        })
      )
    )
}
