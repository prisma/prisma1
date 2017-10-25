package cool.graph.cloudwatch

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorSystem, Props}
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.cloudwatch.{AmazonCloudWatchAsyncClient, AmazonCloudWatchAsyncClientBuilder}
import com.amazonaws.services.cloudwatch.model._

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

trait CloudwatchMetric {
  def name: String
  def namespacePostfix: String
  def unit: StandardUnit
  def value: Double
  def dimensionName: String
  def dimensionValue: String
}

case class CountMetric(name: String,
                       namespacePostfix: String,
                       intValue: Int,
                       dimensionName: String = "dummy dimension",
                       dimensionValue: String = "dummy dimension value")
    extends CloudwatchMetric {
  override val unit  = StandardUnit.Count
  override val value = intValue.toDouble
}

trait Cloudwatch {
  def measure(cloudwatchMetric: CloudwatchMetric): Unit
}

case class CloudwatchImpl()(implicit actorSystem: ActorSystem) extends Cloudwatch {
  val actor = actorSystem.actorOf(CloudwatchMetricActorImpl.props)

  def measure(cloudwatchMetric: CloudwatchMetric): Unit = {
    actor ! cloudwatchMetric
  }
}

object CloudwatchMock extends Cloudwatch {
  def measure(cloudwatchMetric: CloudwatchMetric): Unit = {
    //
  }
}

abstract class CloudwatchMetricActor extends Actor

object CloudwatchMetricActorImpl {
  def props = Props(new CloudwatchMetricActorImpl())
}

/**
  * Stores CloudWatch metrics for up to 60 seconds, then aggregates by dimension and service before pushing
  */
class CloudwatchMetricActorImpl extends CloudwatchMetricActor {

  val credentials =
    new BasicAWSCredentials(sys.env("AWS_ACCESS_KEY_ID"), sys.env("AWS_SECRET_ACCESS_KEY"))

  val cw = AmazonCloudWatchAsyncClientBuilder.standard
    .withCredentials(new AWSStaticCredentialsProvider(credentials))
    .withEndpointConfiguration(new EndpointConfiguration(sys.env("CLOUDWATCH_ENDPOINT"), sys.env("AWS_REGION")))
    .build

  val environment     = sys.env.getOrElse("ENVIRONMENT", "local")
  val serviceName     = sys.env.getOrElse("SERVICE_NAME", "local")
  val namespacePrefix = s"/graphcool/${environment}/"

  case class Metric(name: String, namespace: String, dimensions: List[(String, String)], unit: StandardUnit, value: Double)

  def createMetrics(metric: CloudwatchMetric): List[Metric] = {
    List(
      Metric(
        metric.name,
        s"$namespacePrefix${metric.namespacePostfix}",
        List(("By Service", serviceName), (metric.dimensionName, metric.dimensionValue)),
        metric.unit,
        metric.value
      ),
      Metric(
        metric.name,
        s"$namespacePrefix${metric.namespacePostfix}",
        List(("By Service", "ALL"), (metric.dimensionName, metric.dimensionValue)),
        metric.unit,
        metric.value
      ),
      Metric(metric.name,
             s"$namespacePrefix${metric.namespacePostfix}",
             List(("By Service", serviceName), (metric.dimensionName, "ALL")),
             metric.unit,
             metric.value),
      Metric(metric.name, s"$namespacePrefix${metric.namespacePostfix}", List(("By Service", "ALL"), (metric.dimensionName, "ALL")), metric.unit, metric.value)
    )
  }

  val PUSH_TO_CLOUDWATCH = "PUSH_TO_CLOUDWATCH"

  import context.dispatcher

  val tick =
    context.system.scheduler
      .schedule(FiniteDuration(60, TimeUnit.SECONDS), FiniteDuration(60, TimeUnit.SECONDS), self, PUSH_TO_CLOUDWATCH)

  override def postStop() = tick.cancel()

  val metrics: scala.collection.mutable.MutableList[CloudwatchMetric] = mutable.MutableList()

  def receive = {
    case metric: CloudwatchMetric => {
      metrics += metric
    }
    case PUSH_TO_CLOUDWATCH => {

      import collection.JavaConverters._

      val groups = metrics
        .groupBy(m => (m.namespacePostfix, m.unit, m.dimensionValue, m.dimensionName, m.name))
        .values

      val statistics = groups.map(group => {
        val max   = group.map(_.value).max
        val min   = group.map(_.value).min
        val count = group.length
        val sum   = group.map(_.value).sum

        (group.head, (max, min, count, sum))
      })

      statistics.map(statistic => {
        val statSet = new StatisticSet()
          .withMaximum(statistic._2._1)
          .withMinimum(statistic._2._2)
          .withSampleCount(statistic._2._3.toDouble)
          .withSum(statistic._2._4)

        createMetrics(statistic._1)
          .map((m: Metric) => {
            val request = new PutMetricDataRequest().withNamespace(m.namespace)
            val cwMetric = new MetricDatum()
              .withMetricName(m.name)
              .withUnit(m.unit)
              .withStatisticValues(statSet)
              .withDimensions(
                m.dimensions
                  .map(dimension =>
                    new Dimension()
                      .withName(dimension._1)
                      .withValue(dimension._2))
                  .asJavaCollection)
            request.withMetricData(cwMetric)
          })
          .foreach(x => println(cw.putMetricData(x)))
      })

      metrics.clear()
    }
  }
}
