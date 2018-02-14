package com.prisma.api.mutations

import com.prisma.api.ApiBaseSpec
import com.prisma.api.mutations.mutations.CascadingDeletes._
import com.prisma.api.schema.APIErrors.CascadingDeletePathLoops
import com.prisma.shared.models._
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

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
    val res    = collectPaths(project, NodeSelector.forId(parent, "does not exist"), parent, List(parent))

    res.length should be(1)
    res.head.edges should be(List.empty)
  }

  "Paths for nested relations where all sides are cascading" should "be generated correctly" in {
    //                                P
    //                        C             SC
    //              GC            GC2               SGC
    //                  GGC                   SGGC          SGGC2

    val project = SchemaDsl() { schema =>
      val parent               = schema.model("P").field_!("p", _.String, isUnique = true)
      val child                = schema.model("C").field_!("c", _.String, isUnique = true)
      val stepchild            = schema.model("SC").field_!("sc", _.String, isUnique = true)
      val grandchild           = schema.model("GC").field_!("gc", _.String, isUnique = true)
      val grandchild2          = schema.model("GC2").field_!("gc2", _.String, isUnique = true)
      val stepgrandchild       = schema.model("SGC").field_!("sgc", _.String, isUnique = true)
      val greatgrandchild      = schema.model("GGC").field_!("ggc", _.String, isUnique = true)
      val stepgreatgrandchild  = schema.model("SGGC").field_!("sggc", _.String, isUnique = true)
      val stepgreatgrandchild2 = schema.model("SGGC2").field_!("sggc2", _.String, isUnique = true)

      child.manyToManyRelation("p", "c", parent, modelBOnDelete = OnDelete.Cascade)
      stepchild.manyToManyRelation("p", "sc", parent, modelBOnDelete = OnDelete.Cascade)
      grandchild.manyToManyRelation("c", "gc", child, modelBOnDelete = OnDelete.Cascade)
      grandchild2.manyToManyRelation("gc2", "c", child, modelBOnDelete = OnDelete.Cascade)
      stepgrandchild.manyToManyRelation("sc", "sgc", stepchild, modelBOnDelete = OnDelete.Cascade)
      greatgrandchild.manyToManyRelation("gc", "ggc", grandchild, modelBOnDelete = OnDelete.Cascade)
      stepgreatgrandchild.manyToManyRelation("sgc", "sggc", stepgrandchild, modelBOnDelete = OnDelete.Cascade)
      stepgreatgrandchild2.manyToManyRelation("sgc", "sggc2", stepgrandchild, modelBOnDelete = OnDelete.Cascade)
    }
    database.setup(project)

    val parent = project.schema.getModelByName_!("P")
    val res    = collectPaths(project, NodeSelector.forId(parent, "does not exist"), parent, List(parent))
    res.foreach(x => println(x.pretty))

    val res2 = res.map(x => x.pretty).mkString("\n")
    res2 should be("""Where: P, id, does not exist |  P<->C C<->GC GC<->GGC
                     |Where: P, id, does not exist |  P<->C C<->GC2
                     |Where: P, id, does not exist |  P<->SC SC<->SGC SGC<->SGGC
                     |Where: P, id, does not exist |  P<->SC SC<->SGC SGC<->SGGC2""".stripMargin)
  }

  "Paths for graphs with incomplete circles that do not go up to the parent again on the childsides" should "work" in {
    //                                P
    //                              /   \
    //                            C   -  SC

    val project = SchemaDsl() { schema =>
      val parent    = schema.model("P").field_!("p", _.String, isUnique = true)
      val child     = schema.model("C").field_!("c", _.String, isUnique = true)
      val stepchild = schema.model("SC").field_!("sc", _.String, isUnique = true)

      child.manyToManyRelation("p", "c", parent, modelBOnDelete = OnDelete.Cascade)
      stepchild.manyToManyRelation("p", "sc", parent, modelBOnDelete = OnDelete.Cascade)
      stepchild.manyToManyRelation("c", "sc", child, modelAOnDelete = OnDelete.Cascade)
    }
    database.setup(project)

    val parent = project.schema.getModelByName_!("P")
    val res    = collectPaths(project, NodeSelector.forId(parent, "does not exist"), parent, List(parent))
    res.foreach(x => println(x.pretty))

    val res2 = res.map(x => x.pretty).mkString("\n")
    res2 should be("""Where: P, id, does not exist |  P<->C
                     |Where: P, id, does not exist |  P<->SC SC<->C""".stripMargin)
  }

  //region Loops

  "Paths for graphs with  circles" should "error" in {
    //                                P
    //                              /   \
    //                            C   -  SC

    val project = SchemaDsl() { schema =>
      val parent    = schema.model("P").field_!("p", _.String, isUnique = true)
      val child     = schema.model("C").field_!("c", _.String, isUnique = true)
      val stepchild = schema.model("SC").field_!("sc", _.String, isUnique = true)

      parent.manyToManyRelation("c", "p", child, modelAOnDelete = OnDelete.Cascade)
      child.manyToManyRelation("sc", "c", stepchild, modelAOnDelete = OnDelete.Cascade)
      stepchild.manyToManyRelation("p", "sc", parent, modelAOnDelete = OnDelete.Cascade)
    }
    database.setup(project)

    val parent = project.schema.getModelByName_!("P")
    assertThrows[CascadingDeletePathLoops] { collectPaths(project, NodeSelector.forId(parent, "does not exist"), parent, List(parent)) }
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
      val parent    = schema.model("P").field_!("p", _.String, isUnique = true)
      val child     = schema.model("C").field_!("c", _.String, isUnique = true)
      val stepchild = schema.model("SC").field_!("sc", _.String, isUnique = true)

      parent.manyToManyRelation("a2", "p", ancestor2, modelAOnDelete = OnDelete.Cascade, modelBOnDelete = OnDelete.Cascade)
      parent.manyToManyRelation("a", "p", ancestor, modelAOnDelete = OnDelete.Cascade, modelBOnDelete = OnDelete.Cascade)
      parent.manyToManyRelation("p", "c", parent, modelAOnDelete = OnDelete.Cascade, modelBOnDelete = OnDelete.Cascade)
      stepchild.manyToManyRelation("p", "sc", parent, modelAOnDelete = OnDelete.Cascade, modelBOnDelete = OnDelete.Cascade)
      stepchild.manyToManyRelation("c", "sc", child, modelAOnDelete = OnDelete.Cascade, modelBOnDelete = OnDelete.Cascade)
    }
    database.setup(project)

    val parent = project.schema.getModelByName_!("P")
    assertThrows[CascadingDeletePathLoops] { collectPaths(project, NodeSelector.forId(parent, "does not exist"), parent, List(parent)) }
  }

  "Self relations that are only marked cascading on one side" should "error because they could loop indefinitely" in {
    //                               ___
    //                               \ /
    //                                P

    val project = SchemaDsl() { schema =>
      val parent = schema.model("P").field_!("p", _.String, isUnique = true)
      parent.manyToManyRelation("follow", "Opposite Side", parent, modelAOnDelete = OnDelete.Cascade)
    }
    database.setup(project)

    val parent = project.schema.getModelByName_!("P")
    assertThrows[CascadingDeletePathLoops] { collectPaths(project, NodeSelector.forId(parent, "does not exist"), parent, List(parent)) }
  }

  "Self relations that are marked cascading on both sides" should "error because they could loop indefinitely" in {
    //                               ___
    //                               \ /
    //                                P

    val project = SchemaDsl() { schema =>
      val parent = schema.model("P").field_!("p", _.String, isUnique = true)
      parent.manyToManyRelation("follow", "Opposite Side", parent, modelAOnDelete = OnDelete.Cascade, modelBOnDelete = OnDelete.Cascade)
    }
    database.setup(project)

    val parent = project.schema.getModelByName_!("P")
    assertThrows[CascadingDeletePathLoops] { collectPaths(project, NodeSelector.forId(parent, "does not exist"), parent, List(parent)) }
  }
  //endregion
}
