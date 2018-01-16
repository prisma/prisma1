package cool.graph.messagebus.utils

import scala.io.Source
import scala.util.{Failure, Success, Try}

object Utils {

  val dockerContainerID: String = {
    Try {
      val source   = Source.fromFile("/etc/hostname")
      val hostname = try { source.mkString.trim } finally source.close()

      hostname
    } match {
      case Success(hostname) => hostname
      case Failure(err)      => println("Warning: Unable to read hostname from /etc/hostname"); ""
    }
  }
}
