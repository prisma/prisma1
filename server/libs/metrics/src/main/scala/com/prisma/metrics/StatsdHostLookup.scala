package cool.graph.metrics

import java.net.InetSocketAddress
import java.util.concurrent.Callable

import scala.concurrent.Await

/**
  * As soon as metrics are flushed, this callable is evaluated.
  * The IP address + port of the _host_ (EC2 VM) running the statsd container is returned by call().
  * -> The statsd container binds on the host directly, so we need the VM IP.
  *
  * On error:
  * - No data is send by the library, and the callable is evaluated again next flush.
  * - This catches transient network errors in resolving the statsd host.
  * - Metrics are queued inmemory (defined in the client), nothing is lost on error here.
  */
case class StatsdHostLookup() extends Callable[InetSocketAddress] {

  var lookupCache: Option[InetSocketAddress] = None

  override def call(): InetSocketAddress = {
    lookupCache match {
      case Some(inetAddr) => inetAddr
      case None           => resolve()
    }
  }

  def resolve(): InetSocketAddress = {
    import scala.concurrent.duration._

    println("[Metrics] Fetching instance IP...")

    val fetchIpFuture = InstanceMetadata.fetchInstanceIP()
    val ip            = Await.result(fetchIpFuture, 5.seconds)
    val lookup        = new InetSocketAddress(ip, 8125)

    println("[Metrics] Done fetching instance IP.")
    lookupCache = Some(lookup)
    lookup
  }
}
