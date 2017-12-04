package cool.graph.shared.models

case class ProjectId(name: String, stage: String)

object ProjectId {
  private val separator = '@'

  def fromEncodedString(str: String): ProjectId = {
    val parts = str.split(separator)
    val name  = parts(0)
    val stage = parts(1)
    ProjectId(name, stage)
  }

  def toEncodedString(name: String, stage: String) = name + separator + stage
}
