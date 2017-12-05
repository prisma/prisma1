package cool.graph

import cool.graph.client.FeatureMetric.FeatureMetric
import cool.graph.client.{MutationQueryWhitelist}
import cool.graph.shared.models.Client
import scaldi.{Injectable, Injector}

import scala.collection.concurrent.TrieMap

trait RequestContextTrait {
  val requestId: String
  val requestIp: String
  val clientId: String
  val projectId: Option[String]
  val log: Function[String, Unit]
  var graphcoolHeader: Option[String] = None

  // The console always includes the header `X-GraphCool-Source` with the value `dashboard:[sub section]`
  def isFromConsole = graphcoolHeader.exists(header => header.contains("dashboard") || header.contains("console"))

  val isSubscription: Boolean = false
  val mutationQueryWhitelist  = new MutationQueryWhitelist()

  private var featureMetrics: TrieMap[String, Unit] = TrieMap()

  def addFeatureMetric(featureMetric: FeatureMetric): Unit = featureMetrics += (featureMetric.toString -> Unit)
  def listFeatureMetrics: List[String]                     = featureMetrics.keys.toList

}

trait SystemRequestContextTrait extends RequestContextTrait {
  override val clientId: String = client.map(_.id).getOrElse("")
  val client: Option[Client]
}

case class RequestContext(clientId: String, requestId: String, requestIp: String, log: Function[String, Unit], projectId: Option[String] = None)(
    implicit inj: Injector)
    extends RequestContextTrait
    with Injectable {}
