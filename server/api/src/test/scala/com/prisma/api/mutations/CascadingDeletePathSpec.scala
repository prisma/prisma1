package com.prisma.api.mutations

import com.prisma.api.ApiBaseSpec
import com.prisma.shared.models._
import com.prisma.shared.project_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}
import com.prisma.api.database.CascadingDeletes._
import com.prisma.api.database.DatabaseMutationBuilder

class CascadingDeletePathSpec extends FlatSpec with Matchers with ApiBaseSpec {

  "Paths" should "not be generated for non-cascading relations" in {

    //                                P
    //                        C             SC
    //              GC            GC2               SGC
    //                  GGC                   SGGC          SGGC2

    val project = SchemaDsl() { schema =>
      val parent               = schema.model("P").field_!("p", _.String, isUnique = true)
      val child                = schema.model("C").field_!("c", _.String, isUnique = true).manyToManyRelation("p", "c", parent)
      val stepchild            = schema.model("SC").field_!("sc", _.String, isUnique = true).manyToManyRelation("p", "sc", parent)
      val grandchild           = schema.model("GC").field_!("gc", _.String, isUnique = true).manyToManyRelation("c", "gc", child)
      val grandchild2          = schema.model("GC2").field_!("gc2", _.String, isUnique = true).manyToManyRelation("gc2", "c", child)
      val stepgrandchild       = schema.model("SGC").field_!("sgc", _.String, isUnique = true).manyToManyRelation("sc", "sgc", stepchild)
      val greatgrandchild      = schema.model("GGC").field_!("ggc", _.String, isUnique = true).manyToManyRelation("gc", "ggc", grandchild)
      val stepgreatgrandchild  = schema.model("SGGC").field_!("sggc", _.String, isUnique = true).manyToManyRelation("sgc", "sggc", stepgrandchild)
      val stepgreatgrandchild2 = schema.model("SGGC2").field_!("sggc2", _.String, isUnique = true).manyToManyRelation("sgc", "sggc2", stepgrandchild)

    }
    database.setup(project)

    val parent = project.schema.getModelByName_!("P")
    val res    = collectPaths(project, NodeSelector.forId(parent, "does not exist"), parent)
    res should be(List.empty)
  }

  "Paths for nested relations where all sides are cascading" should "be generated correctly" in {

    //                                P
    //                        C             SC
    //              GC            GC2               SGC
    //                  GGC                   SGGC          SGGC2

    val project = SchemaDsl() { schema =>
      val parent = schema.model("P").field_!("p", _.String, isUnique = true)
      val child = schema
        .model("C")
        .field_!("c", _.String, isUnique = true)
        .manyToManyRelation("p", "c", parent, modelAOnDelete = OnDelete.Cascade, modelBOnDelete = OnDelete.Cascade)
      val stepchild = schema
        .model("SC")
        .field_!("sc", _.String, isUnique = true)
        .manyToManyRelation("p", "sc", parent, modelAOnDelete = OnDelete.Cascade, modelBOnDelete = OnDelete.Cascade)
      val grandchild = schema
        .model("GC")
        .field_!("gc", _.String, isUnique = true)
        .manyToManyRelation("c", "gc", child, modelAOnDelete = OnDelete.Cascade, modelBOnDelete = OnDelete.Cascade)
      val grandchild2 = schema
        .model("GC2")
        .field_!("gc2", _.String, isUnique = true)
        .manyToManyRelation("gc2", "c", child, modelAOnDelete = OnDelete.Cascade, modelBOnDelete = OnDelete.Cascade)
      val stepgrandchild = schema
        .model("SGC")
        .field_!("sgc", _.String, isUnique = true)
        .manyToManyRelation("sc", "sgc", stepchild, modelAOnDelete = OnDelete.Cascade, modelBOnDelete = OnDelete.Cascade)
      val greatgrandchild = schema
        .model("GGC")
        .field_!("ggc", _.String, isUnique = true)
        .manyToManyRelation("gc", "ggc", grandchild, modelAOnDelete = OnDelete.Cascade, modelBOnDelete = OnDelete.Cascade)
      val stepgreatgrandchild = schema
        .model("SGGC")
        .field_!("sggc", _.String, isUnique = true)
        .manyToManyRelation("sgc", "sggc", stepgrandchild, modelAOnDelete = OnDelete.Cascade, modelBOnDelete = OnDelete.Cascade)
      val stepgreatgrandchild2 = schema
        .model("SGGC2")
        .field_!("sggc2", _.String, isUnique = true)
        .manyToManyRelation("sgc", "sggc2", stepgrandchild, modelAOnDelete = OnDelete.Cascade, modelBOnDelete = OnDelete.Cascade)

    }
    database.setup(project)

    val parent = project.schema.getModelByName_!("P")
    val res    = collectPaths(project, NodeSelector.forId(parent, "does not exist"), parent)
    res.foreach(x => println(x.pretty))

    val res2 = res.map(x => x.pretty).mkString("\n")
    res2 should be("""Where: P, id, does not exist |  P<->C
                     |Where: P, id, does not exist |  P<->C C<->GC
                     |Where: P, id, does not exist |  P<->C C<->GC GC<->GGC
                     |Where: P, id, does not exist |  P<->C C<->GC2
                     |Where: P, id, does not exist |  P<->SC
                     |Where: P, id, does not exist |  P<->SC SC<->SGC
                     |Where: P, id, does not exist |  P<->SC SC<->SGC SGC<->SGGC
                     |Where: P, id, does not exist |  P<->SC SC<->SGC SGC<->SGGC2""".stripMargin)
  }

