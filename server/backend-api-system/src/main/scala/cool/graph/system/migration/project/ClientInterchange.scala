package cool.graph.system.migration.project

import cool.graph.shared.functions.ExternalFile
import cool.graph.shared.models.Project
import cool.graph.system.migration.ProjectConfig
import sangria.ast.ObjectValue
import scaldi.Injector
import spray.json._

case class ProjectExports(content: String, files: Vector[FileContainer])
case class FileContainer(path: String, content: String)
case class PermissionsExport(modelPermissions: Vector[ObjectValue], relationPermissions: Vector[ObjectValue], files: Vector[FileContainer])
case class DatabaseSchemaExport(databaseSchema: ObjectValue, files: Vector[FileContainer])
case class FunctionsExport(functions: Vector[ObjectValue], files: Vector[FileContainer])

case class ClientInterchangeFormatTop(modules: Vector[ClientInterchangeFormatModule])
case class ClientInterchangeFormatModule(name: String, content: String, files: Map[String, String], externalFiles: Option[Map[String, ExternalFile]])

object ClientInterchangeFormatFormats extends DefaultJsonProtocol {
  implicit lazy val ExternalFileFormat                                                                 = jsonFormat4(ExternalFile)
  implicit lazy val ClientInterchangeFormatModuleFormat: RootJsonFormat[ClientInterchangeFormatModule] = jsonFormat4(ClientInterchangeFormatModule)
  implicit lazy val ClientInterchangeFormatTopFormat: RootJsonFormat[ClientInterchangeFormatTop]       = jsonFormat1(ClientInterchangeFormatTop)
}

object ClientInterchange {
  def export(project: Project)(implicit inj: Injector): ProjectExports = {
    val x = ProjectConfig.moduleFromProject(project) //.print(project)

    val files = x.files

    ProjectExports(x.module.print, files)
  }

  def render(project: Project)(implicit inj: Injector): String = {
    import ClientInterchangeFormatFormats._

    val exports: ProjectExports = export(project)

    ClientInterchangeFormatTop(
      modules = Vector(
        ClientInterchangeFormatModule(
          name = "",
          content = exports.content,
          files = exports.files.map(x => (x.path, x.content)).toMap,
          externalFiles = None
        )
      )
    ).toJson.prettyPrint
  }

  def parse(interchange: String): ClientInterchangeFormatTop = {
    import ClientInterchangeFormatFormats._

    interchange.parseJson.convertTo[ClientInterchangeFormatTop]
  }
}
