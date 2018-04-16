package com.prisma.shared.models

case class ProjectId(name: String, stage: String) {
  def asString = ProjectId.toEncodedString(name, stage)
}

object ProjectId {
  private val workspaceSeparator = '~'
  private val stageSeparator     = '@'
  private val defaultService     = "default"
  private val defaultStage       = "default"

  val reservedServiceNames = Seq("cluster", "export", "import")

  def fromEncodedString(str: String): ProjectId = {
    val parts = str.split(stageSeparator)
    val name  = parts(0)
    val stage = parts(1)

    ProjectId(name, stage)
  }

  def toEncodedString(name: String, stage: String): String = toEncodedString(List(name, stage))

  def toEncodedString(segments: List[String]): String = {
    segments.filter(_.nonEmpty) match {
      case Nil =>
        defaultService + stageSeparator + defaultStage

      case name :: Nil =>
        name + stageSeparator + defaultStage

      case name :: stage :: Nil =>
        name + stageSeparator + stage

      case workspace :: name :: stage :: Nil =>
        workspace + workspaceSeparator + name + stageSeparator + stage

      case _ =>
        sys.error("Unsupported project ID encoding. ")
    }
  }

  def fromSegments(segments: List[String]) = {
    val encodedString = toEncodedString(segments)
    fromEncodedString(encodedString)
  }
}
