package cool.graph.deploy.migration.inference

import cool.graph.deploy.specutils.TestProject
import cool.graph.shared.models.{ProjectWithClientId, Schema}
import cool.graph.shared.project_dsl.SchemaDsl
import org.scalactic.Or
import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.Json
import sangria.parser.QueryParser

import scala.reflect.io.File

class GenerateBigProjectSpec extends FlatSpec with Matchers {
  import cool.graph.shared.models.ProjectJsonFormatter._

  val inferer = SchemaInferrer()

  "the big project" should "be generated in" in {
    val schema              = readFromDisk("big_schema")
    val result              = infer(schema).get
    val project             = TestProject().copy(schema = result)
    val projectWithClientId = ProjectWithClientId(project)

    val json = Json.toJson(projectWithClientId).toString()
    writeSchemaIntoFile(json)
  }

  def writeSchemaIntoFile(schema: String): Unit = File("schema").writeAll(schema)

  def readFromDisk(file: String): String = {
    val stream  = getClass.getResourceAsStream(s"/$file.gql")
    val content = scala.io.Source.fromInputStream(stream).getLines.mkString
    stream.close()
    content
  }

  def infer(types: String): Or[Schema, ProjectSyntaxError] = {
    val document     = QueryParser.parse(types).get
    val emptyProject = SchemaDsl().buildProject()
    inferer.infer(emptyProject.schema, SchemaMapping.empty, document)
  }
}
