package cool.graph.deprecated.actions

import com.typesafe.scalalogging.LazyLogging
import cool.graph.client.database.{DeferredResolverProvider, SimpleManyModelDeferredResolver, SimpleToManyDeferredResolver}
import cool.graph.cuid.Cuid.createCuid
import cool.graph.deprecated.actions.schemas.{ActionUserContext, MutationMetaData}
import cool.graph.shared.models.{Model, Project}
import cool.graph.shared.schema.JsonMarshalling._
import sangria.execution.Executor
import sangria.parser.QueryParser
import sangria.schema.Schema
import scaldi.{Injectable, Injector}
import spray.json.{JsObject, JsString}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

case class Event(id: String, url: String, payload: Option[JsObject])

class MutationCallbackSchemaExecutor(project: Project,
                                     model: Model,
                                     schema: Schema[ActionUserContext, Unit],
                                     nodeId: String,
                                     fragment: String,
                                     url: String,
                                     mutationId: String)(implicit inj: Injector)
    extends Injectable
    with LazyLogging {
  def execute: Future[Event] = {
    val dataFut = QueryParser.parse(fragment) match {
      case Success(queryAst) =>
        Executor.execute(
          schema,
          queryAst,
          deferredResolver = new DeferredResolverProvider(
            new SimpleToManyDeferredResolver,
            new SimpleManyModelDeferredResolver,
            skipPermissionCheck = true
          ),
          userContext = ActionUserContext(
            requestId = "",
            project = project,
            nodeId = nodeId,
            mutation = MutationMetaData(id = mutationId, _type = "Create"),
            log = (x: String) => logger.info(x)
          )
        )
      case Failure(error) =>
        Future.successful(JsObject("error" -> JsString(error.getMessage)))
    }

    dataFut
      .map {
        case JsObject(dataMap) =>
          Event(id = createCuid(), url = url, payload = Some(dataMap("data").asJsObject))
        case json =>
          sys.error(s"Must only receive JsObjects here. But got instead: ${json.compactPrint}")
      }

  }
}
