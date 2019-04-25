package com.prisma.api.mutations

import akka.http.scaladsl.settings.ParserSettings.IllegalResponseHeaderValueProcessingMode.Ignore
import com.prisma.IgnoreSQLite
import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability._
import com.prisma.shared.models._
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class CascadingDeleteSpec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities  = Set(JoinRelationLinksCapability)
  override def doNotRunForCapabilities = Set(EmbeddedTypesCapability)

  //region  TOP LEVEL DELETE

  "P1!-C1! relation deleting the parent" should "work if parent is marked marked cascading" taggedAs IgnoreSQLite in { // TODO: Remove SQLite ignore when cascading again
    //         P-C
    val project = SchemaDsl.fromStringV11() {
      """
        |type P {
        |  id: ID! @id
        |  p: String! @unique
        |  c: C! @relation(onDelete: CASCADE link: INLINE)
        |}
        |
        |type C {
        |  id: ID! @id
        |  c: String! @unique
        |  p: P!
        |}
      """.stripMargin
    }
    database.setup(project)

    server.query("""mutation{createP(data:{p:"p", c: {create:{c: "c"}}}){p, c {c}}}""", project)
    server.query("""mutation{createP(data:{p:"p2", c: {create:{c: "c2"}}}){p, c {c}}}""", project)

    server.query("""mutation{deleteP(where: {p:"p"}){id}}""", project)
    server.query("""query{ps{p, c {c}}}""", project).toString should be("""{"data":{"ps":[{"p":"p2","c":{"c":"c2"}}]}}""")
    server.query("""query{cs{c, p {p}}}""", project).toString should be("""{"data":{"cs":[{"c":"c2","p":{"p":"p2"}}]}}""")

  }

  "PM-CM relation deleting the parent" should "delete all children if the parent is marked cascading" taggedAs IgnoreSQLite in { // TODO: Remove SQLite ignore when cascading again
    //         P-C
    val project = SchemaDsl.fromStringV11() {
      """
        |type P {
        |  id: ID! @id
        |  p: String! @unique
        |  c: [C] @relation(onDelete: CASCADE)
        |}
        |
        |type C {
        |  id: ID! @id
        |  c: String! @unique
        |  p: [P]
        |}
      """.stripMargin
    }
    database.setup(project)

    server.query("""mutation{createP(data:{p:"p",  c: {create:[{c: "c"},  {c: "c2"}]}}){p, c {c}}}""", project)
    server.query("""mutation{createP(data:{p:"p2", c: {create:[{c: "cx"}, {c: "cx2"}]}}){p, c {c}}}""", project)
    server.query("""mutation{updateC(where:{c:"c2"}, data:{p: {create:{p: "pz"}}}){id}}""", project)

    server.query("""mutation{deleteP(where: {p:"p"}){id}}""", project)
    server.query("""query{ps{p, c {c}}}""", project).toString should be("""{"data":{"ps":[{"p":"p2","c":[{"c":"cx"},{"c":"cx2"}]},{"p":"pz","c":[]}]}}""")
    server.query("""query{cs{c, p {p}}}""", project).toString should be("""{"data":{"cs":[{"c":"cx","p":[{"p":"p2"}]},{"c":"cx2","p":[{"p":"p2"}]}]}}""")

  }

  "PM-CM relation deleting the parent" should "succeed if both sides are marked cascading although that is a circle" taggedAs IgnoreSQLite in { // TODO: Remove SQLite ignore when cascading again
    //         P-C
    val project = SchemaDsl.fromStringV11() {
      """
        |type P {
        |  id: ID! @id
        |  p: String! @unique
        |  c: [C] @relation(onDelete: CASCADE)
        |}
        |
        |type C {
        |  id: ID! @id
        |  c: String! @unique
        |  p: [P] @relation(onDelete: CASCADE)
        |}
      """.stripMargin
    }
    database.setup(project)

    server.query("""mutation{createP(data:{p:"p",  c: {create:[{c: "c"},  {c: "c2"}]}}){p, c {c}}}""", project)
    server.query("""mutation{updateC(where:{c:"c2"}, data:{p: {create:{p: "pz"}}}){id}}""", project)

    server.query("""mutation{deleteP(where: {p:"p"}){id}}""", project)
    server.query("""query{ps{p, c {c}}}""", project).toString should be("""{"data":{"ps":[]}}""")

  }

  "P1!-C1! relation deleting the parent" should "work if both sides are marked marked cascading" taggedAs IgnoreSQLite in { // TODO: Remove SQLite ignore when cascading again
    //         P-C
    val project = SchemaDsl.fromStringV11() {
      """
        |type P {
        |  id: ID! @id
        |  p: String! @unique
        |  c: C! @relation(onDelete: CASCADE link: INLINE)
        |}
        |
        |type C {
        |  id: ID! @id
        |  c: String! @unique
        |  p: P! @relation(onDelete: CASCADE)
        |}
      """.stripMargin
    }
    database.setup(project)

    server.query("""mutation{createP(data:{p:"p", c: {create:{c: "c"}}}){p, c {c}}}""", project)

    server.query("""mutation{deleteP(where: {p:"p"}){id}}""", project)
    server.query("""query{ps{p, c {c}}}""", project).toString should be("""{"data":{"ps":[]}}""")
    server.query("""query{cs{c}}""", project).toString should be("""{"data":{"cs":[]}}""")

  }

  "P1!-C1! relation deleting the parent" should "error if only child is marked marked cascading" taggedAs IgnoreSQLite in { // TODO: Remove SQLite ignore when cascading again
    //         P-C
    val project = SchemaDsl.fromStringV11() {
      """
        |type P {
        |  id: ID! @id
        |  p: String! @unique
        |  c: C! @relation(link: INLINE)
        |}
        |
        |type C {
        |  id: ID! @id
        |  c: String! @unique
        |  p: P! @relation(onDelete: CASCADE)
        |}
      """.stripMargin
    }
    database.setup(project)

    server.query("""mutation{createP(data:{p:"p", c: {create:{c: "c"}}}){p, c {c}}}""", project)
    server.query("""mutation{createP(data:{p:"p2", c: {create:{c: "c2"}}}){p, c {c}}}""", project)

    server.queryThatMustFail("""mutation{deleteP(where: {p:"p"}){id}}""", project, errorCode = 3042)
    server.query("""query{ps{p, c {c}}}""", project).toString should be("""{"data":{"ps":[{"p":"p","c":{"c":"c"}},{"p":"p2","c":{"c":"c2"}}]}}""")
    server.query("""query{cs{c, p {p}}}""", project).toString should be("""{"data":{"cs":[{"c":"c","p":{"p":"p"}},{"c":"c2","p":{"p":"p2"}}]}}""")

  }

  "P1!-C1!-C1!-GC! relation deleting the parent and child and grandchild if marked cascading" should "work" taggedAs IgnoreSQLite in { // TODO: Remove SQLite ignore when cascading again
    //         P-C-GC
    val project = SchemaDsl.fromStringV11() {
      """
        |type P {
        |  id: ID! @id
        |  p: String! @unique
        |  c: C! @relation(onDelete: CASCADE link: INLINE)
        |}
        |
        |type C {
        |  id: ID! @id
        |  c: String! @unique
        |  p: P!
        |  gc: GC! @relation(onDelete: CASCADE link: INLINE)
        |}
        |
        |type GC {
        |  id: ID! @id
        |  gc: String! @unique
        |  c: C!
        |}
      """.stripMargin
    }
    database.setup(project)

    server.query("""mutation{createP(data:{p:"p", c: {create:{c: "c", gc :{create:{gc: "gc"}}}}}){p, c {c, gc{gc}}}}""", project)
    server.query("""mutation{createP(data:{p:"p2", c: {create:{c: "c2", gc :{create:{gc: "gc2"}}}}}){p, c {c,gc{gc}}}}""", project)

    server.query("""mutation{deleteP(where: {p:"p"}){id}}""", project)

    server.query("""query{ps{p, c {c, gc{gc}}}}""", project).toString should be("""{"data":{"ps":[{"p":"p2","c":{"c":"c2","gc":{"gc":"gc2"}}}]}}""")
    server.query("""query{cs{c, gc{gc}, p {p}}}""", project).toString should be("""{"data":{"cs":[{"c":"c2","gc":{"gc":"gc2"},"p":{"p":"p2"}}]}}""")
    server.query("""query{gCs{gc, c {c, p{p}}}}""", project).toString should be("""{"data":{"gCs":[{"gc":"gc2","c":{"c":"c2","p":{"p":"p2"}}}]}}""")

  }

  "P1!-C1!-C1-GC relation deleting the parent and child marked cascading" should "work but preserve the grandchild" taggedAs IgnoreSQLite in { // TODO: Remove SQLite ignore when cascading again
    //         P-C-GC
    val project = SchemaDsl.fromStringV11() {
      """
        |type P {
        |  id: ID! @id
        |  p: String! @unique
        |  c: C! @relation(onDelete: CASCADE link: INLINE)
        |}
        |
        |type C {
        |  id: ID! @id
        |  c: String! @unique
        |  p: P!
        |  gc: GC @relation(link: INLINE)
        |}
        |
        |type GC {
        |  id: ID! @id
        |  gc: String! @unique
        |  c: C
        |}
      """.stripMargin
    }

    database.setup(project)

    server.query("""mutation{createP(data:{p:"p", c: {create:{c: "c", gc :{create:{gc: "gc"}}}}}){p, c {c, gc{gc}}}}""", project)
    server.query("""mutation{createP(data:{p:"p2", c: {create:{c: "c2", gc :{create:{gc: "gc2"}}}}}){p, c {c,gc{gc}}}}""", project)

    server.query("""mutation{deleteP(where: {p:"p"}){id}}""", project)

    server.query("""query{ps{p, c {c, gc{gc}}}}""", project).toString should be("""{"data":{"ps":[{"p":"p2","c":{"c":"c2","gc":{"gc":"gc2"}}}]}}""")
    server.query("""query{cs{c, gc{gc}, p {p}}}""", project).toString should be("""{"data":{"cs":[{"c":"c2","gc":{"gc":"gc2"},"p":{"p":"p2"}}]}}""")
    server.query("""query{gCs{gc, c {c, p{p}}}}""", project).toString should be(
      """{"data":{"gCs":[{"gc":"gc","c":null},{"gc":"gc2","c":{"c":"c2","p":{"p":"p2"}}}]}}""")

  }

  "P1!-C1! relation deleting the parent marked cascading" should "error if the child is required in another non-cascading relation" taggedAs IgnoreSQLite in { // TODO: Remove SQLite ignore when cascading again
    //         P-C-GC
    val project = SchemaDsl.fromStringV11() {
      """
        |type P {
        |  id: ID! @id
        |  p: String! @unique
        |  c: C! @relation(onDelete: CASCADE link: INLINE)
        |}
        |
        |type C {
        |  id: ID! @id
        |  c: String! @unique
        |  p: P!
        |  gc: GC! @relation(link: INLINE)
        |}
        |
        |type GC {
        |  id: ID! @id
        |  gc: String! @unique
        |  c: C!
        |}
      """.stripMargin
    }
    database.setup(project)

    server.query("""mutation{createP(data:{p:"p", c: {create:{c: "c", gc :{create:{gc: "gc"}}}}}){p, c {c, gc{gc}}}}""", project)
    server.query("""mutation{createP(data:{p:"p2", c: {create:{c: "c2", gc :{create:{gc: "gc2"}}}}}){p, c {c,gc{gc}}}}""", project)

    server.queryThatMustFail("""mutation{deleteP(where: {p:"p"}){id}}""", project, errorCode = 3042)
    server.query("""query{ps{p, c {c}}}""", project).toString should be("""{"data":{"ps":[{"p":"p","c":{"c":"c"}},{"p":"p2","c":{"c":"c2"}}]}}""")
    server.query("""query{cs{c, p {p}}}""", project).toString should be("""{"data":{"cs":[{"c":"c","p":{"p":"p"}},{"c":"c2","p":{"p":"p2"}}]}}""")

  }

  "If the parent is not cascading nothing on the path" should "be deleted except for the parent" taggedAs IgnoreSQLite in { // TODO: Remove SQLite ignore when cascading again
    //         P-C-GC
    val project = SchemaDsl.fromStringV11() {
      """
        |type P {
        |  id: ID! @id
        |  p: String! @unique
        |  c: C
        |}
        |
        |type C {
        |  id: ID! @id
        |  c: String! @unique
        |  p: P @relation(onDelete: CASCADE link: INLINE)
        |  gc: GC @relation(link: INLINE)
        |}
        |
        |type GC {
        |  id: ID! @id
        |  gc: String! @unique
        |  c: C
        |}
      """.stripMargin
    }
    database.setup(project)

    server.query("""mutation{createP(data:{p:"p", c: {create:{c: "c", gc :{create:{gc: "gc"}}}}}){p, c {c, gc{gc}}}}""", project)

    server.query("""mutation{deleteP(where: {p:"p"}){id}}""", project)
    server.query("""query{ps{p, c {c}}}""", project).toString should be("""{"data":{"ps":[]}}""")
    server.query("""query{cs{c, p {p}}}""", project).toString should be("""{"data":{"cs":[{"c":"c","p":null}]}}""")
    server.query("""query{gCs{gc, c {c}}}""", project).toString should be("""{"data":{"gCs":[{"gc":"gc","c":{"c":"c"}}]}}""")

  }

  "P1!-C1! PM-SC1! relation deleting the parent marked cascading" should "work" taggedAs IgnoreSQLite in { // TODO: Remove SQLite ignore when cascading again
    //         P
    //       /   \
    //      C     SC
    val project = SchemaDsl.fromStringV11() {
      """
        |type P {
        |  id: ID! @id
        |  p: String! @unique
        |  c: C @relation(onDelete: CASCADE link: INLINE)
        |  scs: [SC] @relation(onDelete: CASCADE)
        |}
        |
        |type C {
        |  id: ID! @id
        |  c: String! @unique
        |  p: P
        |}
        |
        |type SC {
        |  id: ID! @id
        |  sc: String! @unique
        |  p:P!
        |}
      """.stripMargin
    }

    database.setup(project)

    server.query("""mutation{createP(data:{p:"p", c: {create:{c: "c"}}, scs: {create:[{sc: "sc1"},{sc: "sc2"}]}}){p, c {c},scs{sc}}}""", project)
    server.query("""mutation{createP(data:{p:"p2", c: {create:{c: "c2"}}, scs: {create:[{sc: "sc3"},{sc: "sc4"}]}}){p, c {c},scs{sc}}}""", project)

    server.query("""mutation{deleteP(where: {p:"p"}){id}}""", project)

    server.query("""query{ps{p, c {c}, scs {sc}}}""", project).toString should be(
      """{"data":{"ps":[{"p":"p2","c":{"c":"c2"},"scs":[{"sc":"sc3"},{"sc":"sc4"}]}]}}""")
    server.query("""query{cs{c, p {p}}}""", project).toString should be("""{"data":{"cs":[{"c":"c2","p":{"p":"p2"}}]}}""")
    server.query("""query{sCs{sc,  p{p}}}""", project).toString should be("""{"data":{"sCs":[{"sc":"sc3","p":{"p":"p2"}},{"sc":"sc4","p":{"p":"p2"}}]}}""")

  }

  "P!->C PM->SC relation without backrelations" should "work when deleting the parent marked cascading" taggedAs IgnoreSQLite in { // TODO: Remove SQLite ignore when cascading again
    //         P
    //       /   \      not a real circle since from the children there are no backrelations to the parent
    //      C  -  SC
    val project = SchemaDsl.fromStringV11() {
      """
        |type P {
        |  id: ID! @id
        |  p: String! @unique
        |  c: C! @relation(onDelete: CASCADE link: INLINE)
        |  scs: [SC] @relation(onDelete: CASCADE)
        |}
        |
        |type C {
        |  id: ID! @id
        |  c: String! @unique
        |  p: P
        |  sc: SC @relation(onDelete: CASCADE link: INLINE)
        |}
        |
        |type SC {
        |  id: ID! @id
        |  sc: String! @unique
        |  p:P
        |  c: C
        |}
      """.stripMargin
    }

    database.setup(project)

    server.query("""mutation{createC(data:{c:"c", sc: {create:{sc: "sc"}}}){c, sc{sc}}}""", project)
    server.query("""mutation{createC(data:{c:"c2", sc: {create:{sc: "sc2"}}}){c, sc{sc}}}""", project)
    server.query("""mutation{createP(data:{p:"p", c: {connect:{c: "c"}}, scs: {connect:[{sc: "sc"},{sc: "sc2"}]}}){p, c {c}, scs{sc}}}""", project)

    server.query("""mutation{deleteP(where: {p:"p"}){id}}""", project)

    server.query("""query{ps{p, c {c}, scs {sc}}}""", project).toString should be("""{"data":{"ps":[]}}""")
    server.query("""query{cs{c}}""", project).toString should be("""{"data":{"cs":[{"c":"c2"}]}}""")
    server.query("""query{sCs{sc}}""", project).toString should be("""{"data":{"sCs":[]}}""")

  }

  "A path that is interrupted since there are nodes missing" should "only cascade up until the gap" taggedAs IgnoreSQLite in { // TODO: Remove SQLite ignore when cascading again
    //         P-C-GC-|-D-E
    val project = SchemaDsl.fromStringV11() {
      """
        |type P {
        |  id: ID! @id
        |  p: String! @unique
        |  c: C! @relation(onDelete: CASCADE link: INLINE)
        |}
        |
        |type C {
        |  id: ID! @id
        |  c: String! @unique
        |  p: P!
        |  gc: GC! @relation(onDelete: CASCADE link: INLINE)
        |}
        |
        |type GC {
        |  id: ID! @id
        |  gc: String! @unique
        |  c: C!
        |  d: [D] @relation(onDelete: CASCADE)
        |}
        |
        |type D {
        |  id: ID! @id
        |  d: String! @unique
        |  gc: [GC]
        |  e: [E] @relation(onDelete: CASCADE)
        |}
        |
        |type E {
        |  id: ID! @id
        |  e: String! @unique
        |  d: [D]
        |}
      """.stripMargin
    }
    database.setup(project)

    server.query("""mutation{createP(data:{p:"p", c: {create:{c: "c", gc :{create:{gc: "gc"}}}}}){p, c {c, gc{gc}}}}""", project)
    server.query("""mutation{createP(data:{p:"p2", c: {create:{c: "c2", gc :{create:{gc: "gc2"}}}}}){p, c {c,gc{gc}}}}""", project)

    server.query("""mutation{createD(data:{d:"d", e: {create:[{e: "e"}]}}){d}}""", project)

    server.query("""mutation{deleteP(where: {p:"p"}){id}}""", project)
    server.query("""query{ps{p, c {c}}}""", project).toString should be("""{"data":{"ps":[{"p":"p2","c":{"c":"c2"}}]}}""")
    server.query("""query{cs{c, p {p}, gc {gc}}}""", project).toString should be("""{"data":{"cs":[{"c":"c2","p":{"p":"p2"},"gc":{"gc":"gc2"}}]}}""")
    server.query("""query{gCs{gc, c {c}, d {d}}}""", project).toString should be("""{"data":{"gCs":[{"gc":"gc2","c":{"c":"c2"},"d":[]}]}}""")
    server.query("""query{ds{d, gc {gc},e {e}}}""", project).toString should be("""{"data":{"ds":[{"d":"d","gc":[],"e":[{"e":"e"}]}]}}""")
    server.query("""query{es{e, d {d}}}""", project).toString should be("""{"data":{"es":[{"e":"e","d":[{"d":"d"}]}]}}""")

  }

  "A deep uninterrupted path" should "cascade all the way down" taggedAs IgnoreSQLite in { // TODO: Remove SQLite ignore when cascading again
    //         P-C-GC-D-E
    val project = SchemaDsl.fromStringV11() {
      """
        |type P {
        |  id: ID! @id
        |  p: String! @unique
        |  c: C! @relation(onDelete: CASCADE link: INLINE)
        |}
        |
        |type C {
        |  id: ID! @id
        |  c: String! @unique
        |  p: P!
        |  gc: GC! @relation(onDelete: CASCADE link: INLINE)
        |}
        |
        |type GC {
        |  id: ID! @id
        |  gc: String! @unique
        |  c: C!
        |  d: [D] @relation(onDelete: CASCADE)
        |}
        |
        |type D {
        |  id: ID! @id
        |  d: String! @unique
        |  gc: [GC]
        |  e: [E] @relation(onDelete: CASCADE)
        |}
        |
        |type E {
        |  id: ID! @id
        |  e: String! @unique
        |  d: [D]
        |}
      """.stripMargin
    }
    database.setup(project)

    server.query("""mutation{createP(data:{p:"p", c: {create:{c: "c", gc :{create:{gc: "gc"}}}}}){p, c {c, gc{gc}}}}""", project)
    server.query("""mutation{createP(data:{p:"p2", c: {create:{c: "c2", gc :{create:{gc: "gc2"}}}}}){p, c {c,gc{gc}}}}""", project)

    server.query("""mutation{createD(data:{d:"d", e: {create:[{e: "e"}]}, gc: {connect:{gc: "gc"}}}){d}}""", project)

    server.query("""mutation{deleteP(where: {p:"p"}){id}}""", project)
    server.query("""query{ps{p, c {c}}}""", project).toString should be("""{"data":{"ps":[{"p":"p2","c":{"c":"c2"}}]}}""")
    server.query("""query{cs{c, p {p}, gc {gc}}}""", project).toString should be("""{"data":{"cs":[{"c":"c2","p":{"p":"p2"},"gc":{"gc":"gc2"}}]}}""")
    server.query("""query{gCs{gc, c {c}, d {d}}}""", project).toString should be("""{"data":{"gCs":[{"gc":"gc2","c":{"c":"c2"},"d":[]}]}}""")
    server.query("""query{ds{d, gc {gc},e {e}}}""", project).toString should be("""{"data":{"ds":[]}}""")
    server.query("""query{es{e, d {d}}}""", project).toString should be("""{"data":{"es":[]}}""")

  }

  "A deep uninterrupted path" should "error on a required relation violation at the end" taggedAs IgnoreSQLite in { // TODO: Remove SQLite ignore when cascading again
    //         P-C-GC-D-E-F!
    val project = SchemaDsl.fromStringV11() {
      """
        |type P {
        |  id: ID! @id
        |  p: String! @unique
        |  c: C! @relation(onDelete: CASCADE link: INLINE)
        |}
        |
        |type C {
        |  id: ID! @id
        |  c: String! @unique
        |  p: P!
        |  gc: GC! @relation(onDelete: CASCADE link: INLINE)
        |}
        |
        |type GC {
        |  id: ID! @id
        |  gc: String! @unique
        |  c: C!
        |  d: [D] @relation(onDelete: CASCADE)
        |}
        |
        |type D {
        |  id: ID! @id
        |  d: String! @unique
        |  gc: [GC]
        |  e: [E] @relation(onDelete: CASCADE)
        |}
        |
        |type E {
        |  id: ID! @id
        |  e: String! @unique
        |  d: [D]
        |  f: F! @relation(link: INLINE)
        |}
        |
        |type F {
        |  id: ID! @id
        |  f: String! @unique
        |  e: E!
        |}
      """.stripMargin
    }
    database.setup(project)

    server.query("""mutation{createP(data:{p:"p", c: {create:{c: "c", gc :{create:{gc: "gc"}}}}}){p, c {c, gc{gc}}}}""", project)
    server.query("""mutation{createP(data:{p:"p2", c: {create:{c: "c2", gc :{create:{gc: "gc2"}}}}}){p, c {c,gc{gc}}}}""", project)
    server.query("""mutation{createD(data:{d:"d", e: {create:[{e: "e", f: {create :{f:"f"}}}]}, gc: {connect:{gc: "gc"}}}){d}}""", project)

    server.queryThatMustFail(
      """mutation{deleteP(where: {p:"p"}){id}}""",
      project,
      errorCode = 3042,
      errorContains = """The change you are trying to make would violate the required relation 'EToF' between E and F"""
    )

    server.query("""query{ps{p, c {c}}}""", project).toString should be("""{"data":{"ps":[{"p":"p","c":{"c":"c"}},{"p":"p2","c":{"c":"c2"}}]}}""")
    server.query("""query{cs{c, p {p}, gc {gc}}}""", project).toString should be(
      """{"data":{"cs":[{"c":"c","p":{"p":"p"},"gc":{"gc":"gc"}},{"c":"c2","p":{"p":"p2"},"gc":{"gc":"gc2"}}]}}""")
    server.query("""query{gCs{gc, c {c}, d {d}}}""", project).toString should be(
      """{"data":{"gCs":[{"gc":"gc","c":{"c":"c"},"d":[{"d":"d"}]},{"gc":"gc2","c":{"c":"c2"},"d":[]}]}}""")
    server.query("""query{ds{d, gc {gc},e {e}}}""", project).toString should be("""{"data":{"ds":[{"d":"d","gc":[{"gc":"gc"}],"e":[{"e":"e"}]}]}}""")
    server.query("""query{fs{f, e {e}}}""", project).toString should be("""{"data":{"fs":[{"f":"f","e":{"e":"e"}}]}}""")

  }

  "A required relation violation anywhere on the path" should "error and roll back all of the changes" taggedAs IgnoreSQLite in { // TODO: Remove SQLite ignore when cascading again

    /**           A           If cascading all the way down to D from A is fine, but deleting C would
      *          /            violate a required relation on E that is not cascading then this should
      *         B             error and not delete anything.
      *          \
      *          C . E
      *          /
      *         D
      */
    val project = SchemaDsl.fromStringV11() {
      """
        |type A {
        |  id: ID! @id
        |  a: DateTime! @unique
        |  b: B! @relation(onDelete: CASCADE, link: INLINE)
        |}
        |
        |type B {
        |  id: ID! @id
        |  b: DateTime! @unique
        |  a: A!
        |  c: C! @relation(onDelete: CASCADE, link: INLINE)
        |}
        |
        |type C {
        |  id: ID! @id
        |  c: DateTime! @unique
        |  b: B!
        |  d: [D] @relation(onDelete: CASCADE)
        |  e: E! @relation(link: INLINE)
        |}
        |
        |type D {
        |  id: ID! @id
        |  d: DateTime! @unique
        |  c: [C]
        |}
        |
        |type E {
        |  id: ID! @id
        |  e: DateTime! @unique
        |  c: C!
        |}
      """.stripMargin
    }
    database.setup(project)

    server.query("""mutation{createA(data:{a:"2020", b: {create:{b: "2021", c :{create:{c: "2022", e: {create:{e: "2023"}}}}}}}){a}}""", project)
    server.query("""mutation{createA(data:{a:"2030", b: {create:{b: "2031", c :{create:{c: "2032", e: {create:{e: "2033"}}}}}}}){a}}""", project)

    server.query("""mutation{updateC(where: {c: "2022"}, data:{d: {create:[{d: "2024"},{d: "2025"}] }}){c}}""", project)
    server.query("""mutation{updateC(where: {c: "2032"}, data:{d: {create:[{d: "2034"},{d: "2035"}] }}){c}}""", project)

    server.queryThatMustFail(
      """mutation{deleteA(where: {a:"2020"}){a}}""",
      project,
      errorCode = 3042,
      errorContains = "The change you are trying to make would violate the required relation 'CToE' between C and E"
    )
  }

  "A required relation violation on the parent" should "roll back all cascading deletes on the path" taggedAs IgnoreSQLite in { // TODO: Remove SQLite ignore when cascading again

    /**           A           If A!<->D! ia not marked cascading an existing D should cause all the deletes to fail
      *         / | :         even if A<->B, A<->C and C<->E could successfully cascade.
      *        B  C  D
      *          |
      *          E
      */
    val project = SchemaDsl.fromStringV11() {
      """
        |type A {
        |  id: ID! @id
        |  a: String! @unique
        |  d: D! @relation(link: INLINE)
        |  b: B! @relation(onDelete: CASCADE, link: INLINE)
        |  c: [C] @relation(onDelete: CASCADE)
        |}
        |
        |type B {
        |  id: ID! @id
        |  b: String! @unique
        |  a: A!
        |}
        |
        |type C {
        |  id: ID! @id
        |  c: String! @unique
        |  a: [A]
        |  e: E! @relation(onDelete: CASCADE, link: INLINE)
        |}
        |
        |type D {
        |  id: ID! @id
        |  d: String! @unique
        |  a: A!
        |}
        |
        |type E {
        |  id: ID! @id
        |  e: String! @unique
        |  c: C!
        |}
      """.stripMargin
    }
    database.setup(project)

    server.query(
      """mutation{createA(data:{a:"a", 
        |                       b: {create: {b: "b"}},
        |                       c: {create:[{c: "c1", e: {create:{e: "e"}}},{c: "c2", e: {create:{e: "e2"}}}]}, 
        |                       d: {create: {d: "d"}}
        |                      }){a}}""".stripMargin,
      project
    )

    server.queryThatMustFail(
      """mutation{deleteA(where: {a:"a"}){a}}""",
      project,
      errorCode = 3042,
      errorContains = "The change you are trying to make would violate the required relation 'AToD' between A and D"
    )

  }

  "Several relations between the same model" should "be handled correctly" taggedAs IgnoreSQLite in { // TODO: Remove SQLite ignore when cascading again

    /**           A           If there are two relations between B and C and only one of them is marked
      *          /            cascading, then only the nodes connected to C's which are connected to B
      *         B             by this relations should be deleted.
      *        /  :
      *       Cs   C
      *        \ /
      *         D
      */
    val project = SchemaDsl.fromStringV11() {
      """
        |type A {
        |  id: ID! @id
        |  a: Float! @unique
        |  b: B @relation(onDelete: CASCADE, link: INLINE)
        |}
        |
        |type B {
        |  id: ID! @id
        |  b: Float! @unique
        |  cs: [C] @relation(onDelete: CASCADE, name:"Relation1")
        |  c: C @relation(name: "Relation2" link: INLINE)
        |}
        |
        |type C {
        |  id: ID! @id
        |  c: Float! @unique
        |  bs: [B] @relation(name: "Relation1")
        |  b: B @relation(name: "Relation2")
        |  d: [D] @relation(onDelete: CASCADE)
        |}
        |
        |type D {
        |  id: ID! @id
        |  d: Float! @unique
        |  c: [C]
        |}
      """.stripMargin
    }
    database.setup(project)

    server.query("""mutation{createA(data:{a: 10.10, b: {create:{b: 11.11}}}){a}}""", project)

    server.query("""mutation{updateB(where: {b: 11.11}, data:{cs: {create:[{c: 12.12},{c: 12.13}]}}){b}}""", project)
    server.query("""mutation{updateB(where: {b: 11.11}, data:{c: {create:{c: 12.99}}}){b}}""", project)

    server.query("""mutation{updateC(where: {c: 12.12}, data:{d: {create:{d: 13.13}}}){c}}""", project)
    server.query("""mutation{updateC(where: {c: 12.99}, data:{d: {create:{d: 13.99}}}){c}}""", project)

    server.query("""mutation{deleteA(where: {a:10.10}){a}}""", project)

    server.query("""query{as{a, b {b}}}""", project).toString should be("""{"data":{"as":[]}}""")
    server.query("""query{bs{b, c {c}, cs {c}}}""", project).toString should be("""{"data":{"bs":[]}}""")
    server.query("""query{cs{c, d {d}}}""", project).toString should be("""{"data":{"cs":[{"c":12.99,"d":[{"d":13.99}]}]}}""")
    server.query("""query{ds{d}}""", project).toString should be("""{"data":{"ds":[{"d":13.99}]}}""")
  }
  //endregion

  //region  NESTED DELETE

  "NESTING P1!-C1! relation deleting the parent" should "work if parent is marked cascading but error on returning previous values" in { // TODO: Remove SQLite ignore when cascading again
    //         P-C
    val project = SchemaDsl.fromStringV11() {
      """
        |type P {
        |  id: ID! @id
        |  p: String! @unique
        |  c: C! @relation(onDelete: CASCADE link: INLINE)
        |}
        |
        |type C {
        |  id: ID! @id
        |  c: String! @unique
        |  p: P! 
        |}
      """.stripMargin
    }

    database.setup(project)

    server.query("""mutation{createP(data:{p:"p", c: {create:{c: "c"}}}){p, c {c}}}""", project)
    server.query("""mutation{createP(data:{p:"p2", c: {create:{c: "c2"}}}){p, c {c}}}""", project)

    server.queryThatMustFail(
      """mutation{updateC(where: {c:"c"} data: {p: {delete: true}}){id}}""",
      project,
      errorCode = 0,
      errorContains = "Argument 'data' expected type 'CUpdateInput!'"
    )
  }

  "P1-C1-C1!-GC! relation updating the parent to delete the child and grandchild if marked cascading" should "work" taggedAs IgnoreSQLite in { // TODO: Remove SQLite ignore when cascading again
    //         P-C-GC
    val project = SchemaDsl.fromStringV11() {
      """
        |type P {
        |  id: ID! @id
        |  p: String! @unique
        |  c: C @relation(link: INLINE)
        |}
        |
        |type C {
        |  id: ID! @id
        |  c: String! @unique
        |  p: P
        |  gc: GC! @relation(onDelete: CASCADE, link: INLINE)
        |}
        |
        |type GC {
        |  id: ID! @id
        |  gc: String! @unique
        |  c: C!
        |}
      """.stripMargin
    }
    database.setup(project)

    server.query("""mutation{createP(data:{p:"p", c: {create:{c: "c", gc :{create:{gc: "gc"}}}}}){p, c {c, gc{gc}}}}""", project)
    server.query("""mutation{createP(data:{p:"p2", c: {create:{c: "c2", gc :{create:{gc: "gc2"}}}}}){p, c {c,gc{gc}}}}""", project)

    server.query("""mutation{updateP(where: {p:"p"}, data: { c: {delete: true}}){id}}""", project)

    server.query("""query{ps{p, c {c, gc{gc}}}}""", project).toString should be(
      """{"data":{"ps":[{"p":"p","c":null},{"p":"p2","c":{"c":"c2","gc":{"gc":"gc2"}}}]}}""")
    server.query("""query{cs{c, gc{gc}, p {p}}}""", project).toString should be("""{"data":{"cs":[{"c":"c2","gc":{"gc":"gc2"},"p":{"p":"p2"}}]}}""")
    server.query("""query{gCs{gc, c {c, p{p}}}}""", project).toString should be("""{"data":{"gCs":[{"gc":"gc2","c":{"c":"c2","p":{"p":"p2"}}}]}}""")

  }

  "P1!-C1!-C1!-GC! relation updating the parent to delete the child and grandchild if marked cascading" should "error if the child is required on parent" in { // TODO: Remove SQLite ignore when cascading again
    //         P-C-GC
    val project = SchemaDsl.fromStringV11() {
      """
        |type P {
        |  id: ID! @id
        |  p: String! @unique
        |  c: C! @relation(link: INLINE)
        |}
        |
        |type C {
        |  id: ID! @id
        |  c: String! @unique
        |  p: P!
        |  gc: GC! @relation(onDelete: CASCADE, link: INLINE)
        |}
        |
        |type GC {
        |  id: ID! @id
        |  gc: String! @unique
        |  c: C!
        |}
      """.stripMargin
    }
    database.setup(project)

    server.query("""mutation{createP(data:{p:"p", c: {create:{c: "c", gc :{create:{gc: "gc"}}}}}){p, c {c, gc{gc}}}}""", project)
    server.query("""mutation{createP(data:{p:"p2", c: {create:{c: "c2", gc :{create:{gc: "gc2"}}}}}){p, c {c,gc{gc}}}}""", project)

    server.queryThatMustFail(
      """mutation{updateP(where: {p:"p"}, data: { c: {delete: true}}){id}}""",
      project,
      errorCode = 0,
      errorContains = "Argument 'data' expected type 'PUpdateInput!'"
    )
  }
  //endregion

  "Self Relations" should "work" taggedAs IgnoreSQLite in {
    val project = SchemaDsl.fromStringV11() { """type Folder {
                                             |  id: ID! @id
                                             |  name: String! @unique
                                             |  parent: Folder @relation(name: "FolderOnFolder", onDelete: SET_NULL)
                                             |  children: [Folder] @relation(name: "FolderOnFolder", onDelete: CASCADE)
                                             |}""" }
    database.setup(project)

    server.query(
      """mutation{createFolder(data:{
        |           name: "Top",
        |           children: {create:{
        |               name: "Middle",
        |               children :{create:{
        |                   name: "Bottom"}}}}})
        | { name,
        |   parent{name},
        |   children{
        |       name,
        |       parent{name},
        |       children{
        |           name,
        |           parent{name},
        |           children{name}}}}}""".stripMargin,
      project
    )

    server.query("""mutation{deleteFolder(where: {name: "Top"}){name, children{name}}}""", project)

    server.query("""query{folders{name}}""", project).toString should be("""{"data":{"folders":[]}}""")
  }

  "Self Relations" should "work 2" taggedAs (IgnoreSQLite) in { // FIXME: Eats all the RAM // TODO: Remove SQLite ignore when cascading again
    val project = SchemaDsl.fromStringV11() { """type Folder  {
                                             |  id: ID! @id
                                             |  name: String! @unique
                                             |  children: [Folder] @relation(name: "FolderOnFolder", onDelete: CASCADE)
                                             |  parent: Folder @relation(name: "FolderOnFolder", onDelete: SET_NULL)
                                             |}""" }
    database.setup(project)

    server.query(
      """mutation{createFolder(data:{
        |           name: "Top",
        |           children: {create:{
        |               name: "Middle",
        |               children :{create:{
        |                   name: "Bottom"}}}}})
        | { name,
        |   parent{name},
        |   children{
        |       name,
        |       parent{name},
        |       children{
        |           name,
        |           parent{name},
        |           children{name}}}}}""".stripMargin,
      project
    )

    server.query("""mutation{deleteFolder(where: {name: "Top"}){name, children{name}}}""", project)

    server.query("""query{folders{name}}""", project).toString should be("""{"data":{"folders":[]}}""")
  }

  "Self Relations" should "work 3" taggedAs IgnoreSQLite in { // TODO: Remove SQLite ignore when cascading again
    val project = SchemaDsl.fromStringV11() { """type Folder  {
                                             |  id: ID! @id
                                             |  name: String! @unique
                                             |  parent: Folder @relation(name: "FolderOnFolder", onDelete: SET_NULL)
                                             |  children: [Folder] @relation(name: "FolderOnFolder", onDelete: CASCADE)
                                             |}""" }
    database.setup(project)

    server.query(
      """mutation{createFolder(data:{
        |           name: "Top",
        |           children: {create:{
        |               name: "Middle"}}})
        | { name,
        |   parent{name},
        |   children{
        |       name,
        |       parent{name},
        |       children{
        |           name,
        |           parent{name},
        |           children{name}}}}}""".stripMargin,
      project
    )

    server.query("""mutation{deleteFolder(where: {name: "Top"}){name, children{name}}}""", project)

    server.query("""query{folders{name}}""", project).toString should be("""{"data":{"folders":[]}}""")
  }

  "Cascade on both sides" should "halt" taggedAs IgnoreSQLite in { // TODO: Remove SQLite ignore when cascading again
    val project = SchemaDsl.fromStringV11() { """type User {
                                             |  id: ID! @id
                                             |  name: String! @unique
                                             |  a: [A] @relation(name: "A", onDelete: CASCADE)
                                             |  b: [B] @relation(name: "B", onDelete: CASCADE)
                                             |}
                                             |
                                             |type A{
                                             |  id: ID! @id
                                             |  name: String! @unique
                                             |  user: User! @relation(name: "A", onDelete: CASCADE)
                                             |}
                                             |
                                             |type B{
                                             |  id: ID! @id
                                             |  name: String! @unique
                                             |  user: User! @relation(name: "B", onDelete: CASCADE)
                                             |}""" }
    database.setup(project)

    server.query("""mutation createUser{createUser(data:{name: "Paul"}){id}}""", project)

    server.query("""mutation createA{createA(data:{name:"A" user: {connect:{name: "Paul"}}}){id}}""", project)

    server.query("""mutation createB{createB(data:{name:"B" user: {connect:{name: "Paul"}}}){id}}""", project)

    server.query("""mutation deleteUser{deleteUser(where: {name: "Paul"}){id}}""", project)

    server.query("""query{users{name}}""", project).toString should be("""{"data":{"users":[]}}""")
    server.query("""query{as{name}}""", project).toString should be("""{"data":{"as":[]}}""")
    server.query("""query{bs{name}}""", project).toString should be("""{"data":{"bs":[]}}""")

  }

  "A deleteMany " should " work with cascading delete" taggedAs IgnoreSQLite in { // TODO: Remove SQLite ignore when cascading again

    val project: Project = setupForDeleteManys

    server.query("""mutation {deleteManyTops(where:{int_lt: 10}){count}}""", project).toString should be("""{"data":{"deleteManyTops":{"count":2}}}""")

    server.query("""query {tops{int}}""", project).toString should be("""{"data":{"tops":[]}}""")

    server.query("""query {middles{int}}""", project).toString should be("""{"data":{"middles":[]}}""")

    server.query("""query {bottoms{int}}""", project).toString should be("""{"data":{"bottoms":[]}}""")

  }

  "A nested deleteMany " should " work with cascading delete" taggedAs IgnoreSQLite in { // TODO: Remove SQLite ignore when cascading again

    val project: Project = setupForDeleteManys

    server.query("""mutation {deleteManyMiddles(where:{int_gt: 0}){count}}""", project).toString should be("""{"data":{"deleteManyMiddles":{"count":40}}}""")

    server.query("""query {tops{int}}""", project).toString should be("""{"data":{"tops":[{"int":1},{"int":2}]}}""")

    server.query("""query {middles{int}}""", project).toString should be("""{"data":{"middles":[]}}""")

    server.query("""query {bottoms{int}}""", project).toString should be("""{"data":{"bottoms":[]}}""")

  }

  private def setupForDeleteManys = {
    val project: Project = SchemaDsl.fromStringV11() {
      """
        |type Top {
        |   id: ID! @id
        |   int: Int @unique
        |   middles:[Middle]   @relation(name: "TopToMiddle", onDelete: CASCADE)
        |}
        |
        |type Middle {
        |   id: ID! @id
        |   int: Int! @unique
        |   top: Top @relation(name: "TopToMiddle")
        |   bottom: [Bottom] @relation(name: "MiddleToBottom", onDelete: CASCADE)
        |}
        |
        |type Bottom {
        |   id: ID! @id
        |   middle: Middle @relation(name: "MiddleToBottom")
        |   int: Int!
        |}
      """
    }
    database.setup(project)

    def createMiddle(int: Int) = server.query(s"""mutation {createMiddle(data:{int: $int top: {connect:{int: 1}}}){int}}""", project)

    def createMiddle2(int: Int) = server.query(s"""mutation {createMiddle(data:{int: 1000${int} top: {connect:{int: 2}}}){int}}""", project)

    def createBottom(int: Int) = server.query(s"""mutation{a: createBottom(data:{int: $int$int middle: {connect:{int: $int}}}){int}}""", project)

    def createBottom2(int: Int) = server.query(s"""mutation{a: createBottom(data:{int: 1000$int$int middle: {connect:{int: 1000$int}}}){int}}""", project)

    val top  = server.query("""mutation {createTop(data:{int: 1}){int}}""", project)
    val top2 = server.query("""mutation {createTop(data:{int: 2}){int}}""", project)

    for (int <- 1 to 20) {
      createMiddle(int)
      createMiddle2(int)

    }

    for (_ <- 1 to 20) {
      for (int <- 1 to 10) {
        createBottom(int)
        createBottom2(int)
      }
    }
    project
  }
}
