package cool.graph.singleserver

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.akkautil.http.ServerExecutor
import cool.graph.bugsnag.BugSnagger
import cool.graph.client.database._
import cool.graph.client.schema.simple.SimpleSchemaBuilder
import cool.graph.client.server.{ClientServer, GraphQlRequestHandler, GraphQlRequestHandlerImpl, ProjectSchemaBuilder}
import cool.graph.client.{FeatureMetric, UserContext}
import cool.graph.relay.schema.RelaySchemaBuilder
import cool.graph.schemamanager.SchemaManagerServer
import cool.graph.subscriptions.SimpleSubscriptionsServer
import cool.graph.system.SystemServer
import cool.graph.websockets.WebsocketServer
import cool.graph.worker.WorkerServer
import scaldi.{Injectable, Module}

object SingleServerMain extends App with Injectable {
  implicit val system: ActorSystem             = ActorSystem("single-server")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  implicit val injector: SingleServerInjectorImpl = new SingleServerInjectorImpl()
  implicit val bugsnagger: BugSnagger             = injector.bugsnagger
  implicit val inj: Module                        = injector.toScaldi

  val workerServices    = injector.workerServices
  val websocketServices = injector.websocketServices
  val port              = sys.env.getOrElse("PORT", sys.error("PORT env var required but not found.")).toInt

  Version.check()

  //set up additional simple dependencies
  import system.dispatcher

  val simpleDeferredResolver: DeferredResolverProvider[_, UserContext] =
    new DeferredResolverProvider(new SimpleToManyDeferredResolver, new SimpleManyModelDeferredResolver)

  val simpleProjectSchemaBuilder: ProjectSchemaBuilder = ProjectSchemaBuilder(project => new SimpleSchemaBuilder(project).build())

  implicit val simpleGraphQlRequestHandler: GraphQlRequestHandler = GraphQlRequestHandlerImpl(
    errorHandlerFactory = injector.errorHandlerFactory,
    log = injector.log,
    apiVersionMetric = FeatureMetric.ApiSimple,
    apiMetricsMiddleware = injector.apiMetricsMiddleware,
    deferredResolver = simpleDeferredResolver
  )

  //set up additional relay dependencies
  val relayDeferredResolver: DeferredResolverProvider[_, UserContext] =
    new DeferredResolverProvider(new RelayToManyDeferredResolver, new RelayManyModelDeferredResolver)

  val relayProjectSchemaBuilder = ProjectSchemaBuilder(project => new RelaySchemaBuilder(project).build())

  val relayGraphQlRequestHandler = GraphQlRequestHandlerImpl(
    errorHandlerFactory = injector.errorHandlerFactory,
    log = injector.log,
    apiVersionMetric = FeatureMetric.ApiRelay,
    apiMetricsMiddleware = injector.apiMetricsMiddleware,
    deferredResolver = relayDeferredResolver
  )

  ServerExecutor(
    port = port,
    SystemServer(injector.schemaBuilder, "system"),
    SchemaManagerServer("schema-manager"),
    ClientServer("simple")(system, materializer, injector, bugsnagger, simpleProjectSchemaBuilder, simpleGraphQlRequestHandler),
    ClientServer("relay")(system, materializer, injector, bugsnagger, relayProjectSchemaBuilder, relayGraphQlRequestHandler),
    WebsocketServer(websocketServices, "subscriptions"),
    SimpleSubscriptionsServer(),
    WorkerServer(workerServices)
  ).startBlocking()
}
