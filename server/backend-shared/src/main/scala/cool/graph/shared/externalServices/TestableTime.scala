package cool.graph.shared.externalServices

import org.joda.time.DateTime

trait TestableTime {
  def DateTime: org.joda.time.DateTime
}

class TestableTimeImplementation extends TestableTime {
  override def DateTime: DateTime = org.joda.time.DateTime.now
}

/**
  * The Mock generates a DateTime the first time it is called and holds on to it.
  * Reusing the same mock for an entire test allows us to verify generated DateTimes
  */
class TestableTimeMock extends TestableTime {
  var cache                           = org.joda.time.DateTime.now
  def setDateTime(dateTime: DateTime) = cache = dateTime
  override def DateTime: DateTime     = cache
}
