package com.prisma.shared.models

case class ProjectId(name: String, stage: String)

case class ProjectIdEncoder(stageSeparator: Char) {
  val workspaceSeparator: Char     = '~'
  private val defaultService       = "default"
  private val defaultStage         = "default"
  val reservedServiceAndStageNames = Seq("cluster", "export", "import")

  def fromEncodedString(str: String): ProjectId = {
    val parts = str.split(stageSeparator)
    val name  = parts(0)
    val stage = parts(1)

    ProjectId(name, stage)
  }

  def toEncodedString(projectId: ProjectId): String = toEncodedString(List(projectId.name, projectId.stage))

  def toEncodedString(name: String, stage: String): String = toEncodedString(List(name, stage))

  def toEncodedString(segments: List[String]): String = {
    segments.filter(_.nonEmpty) match {
      case Nil                               => defaultService + stageSeparator + defaultStage
      case name :: Nil                       => name + stageSeparator + defaultStage
      case name :: stage :: Nil              => name + stageSeparator + stage
      case workspace :: name :: stage :: Nil => workspace + workspaceSeparator + name + stageSeparator + stage
      case _                                 => sys.error("Unsupported project ID encoding. ")
    }
  }

  def fromSegments(segments: List[String]) = {
    val encodedString = toEncodedString(segments)
    fromEncodedString(encodedString)
  }
}
