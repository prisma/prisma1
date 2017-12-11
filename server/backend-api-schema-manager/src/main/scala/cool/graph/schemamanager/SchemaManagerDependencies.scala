package cool.graph.schemamanager

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import cool.graph.bugsnag.{BugSnagger, BugSnaggerImpl}
import cool.graph.messagebus.pubsub.rabbit.RabbitAkkaPubSub
import cool.graph.messagebus.{Conversions, PubSubSubscriber}
import cool.graph.system.database.finder._
import cool.graph.system.metrics.SystemMetrics
import scaldi.Module
import slick.jdbc.MySQLProfile
import slick.jdbc.MySQLProfile.api._

trait SchemaManagerApiDependencies extends Module {
  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer

  lazy val config = ConfigFactory.load()

  val internalDb: MySQLProfile.backend.DatabaseDef
  val uncachedProjectResolver: UncachedProjectResolver
  val cachedProjectResolver: CachedProjectResolver
  val requestPrefix: String
  val schemaInvalidationSubscriber: PubSubSubscriber[String]

  binding identifiedBy "config" toNonLazy config
  binding identifiedBy "environment" toNonLazy sys.env.getOrElse("ENVIRONMENT", "local")
  binding identifiedBy "service-name" toNonLazy sys.env.getOrElse("SERVICE_NAME", "local")
  binding identifiedBy "actorSystem" toNonLazy system destroyWith (_.terminate())
  binding identifiedBy "dispatcher" toNonLazy system.dispatcher
  binding identifiedBy "actorMaterializer" toNonLazy materializer

  bind[BugSnagger] toNonLazy BugSnaggerImpl(sys.env("BUGSNAG_API_KEY"))
}

case class SchemaManagerDependencies()(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends SchemaManagerApiDependencies {
  import system.dispatcher

  SystemMetrics.init()

  lazy val internalDb                                   = Database.forConfig("internal", config)
  lazy val uncachedProjectResolver                      = UncachedProjectResolver(internalDb)
  lazy val cachedProjectResolver: CachedProjectResolver = CachedProjectResolverImpl(uncachedProjectResolver)
  lazy val requestPrefix                                = sys.env.getOrElse("AWS_REGION", sys.error("AWS Region not found."))
  lazy val globalRabbitUri                              = sys.env("GLOBAL_RABBIT_URI")

  implicit val bugsnagger   = BugSnaggerImpl(sys.env.getOrElse("BUGSNAG_API_KEY", ""))
  implicit val unmarshaller = Conversions.Unmarshallers.ToString

  lazy val schemaInvalidationSubscriber: PubSubSubscriber[String] = RabbitAkkaPubSub.subscriber[String](
    globalRabbitUri,
    "project-schema-invalidation",
    durable = true
  )

  bind[String] identifiedBy "request-prefix" toNonLazy requestPrefix
  bind[PubSubSubscriber[String]] identifiedBy "schema-manager-invalidation-subscriber" toNonLazy schemaInvalidationSubscriber

  binding identifiedBy "internal-db" toNonLazy internalDb
  binding identifiedBy "cachedProjectResolver" toNonLazy cachedProjectResolver
  binding identifiedBy "uncachedProjectResolver" toNonLazy uncachedProjectResolver
}
