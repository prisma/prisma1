package cool.graph.client

import cool.graph.shared.models.Region
import cool.graph.shared.models.Region.Region

case class GlobalApiEndpointManager(euWest1: String, usWest2: String, apNortheast1: String) {

  def getEndpointForProject(region: Region, projectId: String): String = {
    region match {
      case Region.EU_WEST_1      => s"${euWest1}/${projectId}"
      case Region.US_WEST_2      => s"${usWest2}/${projectId}"
      case Region.AP_NORTHEAST_1 => s"${apNortheast1}/${projectId}"
    }
  }
}
