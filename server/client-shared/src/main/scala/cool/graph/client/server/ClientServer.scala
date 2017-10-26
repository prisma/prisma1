package cool.graph.client.server

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers.Segment
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import com.amazonaws.services.kinesis.AmazonKinesis
import com.typesafe.scalalogging.LazyLogging
import cool.graph.bugsnag.{BugSnagger, GraphCoolRequest}
import cool.graph.client.authorization.ClientAuth
import cool.graph.client.finder.ProjectFetcher
import cool.graph.private_api.PrivateClientApi
import cool.graph.shared.errors.CommonErrors.TimeoutExceeded
import cool.graph.shared.errors.UserAPIErrors.ProjectNotFound
import cool.graph.shared.logging.RequestLogger
import cool.graph.util.ErrorHandlerFactory
import scaldi.{Injectable, Injector}
import spray.json.JsValue

import scala.concurrent.Future

case class ClientServer(prefix: String)(
    implicit system: ActorSystem,
    materializer: ActorMaterializer,
    inj: Injector,
    bugsnagger: BugSnagger
) extends cool.graph.akkautil.http.Server
    with Injectable
    with LazyLogging {
  import system.dispatcher

  val log                   = (x: String) => logger.info(x)
  val kinesis               = inject[AmazonKinesis](identified by "kinesis")
  val errorHandlerFactory   = ErrorHandlerFactory(log)
  val projectSchemaFetcher  = inject[ProjectFetcher](identified by "project-schema-fetcher")
  val graphQlRequestHandler = inject[GraphQlRequestHandler](identified by s"$prefix-gql-request-handler")
  val projectSchemaBuilder  = inject[ProjectSchemaBuilder](identified by s"$prefix-schema-builder")
  val clientAuth            = inject[ClientAuth]
  val requestPrefix         = inject[String](identified by "request-prefix")
  val requestIdPrefix       = s"$requestPrefix:$prefix"

  private val requestHandler = RequestHandler(errorHandlerFactory, projectSchemaFetcher, projectSchemaBuilder, graphQlRequestHandler, clientAuth, log)

  override def healthCheck: Future[_] =
    for {
      _ <- graphQlRequestHandler.healthCheck
      _ <- Future {
            try { kinesis.listStreams() } catch {
              case e: com.amazonaws.services.kinesis.model.LimitExceededException =>
                true
            }
          }
    } yield ()

  val innerRoutes: Route = extractRequest { _ =>
    val requestLogger = new RequestLogger(requestIdPrefix = requestIdPrefix, log = log)
    requestLogger.begin

    handleExceptions(toplevelExceptionHandler(requestLogger.requestId)) {
      PrivateClientApi().privateRoute ~ pathPrefix("v1") {
        pathPrefix(Segment) { projectId =>
          get {
            path("schema.json") {
              complete(requestHandler.handleIntrospectionQuery(projectId, requestLogger))
            } ~ {
              getFromResource("graphiql.html")
            }
          } ~
            post {
              path("permissions") {
                extractRawRequest(requestLogger) { rawRequest =>
                  complete(requestHandler.handleRawRequestForPermissionSchema(projectId = projectId, rawRequest = rawRequest))
                }
              } ~ {
                extractRawRequest(requestLogger) { rawRequest =>
                  timeoutHandler(requestId = rawRequest.id, projectId = projectId) {
                    complete(requestHandler.handleRawRequestForProjectSchema(projectId = projectId, rawRequest = rawRequest))
                  }
                }
              }
            }
        }
      }
    }
  }

  def extractRawRequest(requestLogger: RequestLogger)(fn: RawRequest => Route): Route = {
    optionalHeaderValueByName("Authorization") { authorizationHeader =>
      optionalHeaderValueByName("x-graphcool-source") { graphcoolSourceHeader =>
        entity(as[JsValue]) { requestJson =>
          extractClientIP { clientIp =>
            respondWithHeader(RawHeader("Request-Id", requestLogger.requestId)) {
              fn(
                RawRequest(
                  json = requestJson,
                  ip = clientIp.toString,
                  sourceHeader = graphcoolSourceHeader,
                  authorizationHeader = authorizationHeader,
                  logger = requestLogger
                )
              )
            }
          }
        }
      }
    }
  }

  def timeoutHandler(requestId: String, projectId: String): Directive0 = {
    withRequestTimeoutResponse { _ =>
      val unhandledErrorLogger = errorHandlerFactory.unhandledErrorHandler(
        requestId = requestId,
        projectId = Some(projectId)
      )
      val error         = TimeoutExceeded()
      val errorResponse = unhandledErrorLogger(error)
      HttpResponse(errorResponse._1, entity = errorResponse._2.prettyPrint)
    }
  }

  def toplevelExceptionHandler(requestId: String) = ExceptionHandler {
    case e: Throwable =>
      val request = GraphCoolRequest(
        requestId = requestId,
        clientId = None,
        projectId = None,
        query = "",
        variables = ""
      )

      if (!e.isInstanceOf[ProjectNotFound]) {
        bugsnagger.report(e, request)
      }

      errorHandlerFactory.akkaHttpHandler(requestId)(e)
  }
}
