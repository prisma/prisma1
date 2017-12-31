package cool.graph.api.queries

import cool.graph.api.ApiBaseSpec
import cool.graph.shared.project_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class ScalarListsQuerySpec extends FlatSpec with Matchers with ApiBaseSpec {

  "empty scalar list" should "return empty list" in {

    val project = SchemaDsl() { schema =>
      schema.model("Model").field("ints", _.Int, isList = true).field("strings", _.String, isList = true)
    }

    database.setup(project)

    val id = server
      .executeQuerySimple(
        s"""mutation {
           |  createModel(data: {strings: []}) {
           |    id
           |  }
           |}""".stripMargin,
        project
      )
      .pathAsString("data.createModel.id")

    val result = server.executeQuerySimple(
      s"""{
         |  model(where: {id:"${id}"}) {
         |    ints
         |    strings
         |  }
         |}""".stripMargin,
      project
    )

    result.toString should equal("""{"data":{"model":{"ints":[],"strings":[]}}}""")
  }

  "full scalar list" should "return full list" in {

    val project = SchemaDsl() { schema =>
      schema.model("Model").field("ints", _.Int, isList = true).field("strings", _.String, isList = true)
    }

    database.setup(project)

    val id = server
      .executeQuerySimple(
        s"""mutation {
           |  createModel(data: {ints: [1], strings: ["short", "looooooooooong"]}) {
           |    id
           |  }
           |}""".stripMargin,
        project
      )
      .pathAsString("data.createModel.id")

    val result = server.executeQuerySimple(
      s"""{
         |  model(where: {id:"${id}"}) {
         |    ints
         |    strings
         |  }
         |}""".stripMargin,
      project
    )

    result.toString should equal("""{"data":{"model":{"ints":[1],"strings":["short","looooooooooong"]}}}""")
  }

  "full scalar list" should "preserve order of elements" in {

    val project = SchemaDsl() { schema =>
      schema.model("Model").field("ints", _.Int, isList = true).field("strings", _.String, isList = true)
    }

    database.setup(project)

    val id = server
      .executeQuerySimple(
        s"""mutation {
           |  createModel(data: {ints: [1,2], strings: ["short", "looooooooooong"]}) {
           |    id
           |  }
           |}""".stripMargin,
        project
      )
      .pathAsString("data.createModel.id")

    server
      .executeQuerySimple(
        s"""mutation {
           |  updateModel(where: {id: "${id}"} data: {ints: { set: [2,1] }}) {
           |    id
           |  }
           |}""".stripMargin,
        project
      )

    val result = server.executeQuerySimple(
      s"""{
         |  model(where: {id:"${id}"}) {
         |    ints
         |    strings
         |  }
         |}""".stripMargin,
      project
    )

    result.toString should equal("""{"data":{"model":{"ints":[2,1],"strings":["short","looooooooooong"]}}}""")
  }

}
