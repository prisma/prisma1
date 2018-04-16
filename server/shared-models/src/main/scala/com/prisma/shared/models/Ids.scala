package com.prisma.shared.models

case class ProjectId(name: String, stage: String) {
  def asString = ProjectId.toEncodedString(name, stage)
}

object ProjectId {
  private val workspaceSeparator = '~'
  private val stageSeparator     = '$'

  def fromEncodedString(str: String): ProjectId = {
    val parts = str.split(stageSeparator)
    val name  = parts(0)
    val stage = parts(1)
    ProjectId(name, stage)
  }

  def toEncodedString(name: String, stage: String): String = toEncodedString(List(name, stage))

  def toEncodedString(segments: List[String]): String = {
    segments match {
      case name :: stage :: Nil =>
        name + stageSeparator + stage

      case workspace :: name :: stage :: Nil =>
        workspace + workspaceSeparator + name + stageSeparator + stage

      case _ =>
        sys.error("provided segments must either have size 2 or 3")
    }
  }

  def fromSegments(segments: List[String]) = {
    val encodedString = toEncodedString(segments)
    fromEncodedString(encodedString)
  }
}
