import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.Multipart.FormData
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{HttpOrigin, HttpOriginRange, Origin, RawHeader, _}
import akka.http.scaladsl.server.directives.FileInfo
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, Sink, Source}
import akka.stream.{ActorMaterializer, FlowShape}
import akka.util.ByteString
import com.amazonaws.services.kinesis.AmazonKinesis
import com.typesafe.scalalogging.LazyLogging
import cool.graph.Types._
import cool.graph.bugsnag.{BugSnagger, GraphCoolRequest}
import cool.graph.client._
import cool.graph.client.authorization.{ClientAuth, ClientAuthImpl}
import cool.graph.client.database.DatabaseMutationBuilder
import cool.graph.client.files.{FileUploadResponse, FileUploader}
import cool.graph.client.finder.ProjectFetcher
import cool.graph.client.server.HealthChecks
import cool.graph.cuid.Cuid
import cool.graph.fileupload.FileuploadServices
import cool.graph.metrics.ClientSharedMetrics
import cool.graph.shared.database.GlobalDatabaseManager
import cool.graph.shared.errors.UserAPIErrors
import cool.graph.shared.externalServices.TestableTime
import cool.graph.shared.logging.RequestLogger
import cool.graph.shared.models.{AuthenticatedRequest, Project, ProjectWithClientId}
import cool.graph.util.ErrorHandlerFactory
import scaldi.akka.AkkaInjectable
import spray.json.{JsNumber, JsObject, JsString, JsValue}
import scala.collection.immutable._
import scala.concurrent.Future
import scala.concurrent.duration._

object Server extends App with AkkaInjectable with LazyLogging {
  ClientSharedMetrics // this is just here to kick off the profiler

  implicit val system       = ActorSystem("sangria-server")
  implicit val materializer = ActorMaterializer()
  implicit val inj          = new FileuploadServices

  import system.dispatcher

  val globalDatabaseManager    = inject[GlobalDatabaseManager]
  val kinesis                  = inject[AmazonKinesis](identified by "kinesis")
  val log: String => Unit      = (msg: String) => logger.info(msg)
  val errorHandlerFactory      = ErrorHandlerFactory(log)
  val projectSchemaFetcher     = inject[ProjectFetcher](identified by "project-schema-fetcher")
  val globalApiEndpointManager = inject[GlobalApiEndpointManager]
  val bugsnagger               = inject[BugSnagger]
  val auth                     = inject[ClientAuth]
  val apiMetricActor           = inject[ActorRef](identified by "featureMetricActor")
  val testableTime             = inject[TestableTime]

