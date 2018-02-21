package com.prisma.metrics

import java.net.{InetAddress, InetSocketAddress}
import java.util.concurrent.Callable

import scala.concurrent.Await

/**
  * As soon as metrics are flushed, this callable is evaluated.
  * The IP address + port of a _host_ (EC2 VM) running a statsd container is returned by call().
  *
  * On error:
  * - No data is send by the library, and the callable is evaluated again next flush.
  * - This catches transient network errors in resolving the statsd host.
  * - Metrics are queued inmemory (defined in the client), nothing is lost on error here.
  */
case class StatsdHostLookup(dnsName: String, port: Int, reachableTimeout: Int) extends Callable[InetSocketAddress] {

  var lookupCache: Option[InetSocketAddress] = None

  override def call(): InetSocketAddress = {
    lookupCache match {
      case Some(inetSocketAddr) =>
        val isReachable = inetSocketAddr.getAddress.isReachable(reachableTimeout)
        if (isReachable) inetSocketAddr else resolveAndPutIntoCache()
      case None =>
        resolveAndPutIntoCache()
    }
  }

  def resolveAndPutIntoCache(): InetSocketAddress = {
    val address       = InetAddress.getByName(dnsName)
    val socketAddress = new InetSocketAddress(address, port)
    lookupCache = Some(socketAddress)
    socketAddress
  }
}
