package cool.graph.api

import cool.graph.shared.project_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class Queries extends FlatSpec with Matchers with ApiTestServer {
  "Simple Query" should "work" in {
    val schema = SchemaDsl()
    schema.model("Car").field("wheelCount", _.Int).field_!("name", _.String)
    val (client, project) = schema.buildClientAndProject()

    setupProject(client, project)
  }
}
