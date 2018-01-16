package cool.graph.metrics

import cool.graph.metrics.utils.{TestLiveMetricsManager, TestMetricsManager}
import org.scalatest.{FlatSpec, Matchers}

class MetricsTagSpec extends FlatSpec with Matchers {
  it should "have the correct metrics tags without extra custom tags" in {
    val manager = new TestMetricsManager()
    val counter = manager.defineCounter("testCounter")

    counter.constructMetricString(0, Seq("1", "2")) should equal("TestService.testCounter#env=test,instance=local,container=none")
  }

  it should "have the correct metrics tags with custom metrics set" in {
    val manager = new TestMetricsManager()
    val counter = manager.defineCounter("testCounter", CustomTag("testCustomTag1"), CustomTag("testCustomTag2"))

    counter.constructMetricString(0, Seq("1", "2")) should equal(
      "TestService.testCounter#env=test,instance=local,container=none,testCustomTag1=1,testCustomTag2=2")
  }

  it should "have the correct metrics tags for gauges" in {
    val manager = new TestMetricsManager()
    val gauge   = manager.defineGauge("testCounter", (CustomTag("testCustomTag1"), "1"), (CustomTag("testCustomTag2"), "2"))

    gauge.constructedMetricName should equal("TestService.testCounter#env=test,instance=local,container=none,testCustomTag1=1,testCustomTag2=2")
  }

  it should "have the correct metrics tags for timers" in {
    val manager = new TestMetricsManager()
    val timer   = manager.defineTimer("testTimer", CustomTag("projectId"))

    timer.constructMetricString(0, Seq("1234")) should equal("TestService.testTimer#env=test,instance=local,container=none,projectId=1234")
  }

  it should "ignore custom metric tags if the number of provided values doesn't match" in {
    val manager = new TestMetricsManager()
    val counter = manager.defineCounter("testCounter", CustomTag("testCustomTag1"), CustomTag("testCustomTag2"))

    counter.constructMetricString(0, Seq("1")) should equal("TestService.testCounter#env=test,instance=local,container=none")
  }

  it should "not record a custom tag value if the recorded value is above the specified threshold" in {
    val manager = new TestMetricsManager()
    val timer   = manager.defineTimer("testTimer", CustomTag("projectId", recordingThreshold = 100))

    timer.constructMetricString(90, Seq("1234")) should equal("TestService.testTimer#env=test,instance=local,container=none,projectId=-")
  }

  // Only run if you want some live metrics in librato
  ignore should "do some live metrics against librato" in {
    val manager = new TestLiveMetricsManager

    val counter       = manager.defineCounter("testCounter")
    val counterCustom = manager.defineCounter("testCounterWithTags", CustomTag("tag1"), CustomTag("tag2"))
    val gauge         = manager.defineGauge("testGauge")
    val gaugeCustom   = manager.defineGauge("testGaugeWithTags", (CustomTag("tag1"), "constantVal"))
    val timer         = manager.defineTimer("testTimer")
    val timerCustom   = manager.defineTimer("testTimerWithTags", CustomTag("tag1"))

    gauge.set(100)
    gaugeCustom.set(50)
    counter.inc()
    counterCustom.inc("val1", "val2")

    timer.time() {
      Thread.sleep(500)
    }

    timerCustom.time("val1") {
      Thread.sleep(800)
    }

    Thread.sleep(10000)
  }
}
