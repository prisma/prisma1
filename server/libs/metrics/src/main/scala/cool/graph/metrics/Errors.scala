package cool.graph.metrics

import com.timgroup.statsd.StatsDClientErrorHandler
import cool.graph.bugsnag.BugSnaggerImpl

/**
  * Custom error handler to hook into the statsd library.
  * Logs to stdout and reports to bugsnag.
  * Doesn't interrupt application execution by just reporting errors and then swallowing them.
  */
case class CustomErrorHandler() extends StatsDClientErrorHandler {
  val bugsnag = BugSnaggerImpl(sys.env.getOrElse("BUGSNAG_API_KEY", ""))

  override def handle(exception: java.lang.Exception): Unit = {
    bugsnag.report(exception)
    println(s"[Metrics] Encountered error: $exception")
  }
}

/**
  *  Custom error class for easier Bugsnag distinction and to allow the java lib to catch init errors
  */
case class MetricsError(reason: String) extends java.lang.Exception(reason)
