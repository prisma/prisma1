package cool.graph.metrics

import scala.io.Source

object ContainerMetadata {

  /**
    * Fetches the docker container ID of the current container using the hostsname file.
    *
    * @return The docker container ID of the container running this service.
    */
  def fetchContainerId(): String = {
    val source = Source.fromFile("/etc/hostname")
    val hostname = try source.mkString.trim
    finally source.close()

    hostname
  }
}