  "Paths for graphs with  circles" should "terminate" in {

    //                                P
    //                              /   \
    //                            C   -  SC

    val project = SchemaDsl() { schema =>
      val parent = schema.model("P").field_!("p", _.String, isUnique = true)
      val child = schema
        .model("C")
        .field_!("c", _.String, isUnique = true)
        .manyToManyRelation("p", "c", parent, modelAOnDelete = OnDelete.Cascade, modelBOnDelete = OnDelete.Cascade)
      val stepchild = schema
        .model("SC")
        .field_!("sc", _.String, isUnique = true)
        .manyToManyRelation("p", "sc", parent, modelAOnDelete = OnDelete.Cascade, modelBOnDelete = OnDelete.Cascade)
        .manyToManyRelation("c", "sc", child, modelAOnDelete = OnDelete.Cascade, modelBOnDelete = OnDelete.Cascade)
    }
    database.setup(project)

    val parent = project.schema.getModelByName_!("P")
    val res    = collectPaths(project, NodeSelector.forId(parent, "does not exist"), parent)
    res.foreach(x => println(x.pretty))

    val res2 = res.map(x => x.pretty).mkString("\n")
    res2 should be("""P<->C
                     |P<->C C<->SC SC<->P
                     |P<->C C<->SC SC<->P
                     |P<->C C<->SC
                     |P<->SC
                     |P<->SC SC<->C C<->P
                     |P<->SC SC<->C C<->P
                     |P<->SC SC<->C""".stripMargin)
  }

  "Paths for graphs with  circles" should "terminate does not go up to the parent again on the childs" in {

    //                                P
    //                              /   \
    //                            C   -  SC

    val project = SchemaDsl() { schema =>
      val parent = schema.model("P").field_!("p", _.String, isUnique = true)
      val child = schema
        .model("C")
        .field_!("c", _.String, isUnique = true)
        .manyToManyRelation("p", "c", parent, modelBOnDelete = OnDelete.Cascade)
      val stepchild = schema
        .model("SC")
        .field_!("sc", _.String, isUnique = true)
        .manyToManyRelation("p", "sc", parent, modelBOnDelete = OnDelete.Cascade)
        .manyToManyRelation("c", "sc", child, modelAOnDelete = OnDelete.Cascade, modelBOnDelete = OnDelete.Cascade)
    }
    database.setup(project)

    val parent = project.schema.getModelByName_!("P")
    val res    = collectPaths(project, NodeSelector.forId(parent, "does not exist"), parent)
    res.foreach(x => println(x.pretty))

    val res2 = res.map(x => x.pretty).mkString("\n")
    res2 should be("""Where: P, id, does not exist |  P<->C C<->SC
                     |Where: P, id, does not exist |  P<->SC SC<->C""".stripMargin)
  }

  "Paths for graphs with  circles" should "detect the circle and error" in {

    //                            A       A2
    //                              \   /
    //                                P
    //                              /   \
    //                            C   -  SC

    val project = SchemaDsl() { schema =>
      val ancestor  = schema.model("A").field_!("a", _.String, isUnique = true)
      val ancestor2 = schema.model("A2").field_!("a2", _.String, isUnique = true)
      val parent = schema
        .model("P")
        .field_!("p", _.String, isUnique = true)
        .manyToManyRelation("a", "p", ancestor, modelAOnDelete = OnDelete.Cascade, modelBOnDelete = OnDelete.Cascade)
        .manyToManyRelation("a2", "p", ancestor2, modelAOnDelete = OnDelete.Cascade, modelBOnDelete = OnDelete.Cascade)
      val child = schema
        .model("C")
        .field_!("c", _.String, isUnique = true)
        .manyToManyRelation("p", "c", parent, modelAOnDelete = OnDelete.Cascade, modelBOnDelete = OnDelete.Cascade)
      val stepchild = schema
        .model("SC")
        .field_!("sc", _.String, isUnique = true)
        .manyToManyRelation("p", "sc", parent, modelAOnDelete = OnDelete.Cascade, modelBOnDelete = OnDelete.Cascade)
        .manyToManyRelation("c", "sc", child, modelAOnDelete = OnDelete.Cascade, modelBOnDelete = OnDelete.Cascade)
    }
    database.setup(project)

    val parent = project.schema.getModelByName_!("P")
    val res    = collectPaths(project, NodeSelector.forId(parent, "does not exist"), parent)
    res.foreach(x => println(x.pretty))

    val res2 = res.map(x => x.pretty).mkString("\n")
    res2 should be("""P<->A
                     |P<->A2
                     |P<->C
                     |P<->C C<->SC SC<->P P<->A
                     |P<->C C<->SC SC<->P P<->A2
                     |P<->C C<->SC SC<->P
                     |P<->C C<->SC
                     |P<->SC
                     |P<->SC SC<->C C<->P P<->A
                     |P<->SC SC<->C C<->P P<->A2
                     |P<->SC SC<->C C<->P
                     |P<->SC SC<->C""".stripMargin)
  }

