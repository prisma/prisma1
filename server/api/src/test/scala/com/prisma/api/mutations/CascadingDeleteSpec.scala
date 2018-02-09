package com.prisma.api.mutations

import com.prisma.api.ApiBaseSpec
import com.prisma.api.database.DatabaseQueryBuilder
import com.prisma.shared.models._
import com.prisma.shared.project_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class CascadingDeleteSpec extends FlatSpec with Matchers with ApiBaseSpec {

  //region  TOP LEVEL DELETE

  "P1!-C1! relation deleting the parent" should "work if parent is marked marked cascading" in {
    //         P-C
    val project = SchemaDsl() { schema =>
      val parent = schema.model("P").field_!("p", _.String, isUnique = true)
      val child  = schema.model("C").field_!("c", _.String, isUnique = true)

      child.oneToOneRelation_!("p", "c", parent, modelBOnDelete = OnDelete.Cascade)
    }
    database.setup(project)

    server.executeQuerySimple("""mutation{createP(data:{p:"p", c: {create:{c: "c"}}}){p, c {c}}}""", project)
    server.executeQuerySimple("""mutation{createP(data:{p:"p2", c: {create:{c: "c2"}}}){p, c {c}}}""", project)

    server.executeQuerySimple("""mutation{deleteP(where: {p:"p"}){id}}""", project)
    server.executeQuerySimple("""query{ps{p, c {c}}}""", project).toString should be("""{"data":{"ps":[{"p":"p2","c":{"c":"c2"}}]}}""")
    server.executeQuerySimple("""query{cs{c, p {p}}}""", project).toString should be("""{"data":{"cs":[{"c":"c2","p":{"p":"p2"}}]}}""")
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_RelayId").as[Int]) should be(Vector(2))
  }

  "PM-CM relation deleting the parent" should "delete all children if the parent is marked cascading" in {
    //         P-C
    val project = SchemaDsl() { schema =>
      val parent = schema.model("P").field_!("p", _.String, isUnique = true)
      val child  = schema.model("C").field_!("c", _.String, isUnique = true)

      child.manyToManyRelation("p", "c", parent, modelBOnDelete = OnDelete.Cascade)
    }
    database.setup(project)

    server.executeQuerySimple("""mutation{createP(data:{p:"p",  c: {create:[{c: "c"},  {c: "c2"}]}}){p, c {c}}}""", project)
    server.executeQuerySimple("""mutation{createP(data:{p:"p2", c: {create:[{c: "cx"}, {c: "cx2"}]}}){p, c {c}}}""", project)
    server.executeQuerySimple("""mutation{updateC(where:{c:"c2"}, data:{p: {create:{p: "pz"}}}){id}}""", project)

    server.executeQuerySimple("""mutation{deleteP(where: {p:"p"}){id}}""", project)
    server.executeQuerySimple("""query{ps{p, c {c}}}""", project).toString should be(
      """{"data":{"ps":[{"p":"p2","c":[{"c":"cx"},{"c":"cx2"}]},{"p":"pz","c":[]}]}}""")
    server.executeQuerySimple("""query{cs{c, p {p}}}""", project).toString should be(
      """{"data":{"cs":[{"c":"cx","p":[{"p":"p2"}]},{"c":"cx2","p":[{"p":"p2"}]}]}}""")
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_RelayId").as[Int]) should be(Vector(4))
  }

  "PM-CM relation deleting the parent" should "error if both sides are marked cascading since it would be a circle" in {
    //         P-C
    val project = SchemaDsl() { schema =>
      val parent = schema.model("P").field_!("p", _.String, isUnique = true)
      val child  = schema.model("C").field_!("c", _.String, isUnique = true)

      child.manyToManyRelation("p", "c", parent, modelAOnDelete = OnDelete.Cascade, modelBOnDelete = OnDelete.Cascade)
    }
    database.setup(project)

    server.executeQuerySimple("""mutation{createP(data:{p:"p",  c: {create:[{c: "c"},  {c: "c2"}]}}){p, c {c}}}""", project)
    server.executeQuerySimple("""mutation{updateC(where:{c:"c2"}, data:{p: {create:{p: "pz"}}}){id}}""", project)

    server.executeQuerySimpleThatMustFail("""mutation{deleteP(where: {p:"p"}){id}}""", project, errorCode = 3043)
    server.executeQuerySimple("""query{ps{p, c {c}}}""", project).toString should be(
      """{"data":{"ps":[{"p":"p","c":[{"c":"c"},{"c":"c2"}]},{"p":"pz","c":[{"c":"c2"}]}]}}""")
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_RelayId").as[Int]) should be(Vector(4))
  }

  "P1!-C1! relation deleting the parent" should "error if both sides are marked marked cascading" in {
    //         P-C
    val project = SchemaDsl() { schema =>
      val parent = schema.model("P").field_!("p", _.String, isUnique = true)
      val child  = schema.model("C").field_!("c", _.String, isUnique = true)

      child.oneToOneRelation_!("p", "c", parent, modelAOnDelete = OnDelete.Cascade, modelBOnDelete = OnDelete.Cascade)
    }
    database.setup(project)

    server.executeQuerySimple("""mutation{createP(data:{p:"p", c: {create:{c: "c"}}}){p, c {c}}}""", project)

    server.executeQuerySimpleThatMustFail("""mutation{deleteP(where: {p:"p"}){id}}""", project, errorCode = 3043)
    server.executeQuerySimple("""query{ps{p, c {c}}}""", project).toString should be("""{"data":{"ps":[{"p":"p","c":{"c":"c"}}]}}""")
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_RelayId").as[Int]) should be(Vector(2))
  }

  "P1!-C1! relation deleting the parent" should "error if only child is marked marked cascading" in {
    //         P-C
    val project = SchemaDsl() { schema =>
      val parent = schema.model("P").field_!("p", _.String, isUnique = true)
      val child  = schema.model("C").field_!("c", _.String, isUnique = true)

      child.oneToOneRelation_!("p", "c", parent, modelAOnDelete = OnDelete.Cascade)
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
      val parent     = schema.model("P").field_!("p", _.String, isUnique = true)
      val child      = schema.model("C").field_!("c", _.String, isUnique = true)
      val grandChild = schema.model("GC").field_!("gc", _.String, isUnique = true)

      grandChild.oneToOneRelation_!("c", "gc", child, modelBOnDelete = OnDelete.Cascade)
      child.oneToOneRelation_!("p", "c", parent, modelBOnDelete = OnDelete.Cascade)
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
      val parent     = schema.model("P").field_!("p", _.String, isUnique = true)
      val child      = schema.model("C").field_!("c", _.String, isUnique = true)
      val grandChild = schema.model("GC").field_!("gc", _.String, isUnique = true)

      child.oneToOneRelation_!("p", "c", parent, modelBOnDelete = OnDelete.Cascade)
      grandChild.oneToOneRelation("c", "gc", child)
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
      val parent     = schema.model("P").field_!("p", _.String, isUnique = true)
      val child      = schema.model("C").field_!("c", _.String, isUnique = true)
      val grandChild = schema.model("GC").field_!("gc", _.String, isUnique = true)

      child.oneToOneRelation_!("p", "c", parent, modelBOnDelete = OnDelete.Cascade)
      grandChild.oneToOneRelation_!("c", "gc", child)
    }
    database.setup(project)

    server.executeQuerySimple("""mutation{createP(data:{p:"p", c: {create:{c: "c", gc :{create:{gc: "gc"}}}}}){p, c {c, gc{gc}}}}""", project)
    server.executeQuerySimple("""mutation{createP(data:{p:"p2", c: {create:{c: "c2", gc :{create:{gc: "gc2"}}}}}){p, c {c,gc{gc}}}}""", project)

    server.executeQuerySimpleThatMustFail("""mutation{deleteP(where: {p:"p"}){id}}""", project, errorCode = 3042)
    server.executeQuerySimple("""query{ps{p, c {c}}}""", project).toString should be("""{"data":{"ps":[{"p":"p","c":{"c":"c"}},{"p":"p2","c":{"c":"c2"}}]}}""")
    server.executeQuerySimple("""query{cs{c, p {p}}}""", project).toString should be("""{"data":{"cs":[{"c":"c","p":{"p":"p"}},{"c":"c2","p":{"p":"p2"}}]}}""")

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_RelayId").as[Int]) should be(Vector(6))
  }

  "If the parent is not cascading nothing on the path" should "be deleted except for the parent" in {
    //         P-C-GC
    val project = SchemaDsl() { schema =>
      val parent     = schema.model("P").field_!("p", _.String, isUnique = true)
      val child      = schema.model("C").field_!("c", _.String, isUnique = true)
      val grandChild = schema.model("GC").field_!("gc", _.String, isUnique = true)

      child.oneToOneRelation("p", "c", parent, modelAOnDelete = OnDelete.Cascade)
      grandChild.oneToOneRelation("c", "gc", child)
    }
    database.setup(project)

    server.executeQuerySimple("""mutation{createP(data:{p:"p", c: {create:{c: "c", gc :{create:{gc: "gc"}}}}}){p, c {c, gc{gc}}}}""", project)

    server.executeQuerySimple("""mutation{deleteP(where: {p:"p"}){id}}""", project)
    server.executeQuerySimple("""query{ps{p, c {c}}}""", project).toString should be("""{"data":{"ps":[]}}""")
    server.executeQuerySimple("""query{cs{c, p {p}}}""", project).toString should be("""{"data":{"cs":[{"c":"c","p":null}]}}""")
    server.executeQuerySimple("""query{gCs{gc, c {c}}}""", project).toString should be("""{"data":{"gCs":[{"gc":"gc","c":{"c":"c"}}]}}""")

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_RelayId").as[Int]) should be(Vector(2))
  }

  "P1!-C1! PM-SC1! relation deleting the parent marked cascading" should "work" in {
    //         P
    //       /   \
    //      C     SC

    val project = SchemaDsl() { schema =>
      val parent    = schema.model("P").field_!("p", _.String, isUnique = true)
      val child     = schema.model("C").field_!("c", _.String, isUnique = true)
      val stepChild = schema.model("SC").field_!("sc", _.String, isUnique = true)

      child.oneToOneRelation_!("p", "c", parent, modelBOnDelete = OnDelete.Cascade)
      parent.oneToManyRelation_!("scs", "p", stepChild, modelAOnDelete = OnDelete.Cascade)
    }

    database.setup(project)

    server.executeQuerySimple("""mutation{createP(data:{p:"p", c: {create:{c: "c"}}, scs: {create:[{sc: "sc1"},{sc: "sc2"}]}}){p, c {c},scs{sc}}}""", project)
    server.executeQuerySimple("""mutation{createP(data:{p:"p2", c: {create:{c: "c2"}}, scs: {create:[{sc: "sc3"},{sc: "sc4"}]}}){p, c {c},scs{sc}}}""", project)

    server.executeQuerySimple("""mutation{deleteP(where: {p:"p"}){id}}""", project)

    server.executeQuerySimple("""query{ps{p, c {c}, scs {sc}}}""", project).toString should be(
      """{"data":{"ps":[{"p":"p2","c":{"c":"c2"},"scs":[{"sc":"sc3"},{"sc":"sc4"}]}]}}""")
    server.executeQuerySimple("""query{cs{c, p {p}}}""", project).toString should be("""{"data":{"cs":[{"c":"c2","p":{"p":"p2"}}]}}""")
    server.executeQuerySimple("""query{sCs{sc,  p{p}}}""", project).toString should be(
      """{"data":{"sCs":[{"sc":"sc3","p":{"p":"p2"}},{"sc":"sc4","p":{"p":"p2"}}]}}""")

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_RelayId").as[Int]) should be(Vector(4))
  }

  "P!->C PM->SC relation without backrelations" should "work when deleting the parent marked cascading" in {
    //         P
    //       /   \      not a real circle since from the children there are no backrelations to the parent
    //      C  -  SC

    val project = SchemaDsl() { schema =>
      val parent    = schema.model("P").field_!("p", _.String, isUnique = true)
      val child     = schema.model("C").field_!("c", _.String, isUnique = true)
      val stepChild = schema.model("SC").field_!("sc", _.String, isUnique = true)

      parent.oneToOneRelation_!("c", "doesNotMatter", child, modelAOnDelete = OnDelete.Cascade, includeFieldB = false)
      parent.oneToManyRelation("scs", "doesNotMatter", stepChild, modelAOnDelete = OnDelete.Cascade, includeFieldB = false)
      child.oneToOneRelation("sc", "c", stepChild, modelAOnDelete = OnDelete.Cascade)
    }

    database.setup(project)

    server.executeQuerySimple("""mutation{createC(data:{c:"c", sc: {create:{sc: "sc"}}}){c, sc{sc}}}""", project)
    server.executeQuerySimple("""mutation{createC(data:{c:"c2", sc: {create:{sc: "sc2"}}}){c, sc{sc}}}""", project)
    server.executeQuerySimple("""mutation{createP(data:{p:"p", c: {connect:{c: "c"}}, scs: {connect:[{sc: "sc"},{sc: "sc2"}]}}){p, c {c}, scs{sc}}}""", project)

    server.executeQuerySimple("""mutation{deleteP(where: {p:"p"}){id}}""", project)

    server.executeQuerySimple("""query{ps{p, c {c}, scs {sc}}}""", project).toString should be("""{"data":{"ps":[]}}""")
    server.executeQuerySimple("""query{cs{c}}""", project).toString should be("""{"data":{"cs":[{"c":"c2"}]}}""")
    server.executeQuerySimple("""query{sCs{sc}}""", project).toString should be("""{"data":{"sCs":[]}}""")

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_RelayId").as[Int]) should be(Vector(1))
  }

  "A path that is interrupted since there are nodes missing" should "only cascade up until the gap" in {
    //         P-C-GC-|-D-E
    val project = SchemaDsl() { schema =>
      val parent     = schema.model("P").field_!("p", _.String, isUnique = true)
      val child      = schema.model("C").field_!("c", _.String, isUnique = true)
      val grandChild = schema.model("GC").field_!("gc", _.String, isUnique = true)
      val levelD     = schema.model("D").field_!("d", _.String, isUnique = true)
      val levelE     = schema.model("E").field_!("e", _.String, isUnique = true)

      child.oneToOneRelation_!("p", "c", parent, modelBOnDelete = OnDelete.Cascade)
      grandChild.oneToOneRelation_!("c", "gc", child, modelBOnDelete = OnDelete.Cascade)
      levelD.manyToManyRelation("gc", "d", grandChild, modelBOnDelete = OnDelete.Cascade)
      levelE.manyToManyRelation("d", "e", levelD, modelBOnDelete = OnDelete.Cascade)
    }
    database.setup(project)

    server.executeQuerySimple("""mutation{createP(data:{p:"p", c: {create:{c: "c", gc :{create:{gc: "gc"}}}}}){p, c {c, gc{gc}}}}""", project)
    server.executeQuerySimple("""mutation{createP(data:{p:"p2", c: {create:{c: "c2", gc :{create:{gc: "gc2"}}}}}){p, c {c,gc{gc}}}}""", project)

    server.executeQuerySimple("""mutation{createD(data:{d:"d", e: {create:[{e: "e"}]}}){d}}""", project)

    server.executeQuerySimple("""mutation{deleteP(where: {p:"p"}){id}}""", project)
    server.executeQuerySimple("""query{ps{p, c {c}}}""", project).toString should be("""{"data":{"ps":[{"p":"p2","c":{"c":"c2"}}]}}""")
    server.executeQuerySimple("""query{cs{c, p {p}, gc {gc}}}""", project).toString should be(
      """{"data":{"cs":[{"c":"c2","p":{"p":"p2"},"gc":{"gc":"gc2"}}]}}""")
    server.executeQuerySimple("""query{gCs{gc, c {c}, d {d}}}""", project).toString should be("""{"data":{"gCs":[{"gc":"gc2","c":{"c":"c2"},"d":[]}]}}""")
    server.executeQuerySimple("""query{ds{d, gc {gc},e {e}}}""", project).toString should be("""{"data":{"ds":[{"d":"d","gc":[],"e":[{"e":"e"}]}]}}""")
    server.executeQuerySimple("""query{es{e, d {d}}}""", project).toString should be("""{"data":{"es":[{"e":"e","d":[{"d":"d"}]}]}}""")

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_RelayId").as[Int]) should be(Vector(5))
  }

  "A deep uninterrupted path" should "cascade all the way down" in {
    //         P-C-GC-D-E
    val project = SchemaDsl() { schema =>
      val parent     = schema.model("P").field_!("p", _.String, isUnique = true)
      val child      = schema.model("C").field_!("c", _.String, isUnique = true)
      val grandChild = schema.model("GC").field_!("gc", _.String, isUnique = true)
      val levelD     = schema.model("D").field_!("d", _.String, isUnique = true)
      val levelE     = schema.model("E").field_!("e", _.String, isUnique = true)

      child.oneToOneRelation_!("p", "c", parent, modelBOnDelete = OnDelete.Cascade)
      grandChild.oneToOneRelation_!("c", "gc", child, modelBOnDelete = OnDelete.Cascade)
      levelD.manyToManyRelation("gc", "d", grandChild, modelBOnDelete = OnDelete.Cascade)
      levelE.manyToManyRelation("d", "e", levelD, modelBOnDelete = OnDelete.Cascade)
    }
    database.setup(project)

    server.executeQuerySimple("""mutation{createP(data:{p:"p", c: {create:{c: "c", gc :{create:{gc: "gc"}}}}}){p, c {c, gc{gc}}}}""", project)
    server.executeQuerySimple("""mutation{createP(data:{p:"p2", c: {create:{c: "c2", gc :{create:{gc: "gc2"}}}}}){p, c {c,gc{gc}}}}""", project)

    server.executeQuerySimple("""mutation{createD(data:{d:"d", e: {create:[{e: "e"}]}, gc: {connect:{gc: "gc"}}}){d}}""", project)

    server.executeQuerySimple("""mutation{deleteP(where: {p:"p"}){id}}""", project)
    server.executeQuerySimple("""query{ps{p, c {c}}}""", project).toString should be("""{"data":{"ps":[{"p":"p2","c":{"c":"c2"}}]}}""")
    server.executeQuerySimple("""query{cs{c, p {p}, gc {gc}}}""", project).toString should be(
      """{"data":{"cs":[{"c":"c2","p":{"p":"p2"},"gc":{"gc":"gc2"}}]}}""")
    server.executeQuerySimple("""query{gCs{gc, c {c}, d {d}}}""", project).toString should be("""{"data":{"gCs":[{"gc":"gc2","c":{"c":"c2"},"d":[]}]}}""")
    server.executeQuerySimple("""query{ds{d, gc {gc},e {e}}}""", project).toString should be("""{"data":{"ds":[]}}""")
    server.executeQuerySimple("""query{es{e, d {d}}}""", project).toString should be("""{"data":{"es":[]}}""")

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_RelayId").as[Int]) should be(Vector(3))
  }

  "A deep uninterrupted path" should "error on a required relation violation at the end" in {
    //         P-C-GC-D-E-F!
    val project = SchemaDsl() { schema =>
      val parent     = schema.model("P").field_!("p", _.String, isUnique = true)
      val child      = schema.model("C").field_!("c", _.String, isUnique = true)
      val grandChild = schema.model("GC").field_!("gc", _.String, isUnique = true)
      val levelD     = schema.model("D").field_!("d", _.String, isUnique = true)
      val levelE     = schema.model("E").field_!("e", _.String, isUnique = true)
      val levelF     = schema.model("F").field_!("f", _.String, isUnique = true)

      child.oneToOneRelation_!("p", "c", parent, modelBOnDelete = OnDelete.Cascade)
      grandChild.oneToOneRelation_!("c", "gc", child, modelBOnDelete = OnDelete.Cascade)
      levelD.manyToManyRelation("gc", "d", grandChild, modelBOnDelete = OnDelete.Cascade)
      levelE.manyToManyRelation("d", "e", levelD, modelBOnDelete = OnDelete.Cascade)
      levelF.oneToOneRelation_!("e", "f", levelE)
    }
    database.setup(project)

    server.executeQuerySimple("""mutation{createP(data:{p:"p", c: {create:{c: "c", gc :{create:{gc: "gc"}}}}}){p, c {c, gc{gc}}}}""", project)
    server.executeQuerySimple("""mutation{createP(data:{p:"p2", c: {create:{c: "c2", gc :{create:{gc: "gc2"}}}}}){p, c {c,gc{gc}}}}""", project)
    server.executeQuerySimple("""mutation{createD(data:{d:"d", e: {create:[{e: "e", f: {create :{f:"f"}}}]}, gc: {connect:{gc: "gc"}}}){d}}""", project)

    server.executeQuerySimpleThatMustFail(
      """mutation{deleteP(where: {p:"p"}){id}}""",
      project,
      errorCode = 3042,
      errorContains = """The change you are trying to make would violate the required relation '_FToE' between F and E"""
    )

    server.executeQuerySimple("""query{ps{p, c {c}}}""", project).toString should be("""{"data":{"ps":[{"p":"p","c":{"c":"c"}},{"p":"p2","c":{"c":"c2"}}]}}""")
    server.executeQuerySimple("""query{cs{c, p {p}, gc {gc}}}""", project).toString should be(
      """{"data":{"cs":[{"c":"c","p":{"p":"p"},"gc":{"gc":"gc"}},{"c":"c2","p":{"p":"p2"},"gc":{"gc":"gc2"}}]}}""")
    server.executeQuerySimple("""query{gCs{gc, c {c}, d {d}}}""", project).toString should be(
      """{"data":{"gCs":[{"gc":"gc","c":{"c":"c"},"d":[{"d":"d"}]},{"gc":"gc2","c":{"c":"c2"},"d":[]}]}}""")
    server.executeQuerySimple("""query{ds{d, gc {gc},e {e}}}""", project).toString should be(
      """{"data":{"ds":[{"d":"d","gc":[{"gc":"gc"}],"e":[{"e":"e"}]}]}}""")
    server.executeQuerySimple("""query{fs{f, e {e}}}""", project).toString should be("""{"data":{"fs":[{"f":"f","e":{"e":"e"}}]}}""")

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_RelayId").as[Int]) should be(Vector(9))
  }

  "A required relation violation anywhere on the path" should "error and roll back all of the changes" in {

    /**           A           If cascading all the way down to D from A is fine, but deleting C would
      *          /            violate a required relation on E that is not cascading then this should
      *         B             error and not delete anything.
      *          \
      *          C . E
      *          /
      *         D
      */
    val project = SchemaDsl() { schema =>
      val a = schema.model("A").field_!("a", _.DateTime, isUnique = true)
      val b = schema.model("B").field_!("b", _.DateTime, isUnique = true)
      val c = schema.model("C").field_!("c", _.DateTime, isUnique = true)
      val d = schema.model("D").field_!("d", _.DateTime, isUnique = true)
      val e = schema.model("E").field_!("e", _.DateTime, isUnique = true)

      a.oneToOneRelation_!("b", "a", b, modelAOnDelete = OnDelete.Cascade)
      b.oneToOneRelation_!("c", "b", c, modelAOnDelete = OnDelete.Cascade)
      c.manyToManyRelation("d", "c", d, modelAOnDelete = OnDelete.Cascade)
      c.oneToOneRelation_!("e", "c", e)
    }
    database.setup(project)

    server.executeQuerySimple("""mutation{createA(data:{a:"2020", b: {create:{b: "2021", c :{create:{c: "2022", e: {create:{e: "2023"}}}}}}}){a}}""", project)
    server.executeQuerySimple("""mutation{createA(data:{a:"2030", b: {create:{b: "2031", c :{create:{c: "2032", e: {create:{e: "2033"}}}}}}}){a}}""", project)

    server.executeQuerySimple("""mutation{updateC(where: {c: "2022"}, data:{d: {create:[{d: "2024"},{d: "2025"}] }}){c}}""", project)
    server.executeQuerySimple("""mutation{updateC(where: {c: "2032"}, data:{d: {create:[{d: "2034"},{d: "2035"}] }}){c}}""", project)

    server.executeQuerySimpleThatMustFail(
      """mutation{deleteA(where: {a:"2020"}){a}}""",
      project,
      errorCode = 3042,
      errorContains = "The change you are trying to make would violate the required relation '_CToE' between C and E"
    )
  }

  "Several relations between the same model" should "be handled correctly" in {

    /**           A           If there are two relations between B and C and only one of them is marked
      *          /            cascading, then only the nodes connected to C's which are connected to B
      *         B             by this relations should be deleted.
      *        /  :
      *       Cs   C
      *        \ /
      *         D
      */
    val project = SchemaDsl() { schema =>
      val a = schema.model("A").field_!("a", _.Float, isUnique = true)
      val b = schema.model("B").field_!("b", _.Float, isUnique = true)
      val c = schema.model("C").field_!("c", _.Float, isUnique = true)
      val d = schema.model("D").field_!("d", _.Float, isUnique = true)

      a.oneToOneRelation("b", "a", b, modelAOnDelete = OnDelete.Cascade)
      b.manyToManyRelation("cs", "bs", c, modelAOnDelete = OnDelete.Cascade, relationName = Some("Relation1"))
      c.manyToManyRelation("d", "c", d, modelAOnDelete = OnDelete.Cascade, relationName = Some("Relation2"))
      b.oneToOneRelation("c", "b", c)
    }
    database.setup(project)

    server.executeQuerySimple("""mutation{createA(data:{a: 10.10, b: {create:{b: 11.11}}}){a}}""", project)

    server.executeQuerySimple("""mutation{updateB(where: {b: 11.11}, data:{cs: {create:[{c: 12.12},{c: 12.13}]}}){b}}""", project)
    server.executeQuerySimple("""mutation{updateB(where: {b: 11.11}, data:{c: {create:{c: 12.99}}}){b}}""", project)

    server.executeQuerySimple("""mutation{updateC(where: {c: 12.12}, data:{d: {create:{d: 13.13}}}){c}}""", project)
    server.executeQuerySimple("""mutation{updateC(where: {c: 12.99}, data:{d: {create:{d: 13.99}}}){c}}""", project)

    server.executeQuerySimple("""mutation{deleteA(where: {a:10.10}){a}}""", project)

    server.executeQuerySimple("""query{as{a, b {b}}}""", project).toString should be("""{"data":{"as":[]}}""")
    server.executeQuerySimple("""query{bs{b, c {c}, cs {c}}}""", project).toString should be("""{"data":{"bs":[]}}""")
    server.executeQuerySimple("""query{cs{c, d {d}}}""", project).toString should be("""{"data":{"cs":[{"c":12.99,"d":[{"d":13.99}]}]}}""")
    server.executeQuerySimple("""query{ds{d}}""", project).toString should be("""{"data":{"ds":[{"d":13.99}]}}""")
  }
  //endregion

  //region  NESTED DELETE

  "NESTING P1!-C1! relation deleting the parent" should "work if parent is marked cascading but error on returning previous values" in {
    //         P-C
    val project = SchemaDsl() { schema =>
      val parent = schema.model("P").field_!("p", _.String, isUnique = true)
      val child  = schema.model("C").field_!("c", _.String, isUnique = true)

      child.oneToOneRelation_!("p", "c", parent, modelBOnDelete = OnDelete.Cascade)
    }
    database.setup(project)

    server.executeQuerySimple("""mutation{createP(data:{p:"p", c: {create:{c: "c"}}}){p, c {c}}}""", project)
    server.executeQuerySimple("""mutation{createP(data:{p:"p2", c: {create:{c: "c2"}}}){p, c {c}}}""", project)

    server.executeQuerySimpleThatMustFail("""mutation{updateC(where: {c:"c"} data: {p: {delete:{p:"P"}}}){id}}""",
                                          project,
                                          errorCode = 3039,
                                          errorContains = "No Node for the model")
    server.executeQuerySimple("""query{ps{p, c {c}}}""", project).toString should be("""{"data":{"ps":[{"p":"p2","c":{"c":"c2"}}]}}""")
    server.executeQuerySimple("""query{cs{c, p {p}}}""", project).toString should be("""{"data":{"cs":[{"c":"c2","p":{"p":"p2"}}]}}""")
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_RelayId").as[Int]) should be(Vector(2))
  }

  "P1-C1-C1!-GC! relation updating the parent to delete the child and grandchild if marked cascading" should "work" in {
    //         P-C-GC
    val project = SchemaDsl() { schema =>
      val parent     = schema.model("P").field_!("p", _.String, isUnique = true)
      val child      = schema.model("C").field_!("c", _.String, isUnique = true)
      val grandChild = schema.model("GC").field_!("gc", _.String, isUnique = true)

      grandChild.oneToOneRelation_!("c", "gc", child, modelBOnDelete = OnDelete.Cascade)
      child.oneToOneRelation("p", "c", parent)
    }
    database.setup(project)

    server.executeQuerySimple("""mutation{createP(data:{p:"p", c: {create:{c: "c", gc :{create:{gc: "gc"}}}}}){p, c {c, gc{gc}}}}""", project)
    server.executeQuerySimple("""mutation{createP(data:{p:"p2", c: {create:{c: "c2", gc :{create:{gc: "gc2"}}}}}){p, c {c,gc{gc}}}}""", project)

    server.executeQuerySimple("""mutation{updateP(where: {p:"p"}, data: { c: {delete:{c:"c"}}}){id}}""", project)

    server.executeQuerySimple("""query{ps{p, c {c, gc{gc}}}}""", project).toString should be(
      """{"data":{"ps":[{"p":"p","c":null},{"p":"p2","c":{"c":"c2","gc":{"gc":"gc2"}}}]}}""")
    server.executeQuerySimple("""query{cs{c, gc{gc}, p {p}}}""", project).toString should be(
      """{"data":{"cs":[{"c":"c2","gc":{"gc":"gc2"},"p":{"p":"p2"}}]}}""")
    server.executeQuerySimple("""query{gCs{gc, c {c, p{p}}}}""", project).toString should be(
      """{"data":{"gCs":[{"gc":"gc2","c":{"c":"c2","p":{"p":"p2"}}}]}}""")

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_RelayId").as[Int]) should be(Vector(4))
  }

  "P1!-C1!-C1!-GC! relation updating the parent to delete the child and grandchild if marked cascading" should "error if the child is required on parent" in {
    //         P-C-GC
    val project = SchemaDsl() { schema =>
      val parent     = schema.model("P").field_!("p", _.String, isUnique = true)
      val child      = schema.model("C").field_!("c", _.String, isUnique = true)
      val grandChild = schema.model("GC").field_!("gc", _.String, isUnique = true)

      grandChild.oneToOneRelation_!("c", "gc", child, modelBOnDelete = OnDelete.Cascade)
      child.oneToOneRelation_!("p", "c", parent)
    }
    database.setup(project)

    server.executeQuerySimple("""mutation{createP(data:{p:"p", c: {create:{c: "c", gc :{create:{gc: "gc"}}}}}){p, c {c, gc{gc}}}}""", project)
    server.executeQuerySimple("""mutation{createP(data:{p:"p2", c: {create:{c: "c2", gc :{create:{gc: "gc2"}}}}}){p, c {c,gc{gc}}}}""", project)

    server.executeQuerySimpleThatMustFail(
      """mutation{updateP(where: {p:"p"}, data: { c: {delete:{c:"c"}}}){id}}""",
      project,
      errorCode = 3042,
      errorContains = "The change you are trying to make would violate the required relation '_CToP' between C and P"
    )

    server.executeQuerySimple("""query{ps{p, c {c, gc{gc}}}}""", project).toString should be(
      """{"data":{"ps":[{"p":"p","c":{"c":"c","gc":{"gc":"gc"}}},{"p":"p2","c":{"c":"c2","gc":{"gc":"gc2"}}}]}}""")
    server.executeQuerySimple("""query{cs{c, gc{gc}, p {p}}}""", project).toString should be(
      """{"data":{"cs":[{"c":"c","gc":{"gc":"gc"},"p":{"p":"p"}},{"c":"c2","gc":{"gc":"gc2"},"p":{"p":"p2"}}]}}""")
    server.executeQuerySimple("""query{gCs{gc, c {c, p{p}}}}""", project).toString should be(
      """{"data":{"gCs":[{"gc":"gc","c":{"c":"c","p":{"p":"p"}}},{"gc":"gc2","c":{"c":"c2","p":{"p":"p2"}}}]}}""")

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_RelayId").as[Int]) should be(Vector(6))
  }
  //endregion
}