  val requestHandler: Flow[HttpRequest, HttpResponse, NotUsed] = {

    case class RequestAndSchema(request: HttpRequest, project: Project, clientId: Id, clientOrUserId: Option[AuthenticatedRequest])

    Flow
      .fromGraph(GraphDSL.create() { implicit b =>
        import akka.http.scaladsl.unmarshalling.Unmarshal
        import akka.stream.scaladsl.GraphDSL.Implicits._

        val src           = b.add(Flow[HttpRequest])
        val statusSplit   = b.add(Broadcast[HttpRequest](3))
        val optionsFilter = b.add(Flow[HttpRequest].filter(x => x.method == HttpMethods.OPTIONS))
        val statusFilter  = b.add(Flow[HttpRequest].filter(x => x.method == HttpMethods.GET))
        val dataFilter    = b.add(Flow[HttpRequest].filter(x => x.method == HttpMethods.POST))
        val status        = Flow[HttpRequest].mapAsync(5)(_ => statusHandler.map(_ => HttpResponse(status = StatusCodes.Accepted, entity = "OK")))

        val options = Flow[HttpRequest].map(
          request =>
            HttpResponse(
              status = StatusCodes.Accepted,
              entity = "OK",
              headers = request
                .header[Origin]
                .map(_.origins)
                .map(
                  origins =>
                    corsHeaders(request
                                  .header[`Access-Control-Request-Headers`]
                                  .map(_.headers)
                                  .getOrElse(Seq.empty),
                                origins))
                .getOrElse(Seq())
          ))

        val withSchema = b.add(Flow[HttpRequest].mapAsync(5)(request => {
          val projectId           = request.uri.path.toString().split("/").reverse.head
          val authorizationHeader = request.headers.find(_.name() == "Authorization").map(_.value())

          getAuthContext(projectId, authorizationHeader).map(s => {
            RequestAndSchema(request, s._1, s._2, s._3)
          })
        }))

        val split       = b.add(Broadcast[RequestAndSchema](2))
        val proxyFilter = b.add(Flow[RequestAndSchema].filter(x => x.project.region != globalDatabaseManager.currentRegion))
        val localFilter = b.add(Flow[RequestAndSchema].filter(x => x.project.region == globalDatabaseManager.currentRegion))
        val merge       = b.add(Merge[HttpResponse](4))

        val proxy: Flow[RequestAndSchema, HttpResponse, NotUsed] = Flow[RequestAndSchema].mapAsync(5)(r => {
          println("PROXY")

          val host = Uri(globalApiEndpointManager.getEndpointForProject(r.project.region, r.project.id)).authority.host.address()
          Http(system)
            .outgoingConnection(host, 80)
            .runWith(
              Source.single(
                r.request.copy(headers = r.request.headers.filter(header => !List("remote-address", "timeout-access").contains(header.name.toLowerCase)))),
              Sink.head
            )
            ._2
        })

        val local = Flow[RequestAndSchema].mapAsyncUnordered(5)(x => {
          println("LOCAL")

          val requestLogger = new RequestLogger(requestIdPrefix = sys.env.getOrElse("AWS_REGION", sys.error("AWS Region not found.")) + ":file", log = log)
          val requestId     = requestLogger.begin

          Unmarshal(x.request.entity)
            .to[Multipart.FormData]
            .flatMap { formData =>
              val onePartSource: Future[List[FormData.BodyPart]] =
                formData
                  .toStrict(600.seconds)
                  .flatMap(g => g.parts.runFold(List[FormData.BodyPart]())((acc, body) => acc :+ body))

              onePartSource.map { list =>
                list
                  .find(part => part.filename.isDefined && part.name == "data")
                  .map(part ⇒ (FileInfo(part.name, part.filename.get, part.entity.contentType), part.entity.dataBytes))
              }
            }
            .flatMap { dataOpt =>
              val (fileInfo, byteSource) = dataOpt.get
              fileHandler(
                metadata = fileInfo,
                byteSource = byteSource,
                project = x.project,
                clientId = x.clientId,
                authenticatedRequest = x.clientOrUserId,
                requestId = requestId,
                requestIp = "ip.toString"
              ).andThen {
                case _ =>
                  requestLogger.end(Some(x.project.id), Some(x.clientId))
              }
            }
            .map {
              case (_, json) =>
                HttpResponse(
                  entity = json.prettyPrint,
                  headers = x.request
                    .header[Origin]
                    .map(_.origins)
                    .map(
                      origins =>
                        corsHeaders(x.request
                                      .header[`Access-Control-Request-Headers`]
                                      .map(_.headers)
                                      .getOrElse(Seq.empty),
                                    origins))
                    .getOrElse(Seq()) :+ RawHeader("Request-Id", requestId)
                )
            }

        })

        src ~> statusSplit
        statusSplit ~> statusFilter ~> status ~> merge
        statusSplit ~> optionsFilter ~> options ~> merge
        statusSplit ~> dataFilter ~> withSchema ~> split

        split ~> proxyFilter ~> proxy ~> merge
        split ~> localFilter ~> local ~> merge

        FlowShape(src.in, merge.out)
      })

  }

  Http().bindAndHandle(requestHandler, "0.0.0.0", 8084).onSuccess {
    case _ => logger.info("Server running on: 8084")
  }

  def accessControlAllowOrigin(origins: Seq[HttpOrigin]): `Access-Control-Allow-Origin` =
    `Access-Control-Allow-Origin`.forRange(HttpOriginRange.Default(origins))

