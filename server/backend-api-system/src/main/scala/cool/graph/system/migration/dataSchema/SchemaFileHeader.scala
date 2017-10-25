package cool.graph.system.migration.dataSchema

import scala.util.Try

case class SchemaFileHeader(projectId: String, version: Int)

object SchemaFileHeader {
  def parseFromSchema(schema: String): Option[SchemaFileHeader] = {
    def strintToIntOpt(s: String): Option[Int] = Try(s.toInt).toOption
    val frontMatterMap: Map[String, String] = (for {
      line <- schema.lines.toSeq.map(_.trim)
      if line.startsWith("#")
      x        = line.stripPrefix("#")
      elements = x.split(':')
      if elements.size == 2
      key   = elements(0).trim
      value = elements(1).trim
    } yield (key, value)).toMap

    for {
      projectId    <- frontMatterMap.get("projectId").orElse(frontMatterMap.get("project"))
      version      <- frontMatterMap.get("version")
      versionAsInt <- strintToIntOpt(version)
    } yield SchemaFileHeader(projectId = projectId, version = versionAsInt)
  }
}
