package cool.graph.metrics

import com.timgroup.statsd.{Event, ServiceCheck, StatsDClient}

case class DummyStatsDClient() extends StatsDClient {
  override def recordHistogramValue(aspect: String, value: Double, tags: String*): Unit = {}

  override def recordHistogramValue(aspect: String, value: Double, sampleRate: Double, tags: String*): Unit = {}

  override def recordHistogramValue(aspect: String, value: Long, tags: String*): Unit = {}

  override def recordHistogramValue(aspect: String, value: Long, sampleRate: Double, tags: String*): Unit = {}

  override def increment(aspect: String, tags: String*): Unit = {}

  override def increment(aspect: String, sampleRate: Double, tags: String*): Unit = {}

  override def recordGaugeValue(aspect: String, value: Double, tags: String*): Unit = {}

  override def recordGaugeValue(aspect: String, value: Double, sampleRate: Double, tags: String*): Unit = {}

  override def recordGaugeValue(aspect: String, value: Long, tags: String*): Unit = {}

  override def recordGaugeValue(aspect: String, value: Long, sampleRate: Double, tags: String*): Unit = {}

  override def recordEvent(event: Event, tags: String*): Unit = {}

  override def recordSetValue(aspect: String, value: String, tags: String*): Unit = {}

  override def gauge(aspect: String, value: Double, tags: String*): Unit = {}

  override def gauge(aspect: String, value: Double, sampleRate: Double, tags: String*): Unit = {}

  override def gauge(aspect: String, value: Long, tags: String*): Unit = {}

  override def gauge(aspect: String, value: Long, sampleRate: Double, tags: String*): Unit = {}

  override def recordServiceCheckRun(sc: ServiceCheck): Unit = {}

  override def incrementCounter(aspect: String, tags: String*): Unit = {}

  override def incrementCounter(aspect: String, sampleRate: Double, tags: String*): Unit = {}

  override def count(aspect: String, delta: Long, tags: String*): Unit = {}

  override def count(aspect: String, delta: Long, sampleRate: Double, tags: String*): Unit = {}

  override def histogram(aspect: String, value: Double, tags: String*): Unit = {}

  override def histogram(aspect: String, value: Double, sampleRate: Double, tags: String*): Unit = {}

  override def histogram(aspect: String, value: Long, tags: String*): Unit = {}

  override def histogram(aspect: String, value: Long, sampleRate: Double, tags: String*): Unit = {}

  override def decrementCounter(aspect: String, tags: String*): Unit = {}

  override def decrementCounter(aspect: String, sampleRate: Double, tags: String*): Unit = {}

  override def stop(): Unit = {}

  override def serviceCheck(sc: ServiceCheck): Unit = {}

  override def decrement(aspect: String, tags: String*): Unit = {}

  override def decrement(aspect: String, sampleRate: Double, tags: String*): Unit = {}

  override def time(aspect: String, value: Long, tags: String*): Unit = {}

  override def time(aspect: String, value: Long, sampleRate: Double, tags: String*): Unit = {}

  override def recordExecutionTime(aspect: String, timeInMs: Long, tags: String*): Unit = {}

  override def recordExecutionTime(aspect: String, timeInMs: Long, sampleRate: Double, tags: String*): Unit = {}
}
