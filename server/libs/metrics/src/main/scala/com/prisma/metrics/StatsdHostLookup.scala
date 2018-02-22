package com.prisma.metrics

import java.net.{DatagramSocket, InetAddress, InetSocketAddress, Socket}
import java.util.concurrent.Callable

import scala.concurrent.Await
import scala.util.{Failure, Success, Try}

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
      case None =>
        resolveAndPutIntoCache()

      case Some(inetSocketAddr) =>
//        val isReachable = doesServerListenOnSocketAddress(inetSocketAddr)
//        if (isReachable) {
//          log(s"isReachable: ${pretty(inetSocketAddr)}")
//          inetSocketAddr
//        } else {
//          log(s"socket address was not reachable anymore")
//          resolveAndPutIntoCache()
//        }
        inetSocketAddr
    }
  }

  def resolveAndPutIntoCache(): InetSocketAddress = {
    val address       = InetAddress.getByName(dnsName)
    val socketAddress = new InetSocketAddress(address, port)
    log(s"resolved: ${pretty(socketAddress)}")
    lookupCache = Some(socketAddress)
    socketAddress
  }

  def pretty(socketAddress: InetSocketAddress) = s"IP:${socketAddress.getAddress.getHostAddress} Port:${socketAddress.getPort}"

//  def doesServerListenOnSocketAddress(socketAddress: InetSocketAddress): Boolean = {
//    Try {
//      val socket = new DatagramSocket()
//      socket.connect(socketAddress)
//      socket
//    } match {
//      case Success(socket) =>
//        Try(socket.close())
//        true
//
//      case Failure(exception) =>
//        log(s"failed with the following exception")
//        exception.printStackTrace()
//        false
//    }
//  }

  def log(msg: String): Unit = println(s"[${this.getClass.getSimpleName}] $msg")
}
