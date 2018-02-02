package com.prisma.api.mutations

import com.prisma.api.ApiBaseSpec
import com.prisma.shared.models.OnDelete
import com.prisma.shared.project_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class OnDeleteDirectiveSpec extends FlatSpec with Matchers with ApiBaseSpec {

  "A relation with a onDelete SET_NULL directive" should "set the value on the other node to null" ignore {
    val project = SchemaDsl() { schema =>
      val modelB = schema.model("ModelB").field_!("b", _.String, isUnique = true)
      val modelA = schema.model("ModelA").field_!("a", _.String, isUnique = true).oneToOneRelation("modelB", "modelA", modelB)

    }
    database.setup(project)

    val res = server.executeQuerySimple(
      s"""mutation {
         |  createModelB(data: {
         |    b: "b"
         |    modelA: {create: {a:"a"}}
         |    }
         |  ){
         |  b
         |  }
         |}""".stripMargin,
      project
    )

    res.toString should be(s"""{"data":{"createModelB":{"b":"b"}}}""")

    val deleteRes = server.executeQuerySimple("""mutation{deleteModelB(where: { b: "b" }){b}}""", project)
    deleteRes.toString should be(s"""{"data":{"deleteModelB":{"b":"b"}}}""")

    server.executeQuerySimple("{modelBs{b}}", project).toString should be("""{"data":{"modelBs":[]}}""")
    server.executeQuerySimple("{modelAs{a}}", project).toString should be("""{"data":{"modelAs":[{"a":"a"}]}}""")
  }

  "A relation with a onDelete CASCADE directive" should "delete the connected node" ignore {
    val project = SchemaDsl() { schema =>
      val modelB = schema.model("ModelB").field_!("b", _.String, isUnique = true)
      val modelA = schema
        .model("ModelA")
        .field_!("a", _.String, isUnique = true)
        .oneToOneRelation("modelB", "modelA", modelB, modelAOnDelete = OnDelete.Cascade, modelBOnDelete = OnDelete.Cascade)

    }
    database.setup(project)

    val res = server.executeQuerySimple(
      s"""mutation {
         |  createModelB(data: {
         |    b: "b"
         |    modelA: {create: {a:"a"}}
         |    }
         |  ){
         |  b
         |  }
         |}""".stripMargin,
      project
    )

    res.toString should be(s"""{"data":{"createModelB":{"b":"b"}}}""")

    val deleteRes = server.executeQuerySimple("""mutation{deleteModelB(where: { b: "b" }){b}}""", project)
    deleteRes.toString should be(s"""{"data":{"deleteModelB":{"b":"b"}}}""")

    server.executeQuerySimple("{modelBs{b}}", project).toString should be("""{"data":{"modelBs":[]}}""")
    server.executeQuerySimple("{modelAs{a}}", project).toString should be("""{"data":{"modelAs":[]}}""")
  }
}
