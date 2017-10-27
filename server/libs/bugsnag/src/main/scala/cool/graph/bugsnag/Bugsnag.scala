package cool.graph.bugsnag

import com.bugsnag.{Bugsnag => BugsnagClient}

case class Request(method: String, uri: String, headers: Map[String, String])
case class MetaData(tabName: String, key: String, value: Any)
case class GraphCoolRequest(requestId: String, query: String, variables: String, clientId: Option[String], projectId: Option[String])

trait BugSnagger {
  def report(t: Throwable): Unit = report(t, Seq.empty)

  def report(t: Throwable, graphCoolRequest: GraphCoolRequest): Unit
  def report(t: Throwable, metaDatas: Seq[MetaData]): Unit
  def report(t: Throwable, request: Request): Unit
  def report(t: Throwable, request: Request, metaDatas: Seq[MetaData]): Unit
  def report(t: Throwable, requestHeader: Option[Request], metaDatas: Seq[MetaData]): Unit
}

case class BugSnaggerImpl(apiKey: String) extends BugSnagger {
  val gitSha         = sys.env.get("COMMIT_SHA").getOrElse("commit sha not set")
  val environment    = sys.env.get("ENVIRONMENT").getOrElse("environment not set")
  val service        = sys.env.get("SERVICE_NAME").getOrElse("service not set")
  val hostName       = java.net.InetAddress.getLocalHost.getHostName
  private val client = new BugsnagClient(apiKey)

  override def report(t: Throwable): Unit = report(t, Seq.empty)

  override def report(t: Throwable, graphCoolRequest: GraphCoolRequest): Unit = {
    val metaDatas = Seq(
      MetaData("Ids", "requestId", graphCoolRequest.requestId),
      MetaData("Ids", "clientId", graphCoolRequest.clientId.getOrElse("no clientId")),
      MetaData("Ids", "projectId", graphCoolRequest.projectId.getOrElse("no projectId")),
      MetaData("Query", "query", graphCoolRequest.query),
      MetaData("Query", "variables", graphCoolRequest.variables)
    )
    report(t, metaDatas)
  }

  override def report(t: Throwable, metaDatas: Seq[MetaData]): Unit = report(t, None, metaDatas)

  override def report(t: Throwable, request: Request): Unit = report(t, request, Seq.empty)

  override def report(t: Throwable, request: Request, metaDatas: Seq[MetaData]): Unit = {
    report(t, Some(request), metaDatas)
  }

  override def report(t: Throwable, requestHeader: Option[Request], metaDatas: Seq[MetaData]): Unit = {
    val report = client.buildReport(t)

    // In case we're running in an env without api key (local or testing), just print the messages for debugging
    if (apiKey.isEmpty) {
      println(s"[Bugsnag - local / testing] Error: $t")
    }

    report.addToTab("App", "releaseStage", environment)
    report.addToTab("App", "service", service)
    report.addToTab("App", "version", gitSha)
    report.addToTab("App", "hostname", hostName)

    requestHeader.foreach { headers =>
      report.addToTab("Request", "uri", headers.uri)
      report.addToTab("Request", "method", headers.method)
      report.addToTab("Request", "headers", headersAsString(headers))
    }

    metaDatas.foreach { md =>
      report.addToTab(md.tabName, md.key, md.value)
    }

    client.notify(report)
  }

  private def headersAsString(request: Request): String = {
    request.headers
      .map {
        case (key, value) => s"$key: $value"
      }
      .mkString("\n")
  }

}

object BugSnaggerMock extends BugSnagger {
  override def report(t: Throwable): Unit = report(t, Seq.empty)

  override def report(t: Throwable, graphCoolRequest: GraphCoolRequest): Unit = Unit

  override def report(t: Throwable, metaDatas: Seq[MetaData]): Unit = Unit

  override def report(t: Throwable, request: Request): Unit = Unit

  override def report(t: Throwable, request: Request, metaDatas: Seq[MetaData]): Unit = Unit

  override def report(t: Throwable, requestHeader: Option[Request], metaDatas: Seq[MetaData]): Unit = Unit
}
