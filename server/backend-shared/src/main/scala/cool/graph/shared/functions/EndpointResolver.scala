package cool.graph.shared.functions

sealed trait EndpointResolver {
  def endpoints(projectId: String): GraphcoolEndpoints
}

case class GraphcoolEndpoints(simple: String, relay: String, system: String, subscriptions: String) {
  def toMap: Map[String, String] = {
    Map(
      "simple"        -> simple,
      "relay"         -> relay,
      "system"        -> system,
      "subscriptions" -> subscriptions
    )
  }
}

case class LocalEndpointResolver() extends EndpointResolver {
  val port                   = sys.env.getOrElse("PORT", sys.error("PORT env var required but not found."))
  val dockerContainerDNSName = "graphcool"
  val dockerContainerBase    = s"http://$dockerContainerDNSName:$port"

  override def endpoints(projectId: String) = {
    GraphcoolEndpoints(
      simple = s"$dockerContainerBase/simple/v1/$projectId",
      relay = s"$dockerContainerBase/relay/v1/$projectId",
      system = s"$dockerContainerBase/system",
      subscriptions = s"$dockerContainerBase/subscriptions/v1/$projectId"
    )
  }
}

case class LiveEndpointResolver() extends EndpointResolver {
  val awsRegion = sys.env.getOrElse("AWS_REGION", sys.error("AWS_REGION env var required but not found."))

  override def endpoints(projectId: String) = {
    val subscriptionsEndpoint = awsRegion match {
      case "eu-west-1" => s"wss://subscriptions.graph.cool/v1/$projectId"
      case other       => s"wss://subscriptions.$other.graph.cool/v1/$projectId"
    }

    GraphcoolEndpoints(
      simple = s"https://api.graph.cool/simple/v1/$projectId",
      relay = s"https://api.graph.cool/relay/v1/$projectId",
      system = s"https://api.graph.cool/system",
      subscriptions = subscriptionsEndpoint
    )
  }
}

case class MockEndpointResolver() extends EndpointResolver {
  override def endpoints(projectId: String) = {
    GraphcoolEndpoints(
      simple = s"http://test.cool/simple/v1/$projectId",
      relay = s"http://test.cool/relay/v1/$projectId",
      system = s"http://test.cool/system",
      subscriptions = s"http://test.cool/subscriptions/v1/$projectId"
    )
  }
}
