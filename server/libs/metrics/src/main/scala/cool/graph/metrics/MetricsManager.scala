package cool.graph.metrics

import akka.actor.ActorSystem
import com.timgroup.statsd.{NonBlockingStatsDClient, StatsDClient}
import cool.graph.akkautil.SingleThreadedActorSystem

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

/**
  * Metrics management, should be inherited and instantiated _once per logical service_.
  *
  * The metrics structure reported from this code to the statsd backend is as follows:
  *
  * {service_name}.{metric_name}#env={env}container={container_id},instance={instance_id}[,{custom_tag}={custom_value}]
  *
  * - The basic metrics name that goes to statsd is simply the logical service name plus the metric name, e.g. "ApiSimpleService.OpenSessions"
  * - After that, there is a series of tags:
  *   - The env var "METRICS_PREFIX" is used to denote the env the service is running in, e.g. 'dev' or 'prod'.
  *   - The EC2 instance this code is run from. Fetched from EC2
  *   - The container ID this code is run from. Fetched from /etc/hostname, as it is identical to the container ID in ECS.
  *   - Custom metric tags. These should be used sparsely and only if it delivers crucial insights, such as per-project distinctions.
  *
  * The final metric that arrives at Statsd looks for example like this:
  * "ApiSimpleService.RequestCount#env=prod,instance=i-0d3c23cdd0c2f5d03,container=e065fc831976,projectId=someCUID
  */
trait MetricsManager {

  def serviceName: String

  // System used to periodically flush the state of individual gauges
  implicit val gaugeFlushSystem: ActorSystem = SingleThreadedActorSystem(s"$serviceName-gauges")

  val errorHandler = CustomErrorHandler()

  protected val baseTagsString: String = {
    if (sys.env.isDefinedAt("METRICS_PREFIX")) {
      Try {
        val instanceID  = Await.result(InstanceMetadata.fetchInstanceId(), 5.seconds)
        val containerId = ContainerMetadata.fetchContainerId()
        val region      = sys.env.getOrElse("AWS_REGION", "no_region")
        val env         = sys.env.getOrElse("METRICS_PREFIX", "local")

        s"env=$env,region=$region,instance=$instanceID,container=$containerId"
      } match {
        case Success(baseTags) => baseTags
        case Failure(err)      => errorHandler.handle(new Exception(err)); ""
      }
    } else {
      ""
    }
  }

  protected val client: StatsDClient = {
    // As we don't have an 'env' ENV var (prod, dev) this variable suppresses failing metrics output locally / during testing
    if (sys.env.isDefinedAt("METRICS_PREFIX")) {
      new NonBlockingStatsDClient("", Integer.MAX_VALUE, new Array[String](0), errorHandler, StatsdHostLookup())
    } else {
      println("[Metrics] Warning, Metrics can't initialize - no metrics will be recorded.")
      DummyStatsDClient()
    }
  }

  // Gauges DO NOT support custom metric tags per occurrence, only hardcoded custom tags during definition!
  def defineGauge(name: String, predefTags: (CustomTag, String)*): GaugeMetric = GaugeMetric(s"$serviceName.$name", baseTagsString, predefTags, client)
  def defineCounter(name: String, customTags: CustomTag*): CounterMetric       = CounterMetric(s"$serviceName.$name", baseTagsString, customTags, client)
  def defineTimer(name: String, customTags: CustomTag*): TimerMetric           = TimerMetric(s"$serviceName.$name", baseTagsString, customTags, client)

  def shutdown: Unit = Await.result(gaugeFlushSystem.terminate(), 10.seconds)
}
