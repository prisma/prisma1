package com.prisma.metrics

import java.util.concurrent.atomic.AtomicLong

import akka.actor.ActorSystem
import com.librato.metrics.client._

import scala.concurrent.duration._

case class LibratoGaugeMetric(
    name: String,
    baseTags: Map[String, String],
    customTags: Seq[(CustomTag, String)],
    reporter: LibratoReporter,
    flushInterval: FiniteDuration
)(implicit flushSystem: ActorSystem) {

  import flushSystem.dispatcher
  flushSystem.scheduler.schedule(flushInterval, flushInterval) { flush() }

  val value = new AtomicLong(0)

  def add(delta: Long): Unit    = value.addAndGet(delta)
  def set(fixedVal: Long): Unit = value.getAndSet(fixedVal)
  def get: Long                 = value.get
  def inc: Unit                 = add(1)
  def dec: Unit                 = add(-1)

  private def flush(): Unit = {
    val gaugeMeasure = new GaugeMeasure(name, value.get)
    val tagged       = new TaggedMeasure(gaugeMeasure)
    customTags.foreach { tuple =>
      val customTag = tuple._1
      val tagValue  = tuple._2
      tagged.addTag(new Tag(customTag.name, customTag.sanitizeValue(tagValue)))
    }
    baseTags.foreach { baseTag =>
      tagged.addTag(new Tag(baseTag._1, baseTag._2))
    }
    reporter.report(tagged)
  }
}
