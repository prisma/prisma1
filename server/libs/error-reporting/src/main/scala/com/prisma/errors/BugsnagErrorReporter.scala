package com.prisma.errors

import java.lang.Thread.UncaughtExceptionHandler

import com.bugsnag.{Bugsnag, Report}

import scala.collection.immutable.Seq

case class BugsnagErrorReporter(apiKey: String) extends ErrorReporter {
  private val client = {
    val sendUncaughtExceptions = false // we are doing this ourselves
    new Bugsnag(apiKey, sendUncaughtExceptions)
  }

  // use this instance as uncaught exception handler
  val self = this
  val selfAsUncaughtExceptionHandler = new UncaughtExceptionHandler {
    override def uncaughtException(t: Thread, e: Throwable): Unit = self.report(e)
  }
  Thread.setDefaultUncaughtExceptionHandler(selfAsUncaughtExceptionHandler)

  val env         = sys.env.getOrElse("ENV", "No env set")
  val environment = sys.env.getOrElse("ENVIRONMENT", "No environment set")
  val service     = sys.env.getOrElse("SERVICE_NAME", "No service set")
  val version     = sys.env.getOrElse("CLUSTER_VERSION", "No version set")
  val region      = sys.env.getOrElse("AWS_REGION", "No region set")

  override def report(t: Throwable, meta: ErrorMetadata*): Unit = {
    val report: Report = client.buildReport(t)

    // General metadata
    report.addToTab("App", "Env", env)
    report.addToTab("App", "Environment", environment)
    report.addToTab("App", "Service", service)
    report.addToTab("App", "Version", version)
    report.addToTab("App", "Region", region)

    // Specific metadata
    meta foreach {
      case x: RequestMetadata => addRequest(report, x)
      case x: GraphQlMetadata => addGraphQl(report, x)
      case x: ProjectMetadata => addProject(report, x)
      case x: GenericMetadata => addOther(report, x)
      case x                  => println(s"Unrecognized error metadata: $x")
    }

    // In case we're running in an env without api key (local or testing), just print the messages for debugging
    if (apiKey.isEmpty) {
      println(s"[Bugsnag - local / testing] Error report: $report")
      report.getException.printStackTrace()
    } else {
      client.notify(report)
    }
  }

  private def addOther(r: Report, meta: GenericMetadata) = {
    r.addToTab(meta.group, meta.key, meta.value)
  }

  private def addRequest(r: Report, meta: RequestMetadata) = {
    r.addToTab("Request", "RequestId", meta.requestId)
    r.addToTab("Request", "Method", meta.method)
    r.addToTab("Request", "Uri", meta.uri)
    r.addToTab("Request", "Headers", headersAsString(meta.headers))
  }

  private def addGraphQl(r: Report, meta: GraphQlMetadata) = {
    r.addToTab("GraphQl", "Query", meta.query)
    r.addToTab("GraphQl", "Variables", meta.variables)
  }

  private def addProject(r: Report, meta: ProjectMetadata) = {
    r.addToTab("Project", "ProjectId", meta.id)
  }

  private def headersAsString(headers: Seq[(String, String)]): String = {
    headers
      .map {
        case (key, value) => s"$key: $value"
      }
      .mkString("\n")
  }
}
