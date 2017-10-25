package cool.graph.client

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import com.amazonaws.services.cloudwatch.model._
import cool.graph.cloudwatch.CloudwatchMetric
import cool.graph.cuid.Cuid
import cool.graph.shared.errors.UserFacingError
import cool.graph.shared.externalServices.KinesisPublisher
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import scaldi.Injector
import spray.json.{JsArray, JsBoolean, JsNumber, JsObject, JsString}

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration
import scala.util.control.NonFatal

object FeatureMetric extends Enumeration {
  type FeatureMetric = Value
  val Subscriptions           = Value("backend/api/subscriptions")
  val Filter                  = Value("backend/feature/filter")
  val NestedMutations         = Value("backend/feature/nested-mutation")
  val ApiSimple               = Value("backend/api/simple")
  val ApiRelay                = Value("backend/api/relay")
  val ApiFiles                = Value("backend/api/files")
  val ServersideSubscriptions = Value("backend/feature/sss")
  val RequestPipeline         = Value("backend/feature/rp") // add this!
  val PermissionQuery         = Value("backend/feature/permission-queries") // add this!
  val Authentication          = Value("backend/feature/authentication")
  val Algolia                 = Value("backend/feature/algolia") // add this!
  val Auth0                   = Value("backend/feature/integration-auth0")
  val Digits                  = Value("backend/feature/integration-digits")
}

case class ApiFeatureMetric(ip: String,
                            date: DateTime,
                            projectId: String,
                            clientId: String,
                            usedFeatures: List[String],
                            // Should be false when we can't determine. This is the case for subscriptions.
                            // Is always false for File api.
                            isFromConsole: Boolean)

class FeatureMetricActor(
    metricsPublisher: KinesisPublisher,
    interval: Int
) extends Actor {
  import context.dispatcher

  val metrics = mutable.Buffer.empty[ApiFeatureMetric]
  val FLUSH   = "FLUSH"
  val tick = context.system.scheduler.schedule(
    initialDelay = FiniteDuration(interval, TimeUnit.SECONDS),
    interval = FiniteDuration(interval, TimeUnit.SECONDS),
    receiver = self,
    message = FLUSH
  )

  override def postStop() = tick.cancel()

  def receive = {
    case metric: ApiFeatureMetric =>
      metrics += metric

    case FLUSH =>
      flushMetrics()
  }

  def flushMetrics() = {
    val byProject = metrics.groupBy(_.projectId) map {
      case (projectId, metrics) =>
        JsObject(
          "requestCount"        -> JsNumber(metrics.length),
          "projectId"           -> JsString(projectId),
          "usedIps"             -> JsArray(metrics.map(_.ip).distinct.take(10).toVector.map(JsString(_))),
          "features"            -> JsArray(metrics.flatMap(_.usedFeatures).distinct.toVector.map(JsString(_))),
          "date"                -> JsString(metrics.head.date.toString(DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z").withZoneUTC())),
          "version"             -> JsString("1"),
          "justConsoleRequests" -> JsBoolean(metrics.forall(_.isFromConsole))
        )
    }

    byProject.foreach { json =>
      try {
        metricsPublisher.putRecord(json.toString, shardId = Cuid.createCuid())
      } catch {
        case NonFatal(e) => println(s"Putting kinesis FeatureMetric failed: ${e.getMessage} ${e.toString}")
      }
    }
    metrics.clear()
  }
}
case class SqlQueryMetric(value: Double, dimensionValue: String) extends CloudwatchMetric() {
  override val name: String       = "Duration"
  override val namespacePostfix   = "SqlQueries"
  override val unit: StandardUnit = StandardUnit.Milliseconds
  override val dimensionName      = "By Query Name"
}

case class MutactionMetric(value: Double, dimensionValue: String) extends CloudwatchMetric() {
  override val name: String       = "Duration"
  override val namespacePostfix   = "Mutactions"
  override val unit: StandardUnit = StandardUnit.Milliseconds
  override val dimensionName      = "By Mutaction Name"
}

case class HandledError(error: UserFacingError) extends CloudwatchMetric() {
  override val name: String       = "Count"
  override val namespacePostfix   = "HandledError"
  override val unit: StandardUnit = StandardUnit.Count
  override val dimensionName      = "By Error"
  override val dimensionValue =
    s"${error.code} - ${error.getClass.getSimpleName}"
  override val value = 1.0
}

case class UnhandledError(error: Throwable) extends CloudwatchMetric() {
  override val name: String       = "Count"
  override val namespacePostfix   = "UnhandledError"
  override val unit: StandardUnit = StandardUnit.Count
  override val dimensionName      = "By Error"
  override val dimensionValue =
    s"${error.getClass.getSimpleName}"
  override val value = 1.0
}
