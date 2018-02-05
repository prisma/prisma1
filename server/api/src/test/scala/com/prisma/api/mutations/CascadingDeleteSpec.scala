package com.prisma.api.mutations

import com.prisma.api.ApiBaseSpec
import com.prisma.api.database.DatabaseQueryBuilder
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
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_RelayId").as[Int]) should be(Vector(2))
  }

  "P1!-C1! relation deleting the parent" should "work if both sides are marked marked cascading" in {
    //         P-C
    val project = SchemaDsl() { schema =>
      val parent = schema.model("P").field_!("p", _.String, isUnique = true)
      val child = schema
        .model("C")
        .field_!("c", _.String, isUnique = true)
        .oneToOneRelation_!("p", "c", parent, modelAOnDelete = OnDelete.Cascade, modelBOnDelete = OnDelete.Cascade)
    }
    database.setup(project)

    server.executeQuerySimple("""mutation{createP(data:{p:"p", c: {create:{c: "c"}}}){p, c {c}}}""", project)
    server.executeQuerySimple("""mutation{createP(data:{p:"p2", c: {create:{c: "c2"}}}){p, c {c}}}""", project)

    server.executeQuerySimple("""mutation{deleteP(where: {p:"p"}){id}}""", project)
    server.executeQuerySimple("""query{ps{p, c {c}}}""", project).toString should be("""{"data":{"ps":[{"p":"p2","c":{"c":"c2"}}]}}""")
    server.executeQuerySimple("""query{cs{c, p {p}}}""", project).toString should be("""{"data":{"cs":[{"c":"c2","p":{"p":"p2"}}]}}""")
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_RelayId").as[Int]) should be(Vector(2))
  }

  "P1!-C1! relation deleting the parent" should "error if only child is marked marked cascading" in {
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

    server.executeQuerySimpleThatMustFail("""mutation{deleteP(where: {p:"p"}){id}}""", project, errorCode = 3042)
    server.executeQuerySimple("""query{ps{p, c {c}}}""", project).toString should be("""{"data":{"ps":[{"p":"p","c":{"c":"c"}},{"p":"p2","c":{"c":"c2"}}]}}""")
    server.executeQuerySimple("""query{cs{c, p {p}}}""", project).toString should be("""{"data":{"cs":[{"c":"c","p":{"p":"p"}},{"c":"c2","p":{"p":"p2"}}]}}""")

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_RelayId").as[Int]) should be(Vector(4))
  }

  "P1!-C1!-C1!-GC! relation deleting the parent and child and grandchild if marked cascading" should "work" in {
    //         P-C-GC
    val project = SchemaDsl() { schema =>
      val parent = schema.model("P").field_!("p", _.String, isUnique = true)
      val child = schema
        .model("C")
        .field_!("c", _.String, isUnique = true)
        .oneToOneRelation_!("p", "c", parent, modelBOnDelete = OnDelete.Cascade)
      val grandChild = schema
        .model("GC")
        .field_!("gc", _.String, isUnique = true)
        .oneToOneRelation_!("c", "gc", child, modelBOnDelete = OnDelete.Cascade)

    }
    database.setup(project)

    server.executeQuerySimple("""mutation{createP(data:{p:"p", c: {create:{c: "c", gc :{create:{gc: "gc"}}}}}){p, c {c, gc{gc}}}}""", project)
    server.executeQuerySimple("""mutation{createP(data:{p:"p2", c: {create:{c: "c2", gc :{create:{gc: "gc2"}}}}}){p, c {c,gc{gc}}}}""", project)

    server.executeQuerySimple("""mutation{deleteP(where: {p:"p"}){id}}""", project)

    server.executeQuerySimple("""query{ps{p, c {c, gc{gc}}}}""", project).toString should be(
      """{"data":{"ps":[{"p":"p2","c":{"c":"c2","gc":{"gc":"gc2"}}}]}}""")
    server.executeQuerySimple("""query{cs{c, gc{gc}, p {p}}}""", project).toString should be(
      """{"data":{"cs":[{"c":"c2","gc":{"gc":"gc2"},"p":{"p":"p2"}}]}}""")
    server.executeQuerySimple("""query{gCs{gc, c {c, p{p}}}}""", project).toString should be(
      """{"data":{"gCs":[{"gc":"gc2","c":{"c":"c2","p":{"p":"p2"}}}]}}""")

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_RelayId").as[Int]) should be(Vector(3))
  }

  "P1!-C1!-C1-GC relation deleting the parent and child marked cascading" should "work but preserve the grandchild" in {
    //         P-C-GC
    val project = SchemaDsl() { schema =>
      val parent = schema.model("P").field_!("p", _.String, isUnique = true)
      val child = schema
        .model("C")
        .field_!("c", _.String, isUnique = true)
        .oneToOneRelation_!("p", "c", parent, modelBOnDelete = OnDelete.Cascade)
      val grandChild = schema
        .model("GC")
        .field_!("gc", _.String, isUnique = true)
        .oneToOneRelation("c", "gc", child)

    }
    database.setup(project)

    server.executeQuerySimple("""mutation{createP(data:{p:"p", c: {create:{c: "c", gc :{create:{gc: "gc"}}}}}){p, c {c, gc{gc}}}}""", project)
    server.executeQuerySimple("""mutation{createP(data:{p:"p2", c: {create:{c: "c2", gc :{create:{gc: "gc2"}}}}}){p, c {c,gc{gc}}}}""", project)

    server.executeQuerySimple("""mutation{deleteP(where: {p:"p"}){id}}""", project)

    server.executeQuerySimple("""query{ps{p, c {c, gc{gc}}}}""", project).toString should be(
      """{"data":{"ps":[{"p":"p2","c":{"c":"c2","gc":{"gc":"gc2"}}}]}}""")
    server.executeQuerySimple("""query{cs{c, gc{gc}, p {p}}}""", project).toString should be(
      """{"data":{"cs":[{"c":"c2","gc":{"gc":"gc2"},"p":{"p":"p2"}}]}}""")
    server.executeQuerySimple("""query{gCs{gc, c {c, p{p}}}}""", project).toString should be(
      """{"data":{"gCs":[{"gc":"gc","c":null},{"gc":"gc2","c":{"c":"c2","p":{"p":"p2"}}}]}}""")

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_RelayId").as[Int]) should be(Vector(4))
  }

  "P1!-C1! relation deleting the parent marked cascading" should "error if the child is required in another non-cascading relation" in {
    //         P-C-GC
    val project = SchemaDsl() { schema =>
      val parent = schema.model("P").field_!("p", _.String, isUnique = true)
      val child = schema
        .model("C")
        .field_!("c", _.String, isUnique = true)
        .oneToOneRelation_!("p", "c", parent, modelBOnDelete = OnDelete.Cascade)
      val grandChild = schema
        .model("GC")
        .field_!("gc", _.String, isUnique = true)
        .oneToOneRelation_!("c", "gc", child)

    }
    database.setup(project)

    server.executeQuerySimple("""mutation{createP(data:{p:"p", c: {create:{c: "c", gc :{create:{gc: "gc"}}}}}){p, c {c, gc{gc}}}}""", project)
    server.executeQuerySimple("""mutation{createP(data:{p:"p2", c: {create:{c: "c2", gc :{create:{gc: "gc2"}}}}}){p, c {c,gc{gc}}}}""", project)

    server.executeQuerySimpleThatMustFail("""mutation{deleteP(where: {p:"p"}){id}}""", project, errorCode = 3042)
    server.executeQuerySimple("""query{ps{p, c {c}}}""", project).toString should be("""{"data":{"ps":[{"p":"p","c":{"c":"c"}},{"p":"p2","c":{"c":"c2"}}]}}""")
    server.executeQuerySimple("""query{cs{c, p {p}}}""", project).toString should be("""{"data":{"cs":[{"c":"c","p":{"p":"p"}},{"c":"c2","p":{"p":"p2"}}]}}""")

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_RelayId").as[Int]) should be(Vector(4))
  }

  "P1!-C1! PM-SC1! relation deleting the parent marked cascading" should "work" in {
    //         P
    //       /   \
    //      C     SC

    val project = SchemaDsl() { schema =>
      val parent = schema.model("P").field_!("p", _.String, isUnique = true)
      val child = schema
        .model("C")
        .field_!("c", _.String, isUnique = true)
        .oneToOneRelation_!("p", "c", parent, modelBOnDelete = OnDelete.Cascade)
      val stepChild = schema
        .model("SC")
        .field_!("sc", _.String, isUnique = true)
      parent.oneToManyRelation_!("scs", "p", stepChild, modelAOnDelete = OnDelete.Cascade)

    }
    database.setup(project)

    server.executeQuerySimple("""mutation{createP(data:{p:"p", c: {create:{c: "c"}}, scs: {create:[{sc: "sc1"},{sc: "sc2"}]}}){p, c {c},scs{sc}}}""", project)
    server.executeQuerySimple("""mutation{createP(data:{p:"p2", c: {create:{c: "c2"}}, scs: {create:[{sc: "sc3"},{sc: "sc4"}]}}){p, c {c},scs{sc}}}""", project)

    server.executeQuerySimple("""mutation{deleteP(where: {p:"p"}){id}}""", project)

    server.executeQuerySimple("""query{ps{p, c {c, gc{gc}}}}""", project).toString should be(
      """{"data":{"ps":[{"p":"p2","c":{"c":"c2","gc":{"gc":"gc2"}}}]}}""")
    server.executeQuerySimple("""query{cs{c, gc{gc}, p {p}}}""", project).toString should be(
      """{"data":{"cs":[{"c":"c2","gc":{"gc":"gc2"},"p":{"p":"p2"}}]}}""")
    server.executeQuerySimple("""query{gCs{gc, c {c, p{p}}}}""", project).toString should be(
      """{"data":{"gCs":[{"gc":"gc","c":null},{"gc":"gc2","c":{"c":"c2","p":{"p":"p2"}}}]}}""")

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_RelayId").as[Int]) should be(Vector(4))
  }
}
