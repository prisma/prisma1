package com.prisma.api.mutations

import com.prisma.api.ApiBaseSpec
import com.prisma.api.database.CascadingDeletes._
import com.prisma.api.database.DatabaseMutationBuilder
import com.prisma.shared.models._
import com.prisma.shared.project_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class CascadingDeleteSpec extends FlatSpec with Matchers with ApiBaseSpec {

  // Test that it:
  //      deletes all relation rows
  //      deletes all nodes
  //      but only the ones connected by path
  //      fails on required relations after end of cascading path
  //      correctly discerns between cascade / set null

  "P1!-C1! relation deleting the parent" should "work if parent is marked marked cascading" in {
    //         P-C
    val project = SchemaDsl() { schema =>
      val parent = schema.model("P").field_!("p", _.String, isUnique = true)
      val child = schema
        .model("C")
        .field_!("c", _.String, isUnique = true)
        .oneToOneRelation_!("p", "c", parent, modelBOnDelete = OnDelete.Cascade)

    }
    database.setup(project)

    server.executeQuerySimple("""mutation{createP(data:{p:"p", c: {create:{c: "c"}}}){p, c {c}}}""", project)
    server.executeQuerySimple("""mutation{createP(data:{p:"p2", c: {create:{c: "c2"}}}){p, c {c}}}""", project)

    server.executeQuerySimple("""mutation{deleteP(where: {p:"p"}){id}}""", project)
    server.executeQuerySimple("""query{ps{p, c {c}}}""", project).toString should be("""{"data":{"ps":[{"p":"p2","c":{"c":"c2"}}]}}""")
    server.executeQuerySimple("""query{cs{c, p {p}}}""", project).toString should be("""{"data":{"cs":[{"c":"c2","p":{"p":"p2"}}]}}""")
  }

  "P1!-C1! relation deleting the parent" should "not work if only child is marked marked cascading" in {
    //         P-C
    val project = SchemaDsl() { schema =>
      val parent = schema.model("P").field_!("p", _.String, isUnique = true)
      val child = schema
        .model("C")
        .field_!("c", _.String, isUnique = true)
        .oneToOneRelation_!("p", "c", parent, modelAOnDelete = OnDelete.Cascade)

    }
    database.setup(project)

    server.executeQuerySimple("""mutation{createP(data:{p:"p", c: {create:{c: "c"}}}){p, c {c}}}""", project)
    server.executeQuerySimple("""mutation{createP(data:{p:"p2", c: {create:{c: "c2"}}}){p, c {c}}}""", project)

    server.executeQuerySimple("""mutation{deleteP(where: {p:"p"}){id}}""", project)
    server.executeQuerySimple("""query{ps{p, c {c}}}""", project).toString should be("""{"data":{"ps":[{"p":"p2","c":{"c":"c2"}}]}}""")
    server.executeQuerySimple("""query{cs{c, p {p}}}""", project).toString should be("""{"data":{"cs":[{"c":"c2","p":{"p":"p2"}}]}}""")
  }
}
