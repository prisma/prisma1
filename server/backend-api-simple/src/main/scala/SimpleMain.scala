import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.akkautil.http.ServerExecutor
import cool.graph.bugsnag.BugSnagger
import cool.graph.client.database.{DeferredResolverProvider, SimpleManyModelDeferredResolver, SimpleToManyDeferredResolver}
import cool.graph.client.schema.simple.SimpleSchemaBuilder
import cool.graph.client.server.{ClientServer, GraphQlRequestHandlerImpl, ProjectSchemaBuilder}
import cool.graph.client.{ClientInjector, ClientInjectorImpl, FeatureMetric, UserContext}

object SimpleMain extends App {
  implicit val system: ActorSystem             = ActorSystem("sangria-server")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val injector: ClientInjector        = new ClientInjectorImpl()
  implicit val bugsnagger: BugSnagger          = injector.bugsnagger
  import system.dispatcher

  val deferredResolver: DeferredResolverProvider[_, UserContext] =
    new DeferredResolverProvider(new SimpleToManyDeferredResolver, new SimpleManyModelDeferredResolver)

  implicit val projectSchemaBuilder = ProjectSchemaBuilder(project => new SimpleSchemaBuilder(project).build())

  implicit val graphQlRequestHandler = GraphQlRequestHandlerImpl(
    errorHandlerFactory = injector.errorHandlerFactory,
    log = injector.log,
    apiVersionMetric = FeatureMetric.ApiSimple,
    apiMetricsMiddleware = injector.apiMetricsMiddleware,
    deferredResolver = deferredResolver
  )

  ServerExecutor(port = 8080, ClientServer("simple")).startBlocking()
}
