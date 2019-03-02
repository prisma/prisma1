package com.prisma.metrics

import io.micrometer.core.instrument.Tag
import scala.concurrent.{ExecutionContext, Future}

case class CustomTag(name: String, recordingThreshold: Long = 0)

trait Metric {
  val name: String
  val customTags: Seq[CustomTag]

  protected def createTags(customTagValues: Seq[String]): java.util.List[Tag] = {
    import collection.JavaConverters._
    customTags
      .zip(customTagValues)
      .map(tagAndValue => Tag.of(tagAndValue._1.name, sanitizeValue(tagAndValue._2)))
      .asJava
  }

  private def sanitizeValue(tagValue: String) = tagValue.replace('~', '-').replace('@', '-').replace('$', '-')
}

trait CounterMetric extends Metric {
  def inc(customTagValues: String*)
  def incBy(delta: Long, customTagValues: String*)
}

trait TimerMetric extends Metric {
  def timeFuture[T](customTagValues: String*)(f: => Future[T])(implicit ec: ExecutionContext): Future[T]
  def time[T](customTagValues: String*)(f: => T): T
  def record(timeMillis: Long, customTagValues: Seq[String] = Seq.empty): Unit
}

trait GaugeMetric extends Metric {
  def inc: Unit
  def dec: Unit
  def add(delta: Long): Unit
  def set(fixedVal: Long): Unit
  def get: Long
}
