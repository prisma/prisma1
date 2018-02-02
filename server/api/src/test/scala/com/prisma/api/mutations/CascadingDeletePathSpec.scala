package com.prisma.api.mutations

import com.prisma.api.ApiBaseSpec
import com.prisma.shared.models._
import com.prisma.shared.project_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class CascadingDeletePathSpec extends FlatSpec with Matchers with ApiBaseSpec {

  "Paths" should "not be generated for non-cascading relations" ignore {

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
    val res    = collectPaths(project, parent)
    res should be(List.empty)
  }

  "Paths for nested relations where all sides are cascading" should "be generated correctly" ignore {

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
    val res    = collectPaths(project, parent)
    res.foreach(x => println(x.pretty))

    val res2 = res.map(x => x.pretty).mkString("\n")
    res2 should be("""P<->C
                      |P<->C C<->GC
                      |P<->C C<->GC GC<->GGC
                      |P<->C C<->GC2
                      |P<->SC
                      |P<->SC SC<->SGC
                      |P<->SC SC<->SGC SGC<->SGGC
                      |P<->SC SC<->SGC SGC<->SGGC2""".stripMargin)
  }

  "Paths for graphs with  circles" should "terminate" ignore {

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
    val res    = collectPaths(project, parent)
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

  "Paths for graphs with  circles" should "terminate does not go up to the parent again on the childs" ignore {

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
    val res    = collectPaths(project, parent)
    res.foreach(x => println(x.pretty))

    val res2 = res.map(x => x.pretty).mkString("\n")
    res2 should be("""P<->C C<->SC
                     |P<->SC SC<->C""".stripMargin)
  }

  "Paths for graphs with  circles" should "detect the circle and error" ignore {

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
    val res    = collectPaths(project, parent)
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

  "Paths for graphs with  circles" should "detect the circle also on selfrelations and error" ignore {

    //                            C  -  C

    val project = SchemaDsl() { schema =>
      val child = schema.model("C").field_!("c", _.String, isUnique = true)
      child.manyToManyRelation("brother", "sister", child, modelAOnDelete = OnDelete.Cascade, modelBOnDelete = OnDelete.Cascade)

    }
    database.setup(project)

    val parent = project.schema.getModelByName_!("C")
    val res    = collectPaths(project, parent)
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

  case class ModelWithRelation(parent: Model, child: Model, relation: Relation)

  def getMWR(project: Project, model: Model, field: Field): ModelWithRelation =
    ModelWithRelation(model, field.relatedModel(project.schema).get, field.relation.get)

  case class Path(mwrs: List[ModelWithRelation]) {
    def prepend(mwr: ModelWithRelation): Path = copy(mwr +: mwrs)
    def pretty: String                        = mwrs.map(mwr => s"${mwr.parent.name}<->${mwr.child.name}").mkString(" ")
    def detectCircle(path: List[ModelWithRelation] = this.mwrs, seen: List[Model] = List.empty): Unit = {
      path match {
        case x if x.isEmpty                                 =>
        case x if x.nonEmpty && seen.contains(x.head.child) => sys.error("Circle")
        case head :: Nil if head.parent == head.child       => sys.error("Circle")
        case head :: tail                                   => detectCircle(tail, seen :+ head.parent)
      }
    }
  }
  object Path {
    lazy val empty = Path(List.empty)
  }

  def collectPaths(project: Project, startNode: Model, excludes: List[Relation] = List.empty): List[Path] = {
    val cascadingRelationFields = startNode.cascadingRelationFields
    val res = cascadingRelationFields.flatMap { field =>
      val mwr = getMWR(project, startNode, field)
      if (excludes.contains(mwr.relation)) {
        List(Path.empty)
      } else {
        val childPaths = collectPaths(project, mwr.child, excludes :+ mwr.relation)
        childPaths.map(path => path.prepend(mwr))
      }
    }
    val distinct = res.distinct
    distinct.map(path => path.detectCircle())
    distinct

  }
}
