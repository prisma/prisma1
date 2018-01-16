package cool.graph.metrics

import java.util.concurrent.atomic.AtomicLong

import akka.actor.ActorSystem
import com.timgroup.statsd.StatsDClient

import scala.concurrent.Future
import scala.concurrent.duration._

case class CustomTag(name: String, recordingThreshold: Long = 0) {

  /**
    * Returns the tag string for this tag + value combination.
    * Sets the empty value for the tag ("-") if the value to record is above the threshold.
    * This is mostly interesting for timings right now to reduce noise in custom dimensions.
    */
  def apply(recordedValue: Long, tagValue: String): String = {
    if (recordedValue >= recordingThreshold) {
      s"$name=$tagValue"
    } else {
      s"$name=-"
    }
  }
}

trait Metric {

  val name: String
  val baseTags: String
  val customTags: Seq[CustomTag]
  val client: StatsDClient

  // Merges base tags, defined custom tags, and given custom tag values to construct the metric string for statsd.
  def constructMetricString(recordedValue: Long, customTagValues: Seq[String]): String = {
    val customTagsString  = mergeTagsAndValues(recordedValue, customTagValues)
    val completeTagString = Seq(baseTags, customTagsString).filter(_.nonEmpty).mkString(",")

    Seq(name, completeTagString).filter(_.nonEmpty).mkString("#")
  }

  def mergeTagsAndValues(recordedValue: Long, values: Seq[String]): String = {
    if (values.length != customTags.length) {
      println(
        s"[Metrics] Warning: Metric $name not enough / too many custom tag values given at recording time to fill the defined tags $customTags. Ignoring custom tags.")
      ""
    } else {
      customTags
        .zip(values)
        .map(tagAndValue => tagAndValue._1(recordedValue, tagAndValue._2))
        .mkString(",")
    }
  }
}

/**
  * A simple counter metric. Useful for point-in-time measurements like requests/s.
  *
  * @param name The counter name.
  * @param customTags A collection of unique custom tag names that will be filled during recording time.
  */
case class CounterMetric(name: String, baseTags: String, customTags: Seq[CustomTag], client: StatsDClient) extends Metric {
  // Counters allow custom tags per occurrence
  def inc(customTagValues: String*) = client.incrementCounter(constructMetricString(1, customTagValues))

  def incBy(delta: Long, customTagValues: String*) = client.count(constructMetricString(1, customTagValues), delta)
}

/**
  * A metric recording a constant value until changed (like turning a valve to a certain position).
  * This is useful when long-living things like open socket connections or CPU/memory are measured.
  *
  * Gauges are tricky because they encapsulate a state over time. TODO: elaborate
  * Our statsd is configured to clear a gauge if it hasn't been flushed in the interval (== no metrics reported for gauge).
  *
  * @param name The name of the Gauge.
  * @param predefTags A collection of unique custom tag names with values (!) that will be used for this gauge.
  */
case class GaugeMetric(name: String, baseTags: String, predefTags: Seq[(CustomTag, String)], client: StatsDClient)(implicit flushSystem: ActorSystem)
    extends Metric {
  import flushSystem.dispatcher

  val value = new AtomicLong(0)

  override val customTags   = predefTags.map(_._1)
  val constructedMetricName = constructMetricString(0, predefTags.map(_._2))

  // Important, the interval must be lower than the configured statsd flush interval (curr. 10s), or we see weird metric behaviour.
  flushSystem.scheduler.schedule(0.seconds, 5.seconds) { flush() }

  def add(delta: Long): Unit    = value.addAndGet(delta)
  def set(fixedVal: Long): Unit = value.getAndSet(fixedVal)
  def get: Long                 = value.get
  def inc: Unit                 = add(1)
  def dec: Unit                 = add(-1)

  private def flush() = client.gauge(constructedMetricName, value.get)
}

/**
  * A metric recording a timing and emitting a single measurement to statsd.
  * Useful for timing databases, requests, ... you probably get it.
  *
  * @param name The name of the timer.
  * @param customTags A collection of unique custom tag names that will be filled during recording time.
  */
case class TimerMetric(name: String, baseTags: String, customTags: Seq[CustomTag], client: StatsDClient)(implicit flushSystem: ActorSystem) extends Metric {
  def timeFuture[T](customTagValues: String*)(f: => Future[T]): Future[T] = {
    val startTime = java.lang.System.currentTimeMillis
    val result    = f

    result.onComplete { _ =>
      record(java.lang.System.currentTimeMillis - startTime, customTagValues)
    }(flushSystem.dispatcher)

    result
  }

  def time[T](customTagValues: String*)(f: => T): T = {
    val startTime = java.lang.System.currentTimeMillis
    val res       = f

    record(java.lang.System.currentTimeMillis - startTime, customTagValues)
    res
  }

  def record(timeMillis: Long, customTagValues: Seq[String] = Seq.empty): Unit = {
    val metricName = constructMetricString(timeMillis, customTagValues)
    client.recordExecutionTime(metricName, timeMillis)
  }
}