  "Paths for graphs with  circles" should "detect the circle also on selfrelations and error" in {

    //                            C  -  C

    val project = SchemaDsl() { schema =>
      val child = schema.model("C").field_!("c", _.String, isUnique = true)
      child.manyToManyRelation("brother", "sister", child, modelAOnDelete = OnDelete.Cascade, modelBOnDelete = OnDelete.Cascade)

    }
    database.setup(project)

    val parent = project.schema.getModelByName_!("C")
    val res    = collectPaths(project, NodeSelector.forId(parent, "does not exist"), parent)
    res.foreach(x => println(x.pretty))

    val res2 = res.map(x => x.pretty).mkString("\n")
    res2 should be("""P<->A
                     |P<->A2
                     |P<->C
                     |P<->C C<->SC SC<->P P<->A
                     |P<->C C<->SC SC<->P P<->A2
                     |P<->C C<->SC SC<->P
                     |P<->C C<->SC
                     |P<->SC
                     |P<->SC SC<->C C<->P P<->A
                     |P<->SC SC<->C C<->P P<->A2
                     |P<->SC SC<->C C<->P
                     |P<->SC SC<->C""".stripMargin)
  }

  "The mutation builder" should "generate the correct query for terminate" in {

    //                                P
    //                              /   \
    //                            C  ... SC

    val project = SchemaDsl() { schema =>
      val parent = schema.model("P").field_!("p", _.String, isUnique = true)
      val child = schema
        .model("C")
        .field_!("c", _.String, isUnique = true)
        .oneToOneRelation("p", "c", parent, modelAOnDelete = OnDelete.Cascade, modelBOnDelete = OnDelete.Cascade)
      val stepchild = schema
        .model("SC")
        .field_!("sc", _.String, isUnique = true)
        .oneToOneRelation("p", "sc", parent, modelAOnDelete = OnDelete.Cascade, modelBOnDelete = OnDelete.Cascade)
        .manyToManyRelation("c", "sc", child)
    }
    database.setup(project)

    val parent = project.schema.getModelByName_!("P")

    //set up nodes for p1 and children

    val setup    = server.executeQuerySimple("""mutation{createP(data:{p:"p", c: {create:{c: "c"}}, sc: {create: {sc: "sc"}}}){id, c {id}, sc{id}}}""", project)
    val parentId = setup.pathAsString("data.createP.id")

    // connect children of p1 with other siblings that are not in relation to p1

    server.executeQuerySimple("""mutation{updateC(where: {c: "c"},    data:{sc: {create:[{ sc: "sc2"},{ sc: "sc3"}]}}){c}}""", project)
    server.executeQuerySimple("""mutation{updateSC(where: {sc: "sc"}, data:{c:  {create:[{  c: "c2"}, {  c:  "c3"}]}}){sc}}""", project)

    //set up nodes for p2 and children
    server.executeQuerySimple("""mutation{createP(data:{p:"p2", c: {create:{c: "cx"}}, sc: {create: {sc: "scx"}}}){id, c {id}, sc{id}}}""", project)
    server.executeQuerySimple("""mutation{updateC(where: {c: "cx"},    data:{sc: {create:[{ sc: "scx2"},{ sc: "scx3"}]}}){c}}""", project)
    server.executeQuerySimple("""mutation{updateSC(where: {sc: "scx"}, data:{c:  {create:[{  c: "cx2"}, {  c:  "cx3"}]}}){sc}}""", project)

    val res = collectPaths(project, NodeSelector.forId(parent, parentId), parent)
    res.foreach(x => println(x.pretty))

    val res2 = res.map(x => x.pretty).mkString("\n")

    val childrenRelation = project.schema.getUnambiguousRelationThatConnectsModels_!("SC", "C").get

    //first side
    val check1 = DatabaseMutationBuilder.oldParentFailureTriggerByPath(project, childrenRelation, res.head)
    database.runDbActionOnClientDb(check1)

    //second side
    val check2 = DatabaseMutationBuilder.oldParentFailureTriggerByPath(project, childrenRelation, res.reverse.head)
    database.runDbActionOnClientDb(check2)

    //first side
    val query = DatabaseMutationBuilder.deleteRelationRowsByPath(project.id, childrenRelation, res.head)
    database.runDbActionOnClientDb(query)

    //second side
    val query2 = DatabaseMutationBuilder.deleteRelationRowsByPath(project.id, childrenRelation, res.reverse.head)
    database.runDbActionOnClientDb(query2)

    println(query)
  }

}