  def accessControlAllowHeaders(requestHeaders: Seq[String]): Option[`Access-Control-Allow-Headers`] =
    if (requestHeaders.isEmpty) { None } else { Some(`Access-Control-Allow-Headers`(requestHeaders)) }

  def accessControlAllowMethods = `Access-Control-Allow-Methods`(HttpMethods.GET, HttpMethods.POST, HttpMethods.OPTIONS)

  def corsHeaders(requestHeaders: Seq[String], origins: Seq[HttpOrigin]): Seq[HttpHeader] =
    Seq(accessControlAllowOrigin(origins), accessControlAllowMethods) ++ accessControlAllowHeaders(requestHeaders)

  def fileHandler(metadata: FileInfo,
                  byteSource: Source[ByteString, Any],
                  project: Project,
                  clientId: String,
                  authenticatedRequest: Option[AuthenticatedRequest],
                  requestId: String,
                  requestIp: String): Future[(StatusCode with Product with Serializable, JsValue)] = {
    apiMetricActor ! ApiFeatureMetric(requestIp, testableTime.DateTime, project.id, clientId, List(FeatureMetric.ApiFiles.toString), isFromConsole = false)

    val uploader     = new FileUploader(project)
    val uploadResult = uploader.uploadFile(metadata, byteSource)

    createFileNode(project, uploadResult).map(
      id =>
        OK -> JsObject(
          "id"          -> JsString(id),
          "secret"      -> JsString(uploadResult.fileSecret),
          "url"         -> JsString(getUrl(project.id, uploadResult.fileSecret)),
          "name"        -> JsString(uploadResult.fileName),
          "contentType" -> JsString(uploadResult.contentType),
          "size"        -> JsNumber(uploadResult.size)
      ))
  }

  def getUrl(projectId: String, fileSecret: String) = s"https://files.graph.cool/$projectId/$fileSecret"

  def createFileNode(project: Project, uploadResponse: FileUploadResponse): Future[String] = {
    val id = Cuid.createCuid()

    val item = Map(
      "id"          -> id,
      "secret"      -> uploadResponse.fileSecret,
      "url"         -> getUrl(project.id, uploadResponse.fileSecret),
      "name"        -> uploadResponse.fileName,
      "contentType" -> uploadResponse.contentType,
      "size"        -> uploadResponse.size
    )

    val query = DatabaseMutationBuilder.createDataItem(project.id, "File", item)
    globalDatabaseManager.getDbForProject(project).master.run(query).map(_ => id)
  }

  protected def statusHandler: Future[Id] = {
    val status = for {
      _ <- HealthChecks.checkDatabases(globalDatabaseManager)
      _ <- Future(try { kinesis.listStreams() } catch {
            case _: com.amazonaws.services.kinesis.model.LimitExceededException => true
          })
    } yield ()

    status.map(_ => "OK")
  }

  protected def getAuthContext(projectId: String, authorizationHeader: Option[String]): Future[(Project, Id, Option[AuthenticatedRequest])] = {
    val sessionToken = authorizationHeader.flatMap {
      case str if str.startsWith("Bearer ") => Some(str.stripPrefix("Bearer "))
      case _                                => None
    }

    fetchSchema(projectId) flatMap {
      case ProjectWithClientId(project, clientId) =>
        sessionToken match {
          case None =>
            Future.successful(project, clientId, None)

          case Some(x) =>
            auth
              .authenticateRequest(x, project)
              .map(clientOrUserId => (project, clientId, Some(clientOrUserId)))
              .recover {
                case _ => (project, clientId, None) // the token is invalid, so don't include userId
              }
        }
    }
  }

  def fetchSchema(projectId: String): Future[ProjectWithClientId] = {
    val result = projectSchemaFetcher.fetch(projectIdOrAlias = projectId) map {
      case None         => throw UserAPIErrors.ProjectNotFound(projectId)
      case Some(schema) => schema
    }

    result.onFailure {
      case t =>
        val request = GraphCoolRequest(
          requestId = "",
          clientId = None,
          projectId = Some(projectId),
          query = "",
          variables = ""
        )
        bugsnagger.report(t, request)
    }

    result
  }
}
