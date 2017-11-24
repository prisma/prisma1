import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.akkautil.http.ServerExecutor
import cool.graph.bugsnag.BugSnagger
import cool.graph.client.database._
import cool.graph.client.server.{ClientServer, GraphQlRequestHandlerImpl, ProjectSchemaBuilder}
import cool.graph.client.{ClientInjectorImpl, FeatureMetric, UserContext}
import cool.graph.relay.schema.RelaySchemaBuilder

object RelayMain extends App {
  implicit val system: ActorSystem             = ActorSystem("sangria-server")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val injector                        = new ClientInjectorImpl
  implicit val bugsnagger: BugSnagger          = injector.bugsnagger
  import system.dispatcher

  val deferredResolver: DeferredResolverProvider[_, UserContext] =
    new DeferredResolverProvider(new RelayToManyDeferredResolver, new RelayManyModelDeferredResolver)

  implicit val projectSchemaBuilder = ProjectSchemaBuilder(project => new RelaySchemaBuilder(project).build())

  implicit val graphQlRequestHandler = GraphQlRequestHandlerImpl(
    errorHandlerFactory = injector.errorHandlerFactory,
    log = injector.log,
    apiVersionMetric = FeatureMetric.ApiRelay,
    apiMetricsMiddleware = injector.apiMetricsMiddleware,
    deferredResolver = deferredResolver
  )

  ServerExecutor(port = 8083, ClientServer("relay")).startBlocking()
}
