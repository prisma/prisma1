package cool.graph.schemamanager

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import cool.graph.bugsnag.{BugSnagger, BugSnaggerImpl}
import cool.graph.cloudwatch.CloudwatchImpl
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

  SystemMetrics.init()

  binding identifiedBy "cloudwatch" toNonLazy CloudwatchImpl()
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

  lazy val internalDb                                   = Database.forConfig("internal", config)
  lazy val uncachedProjectResolver                      = UncachedProjectResolver(internalDb)
  lazy val cachedProjectResolver: CachedProjectResolver = CachedProjectResolverImpl(uncachedProjectResolver)
  lazy val requestPrefix                                = sys.env.getOrElse("AWS_REGION", sys.error("AWS Region not found."))

  bind[String] identifiedBy "request-prefix" toNonLazy requestPrefix

  binding identifiedBy "internal-db" toNonLazy internalDb
  binding identifiedBy "cachedProjectResolver" toNonLazy cachedProjectResolver
  binding identifiedBy "uncachedProjectResolver" toNonLazy uncachedProjectResolver
}
