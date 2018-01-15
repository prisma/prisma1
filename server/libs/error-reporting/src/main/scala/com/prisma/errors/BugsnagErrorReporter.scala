package com.prisma.errors

import com.bugsnag.{Bugsnag, Report}

case class BugsnagErrorReporter(apiKey: String) extends ErrorReporter {
  private val client = new Bugsnag(apiKey)

  val environment = sys.env.getOrElse("ENV", "No env set")
  val service     = sys.env.getOrElse("SERVICE_NAME", "No service set")
  val version     = sys.env.getOrElse("CLUSTER_VERSION", "No version set")

  override def report(t: Throwable, meta: ErrorMetadata*): Unit = {
    val report: Report = client.buildReport(t)

    // General metadata
    report.addToTab("App", "Env", environment)
    report.addToTab("App", "Service", service)
    report.addToTab("App", "Version", version)

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
    } else {
      client.notify(report)
    }
  }

  private def addOther(r: Report, meta: GenericMetadata) = {
    r.addToTab(meta.group, meta.key, meta.value)
  }

  private def addRequest(r: Report, meta: RequestMetadata) = {
    r.addToTab("Request", "Id", meta.requestId)
    r.addToTab("Request", "Method", meta.method)
    r.addToTab("Request", "Uri", meta.uri)
    r.addToTab("Request", "Headers", headersAsString(meta.headers))
  }

  private def addGraphQl(r: Report, meta: GraphQlMetadata) = {
    r.addToTab("GraphQl", "Query", meta.query)
    r.addToTab("GraphQl", "Variables", meta.variables)
  }

  private def addProject(r: Report, meta: ProjectMetadata) = {
    r.addToTab("Project", "Id", meta.id)
  }

  private def headersAsString(headers: Map[String, String]): String = {
    headers
      .map {
        case (key, value) => s"$key: $value"
      }
      .mkString("\n")
  }
}
