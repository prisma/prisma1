package com.prisma.api.mutations

import com.prisma.api.ApiBaseSpec
import com.prisma.api.database.DatabaseQueryBuilder
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class NestedUpsertMutationInsideUpdateSpec extends FlatSpec with Matchers with ApiBaseSpec {

  "a P1! to C1! relation" should "error  on create since old required parent relation would be broken" in {
    val project = SchemaDsl() { schema =>
      val parent = schema.model("Parent").field_!("p", _.String, isUnique = true)
      val child  = schema.model("Child").field_!("c", _.String, isUnique = true)
      child.oneToOneRelation_!("parentReq", "childReq", parent)
    }
    database.setup(project)

    val res = server
      .executeQuerySimple(
        """mutation {
          |  createParent(data: {
          |    p: "p1"
          |    childReq: {
          |      create: {c: "c1"}
          |    }
          |  }){
          |    id
          |    childReq{
          |       id
          |    }
          |  }
          |}""".stripMargin,
        project
      )

    val parentId = res.pathAsString("data.createParent.id")

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ChildToParent").as[Int]) should be(Vector(1))

    server.executeQuerySimpleThatMustFail(
      s"""
         |mutation {
         |  updateParent(
         |  where: {id: "$parentId"}
         |  data:{
         |    p: "p2"
         |    childReq: {upsert: {
         |    where: {c: "DOES NOT EXIST"}
         |    update: {c: "DOES NOT MATTER"}
         |    create :{c: "SomeC"}
         |    }}
         |  }){
         |  p
         |  childReq {
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
      project,
      errorCode = 3042,
      errorContains = "The change you are trying to make would violate the required relation '_ChildToParent' between Child and Parent"
    )

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ChildToParent").as[Int]) should be(Vector(1))
  }

  "a P1! to C1! relation" should "succeed on update" in {
    val project = SchemaDsl() { schema =>
      val parent = schema.model("Parent").field_!("p", _.String, isUnique = true)
      val child  = schema.model("Child").field_!("c", _.String, isUnique = true)
      child.oneToOneRelation_!("parentReq", "childReq", parent)
    }
    database.setup(project)

    val res = server
      .executeQuerySimple(
        """mutation {
          |  createParent(data: {
          |    p: "p1"
          |    childReq: {
          |      create: {c: "c1"}
          |    }
          |  }){
          |    id
          |    childReq{
          |       id
          |    }
          |  }
          |}""".stripMargin,
        project
      )

    val parentId = res.pathAsString("data.createParent.id")

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ChildToParent").as[Int]) should be(Vector(1))

    val res2 = server.executeQuerySimple(
      s"""
         |mutation {
         |  updateParent(
         |  where: {id: "$parentId"}
         |  data:{
         |    p: "p2"
         |    childReq: {upsert: {
         |    where: {c: "c1"}
         |    update: {c: "New c"}
         |    create :{c: "DOES NOT MATTER"}
         |    }}
         |  }){
         |  p
         |  childReq {
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    res2.toString should be("""{"data":{"updateParent":{"p":"p2","childReq":{"c":"New c"}}}}""")
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ChildToParent").as[Int]) should be(Vector(1))
  }

  "a P1! to C1 relation" should "work on create" in {
    val project = SchemaDsl() { schema =>
      val child  = schema.model("Child").field_!("c", _.String, isUnique = true)
      val parent = schema.model("Parent").field_!("p", _.String, isUnique = true)
      parent.oneToOneRelation_!("childReq", "parentOpt", child, isRequiredOnFieldB = false)
    }
    database.setup(project)

    val res = server
      .executeQuerySimple(
        """mutation {
          |  createParent(data: {
          |    p: "p1"
          |    childReq: {
          |      create: {c: "c1"}
          |    }
          |  }){
          |  id
          |    childReq{
          |       id
          |    }
          |  }
          |}""".stripMargin,
        project
      )

    val parentId = res.pathAsString("data.createParent.id")

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ParentToChild").as[Int]) should be(Vector(1))

    val res2 = server.executeQuerySimple(
      s"""
         |mutation {
         |  updateParent(
         |  where: {id: "$parentId"}
         |  data:{
         |    p: "p2"
         |    childReq: {upsert: {
         |    where: {c: "DOES NOT EXIST"}
         |    update: {c: "DOES NOT MATTER"}
         |    create :{c: "new C"}
         |    }}
         |  }){
         |    childReq {
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    res2.toString should be("""{"data":{"updateParent":{"childReq":{"c":"new C"}}}}""")
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "Parent").as[Int]) should be(Vector(1))
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "Child").as[Int]) should be(Vector(2))
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ParentToChild").as[Int]) should be(Vector(1))
  }

  "a P1! to C1 relation" should "work on update" in {
    val project = SchemaDsl() { schema =>
      val child  = schema.model("Child").field_!("c", _.String, isUnique = true)
      val parent = schema.model("Parent").field_!("p", _.String, isUnique = true)
      parent.oneToOneRelation_!("childReq", "parentOpt", child, isRequiredOnFieldB = false)
    }
    database.setup(project)

    val res = server
      .executeQuerySimple(
        """mutation {
          |  createParent(data: {
          |    p: "p1"
          |    childReq: {
          |      create: {c: "c1"}
          |    }
          |  }){
          |  id
          |    childReq{
          |       id
          |    }
          |  }
          |}""".stripMargin,
        project
      )

    val parentId = res.pathAsString("data.createParent.id")

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ParentToChild").as[Int]) should be(Vector(1))

    val res2 = server.executeQuerySimple(
      s"""
         |mutation {
         |  updateParent(
         |  where: {id: "$parentId"}
         |  data:{
         |    p: "p2"
         |    childReq: {upsert: {
         |    where: {c: "c1"}
         |    update: {c: "updated C"}
         |    create :{c: "DOES NOT MATTER"}
         |    }}
         |  }){
         |    childReq {
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    res2.toString should be("""{"data":{"updateParent":{"childReq":{"c":"updated C"}}}}""")
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "Parent").as[Int]) should be(Vector(1))
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "Child").as[Int]) should be(Vector(1))
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ParentToChild").as[Int]) should be(Vector(1))
  }

  "a P1 to C1 relation" should "work on update" in {
    val project = SchemaDsl() { schema =>
      val child  = schema.model("Child").field_!("c", _.String, isUnique = true)
      val parent = schema.model("Parent").field_!("p", _.String, isUnique = true)
      parent.oneToOneRelation("childOpt", "parentOpt", child)
    }
    database.setup(project)

    val res = server
      .executeQuerySimple(
        """mutation {
          |  createParent(data: {
          |    p: "p1"
          |    childOpt: {
          |      create: {c: "c1"}
          |    }
          |  }){
          |    id
          |    childOpt{
          |       id
          |    }
          |  }
          |}""".stripMargin,
        project
      )

    val parentId = res.pathAsString("data.createParent.id")

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ParentToChild").as[Int]) should be(Vector(1))

    val res2 = server.executeQuerySimple(
      s"""
         |mutation {
         |  updateParent(
         |  where:{id: "$parentId"}
         |  data:{
         |    p: "p2"
         |    childOpt: {upsert: {
         |    where: {c: "c1"}
         |    update: {c: "updated C"}
         |    create :{c: "DOES NOT MATTER"}
         |    }}
         |  }){
         |    childOpt {
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    res2.toString should be("""{"data":{"updateParent":{"childOpt":{"c":"updated C"}}}}""")

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "Parent").as[Int]) should be(Vector(1))
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "Child").as[Int]) should be(Vector(1))
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ParentToChild").as[Int]) should be(Vector(1))
  }

  "a P1 to C1 relation" should "work on create" in {
    val project = SchemaDsl() { schema =>
      val child  = schema.model("Child").field_!("c", _.String, isUnique = true)
      val parent = schema.model("Parent").field_!("p", _.String, isUnique = true)
      parent.oneToOneRelation("childOpt", "parentOpt", child)
    }
    database.setup(project)

    val res = server
      .executeQuerySimple(
        """mutation {
          |  createParent(data: {
          |    p: "p1"
          |    childOpt: {
          |      create: {c: "c1"}
          |    }
          |  }){
          |    id
          |    childOpt{
          |       id
          |    }
          |  }
          |}""".stripMargin,
        project
      )

    val parentId = res.pathAsString("data.createParent.id")

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ParentToChild").as[Int]) should be(Vector(1))

    val res2 = server.executeQuerySimple(
      s"""
         |mutation {
         |  updateParent(
         |  where:{id: "$parentId"}
         |  data:{
         |    p: "p2"
         |    childOpt: {upsert: {
         |    where: {c: "DOES NOT EXIST"}
         |    update: {c: "DOES NOT MATTER"}
         |    create :{c: "new C"}
         |    }}
         |  }){
         |    childOpt {
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    res2.toString should be("""{"data":{"updateParent":{"childOpt":{"c":"new C"}}}}""")

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "Parent").as[Int]) should be(Vector(1))
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "Child").as[Int]) should be(Vector(2))
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ParentToChild").as[Int]) should be(Vector(1))
  }

  "a P1 to C1  relation with the parent without a relation" should "work with create" in {
    val project = SchemaDsl() { schema =>
      val child = schema.model("Child").field_!("c", _.String, isUnique = true)
      schema.model("Parent").field_!("p", _.String, isUnique = true).oneToOneRelation("childOpt", "parentOpt", child)
    }
    database.setup(project)

    val parent1Id = server
      .executeQuerySimple(
        """mutation {
          |  createParent(data: {p: "p1"})
          |  {
          |    id
          |  }
          |}""".stripMargin,
        project
      )
      .pathAsString("data.createParent.id")

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ParentToChild").as[Int]) should be(Vector(0))

    val res = server.executeQuerySimple(
      s"""
         |mutation {
         |  updateParent(
         |  where:{id: "$parent1Id"}
         |  data:{
         |    p: "p2"
         |    childOpt: {upsert: {
         |    where: {c: "DOES NOT EXIST"}
         |    update: {c: "DOES NOT MATTER"}
         |    create :{c: "new C"}
         |    }}
         |  }){
         |    childOpt {
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    res.toString should be("""{"data":{"updateParent":{"childOpt":{"c":"new C"}}}}""")
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "Parent").as[Int]) should be(Vector(1))
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "Child").as[Int]) should be(Vector(1))
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_RelayId").as[Int]) should be(Vector(2))
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ParentToChild").as[Int]) should be(Vector(1))
  }

  "a PM to C1!  relation with a child already in a relation" should "work with create" in {
    val project = SchemaDsl() { schema =>
      val child  = schema.model("Child").field_!("c", _.String, isUnique = true)
      val parent = schema.model("Parent").field_!("p", _.String, isUnique = true)
      parent.oneToManyRelation_!("childrenOpt", "parentReq", child)
    }
    database.setup(project)

    server.executeQuerySimple(
      """mutation {
        |  createParent(data: {
        |    p: "p1"
        |    childrenOpt: {
        |      create: {c: "c1"}
        |    }
        |  }){
        |    childrenOpt{
        |       c
        |    }
        |  }
        |}""".stripMargin,
      project
    )

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ParentToChild").as[Int]) should be(Vector(1))

    val res = server.executeQuerySimple(
      s"""
         |mutation {
         |  updateParent(
         |    where: {p: "p1"}
         |    data:{
         |    childrenOpt: {upsert: {
         |    where: {c: "DOES NOT EXIST"}
         |    update: {c: "DOES NOT MATTER"}
         |    create :{c: "c2"}
         |    }}
         |  }){
         |    childrenOpt {
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    res.toString should be("""{"data":{"updateParent":{"childrenOpt":[{"c":"c1"},{"c":"c2"}]}}}""")

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "Parent").as[Int]) should be(Vector(1))
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "Child").as[Int]) should be(Vector(2))
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_RelayId").as[Int]) should be(Vector(3))
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ParentToChild").as[Int]) should be(Vector(2))
  }

  "a PM to C1!  relation with a child already in a relation" should "work with update" in {
    val project = SchemaDsl() { schema =>
      val child  = schema.model("Child").field_!("c", _.String, isUnique = true)
      val parent = schema.model("Parent").field_!("p", _.String, isUnique = true)
      parent.oneToManyRelation_!("childrenOpt", "parentReq", child)
    }
    database.setup(project)

    server.executeQuerySimple(
      """mutation {
        |  createParent(data: {
        |    p: "p1"
        |    childrenOpt: {
        |      create: {c: "c1"}
        |    }
        |  }){
        |    childrenOpt{
        |       c
        |    }
        |  }
        |}""".stripMargin,
      project
    )

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ParentToChild").as[Int]) should be(Vector(1))

    val res = server.executeQuerySimple(
      s"""
         |mutation {
         |  updateParent(
         |    where: {p: "p1"}
         |    data:{
         |    childrenOpt: {upsert: {
         |    where: {c: "c1"}
         |    update: {c: "updated C"}
         |    create :{c: "DOES NOT MATTER"}
         |    }}
         |  }){
         |    childrenOpt {
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    res.toString should be("""{"data":{"updateParent":{"childrenOpt":[{"c":"updated C"}]}}}""")

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "Parent").as[Int]) should be(Vector(1))
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "Child").as[Int]) should be(Vector(1))
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_RelayId").as[Int]) should be(Vector(2))
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ParentToChild").as[Int]) should be(Vector(1))
  }

  "a P1 to C1!  relation with the parent and a child already in a relation" should "error in a nested mutation by unique for create" in {
    val project = SchemaDsl() { schema =>
      val parent = schema.model("Parent").field_!("p", _.String, isUnique = true)
      val child  = schema.model("Child").field_!("c", _.String, isUnique = true)
      child.oneToOneRelation_!("parentReq", "childOpt", parent, isRequiredOnFieldB = false)
    }
    database.setup(project)

    server.executeQuerySimple(
      """mutation {
        |  createParent(data: {
        |    p: "p1"
        |    childOpt: {
        |      create: {c: "c1"}
        |    }
        |  }){
        |    childOpt{
        |       c
        |    }
        |  }
        |}""".stripMargin,
      project
    )

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ChildToParent").as[Int]) should be(Vector(1))

    server.executeQuerySimpleThatMustFail(
      s"""
         |mutation {
         |  updateParent(
         |  where: {p: "p1"}
         |  data:{
         |    childOpt: {upsert: {
         |    where: {c: "DOES NOT EXIST"}
         |    update: {c: "DOES NOT MATTER"}
         |    create :{c: "new C"}
         |    }}
         |  }){
         |    childOpt {
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
      project,
      errorCode = 3042,
      errorContains = "The change you are trying to make would violate the required relation '_ChildToParent' between Child and Parent"
    )
  }

  "a P1 to C1!  relation with the parent and a child already in a relation" should "succeed in a nested mutation by unique for update" in {
    val project = SchemaDsl() { schema =>
      val parent = schema.model("Parent").field_!("p", _.String, isUnique = true)
      val child  = schema.model("Child").field_!("c", _.String, isUnique = true)
      child.oneToOneRelation_!("parentReq", "childOpt", parent, isRequiredOnFieldB = false)
    }
    database.setup(project)

    server.executeQuerySimple(
      """mutation {
        |  createParent(data: {
        |    p: "p1"
        |    childOpt: {
        |      create: {c: "c1"}
        |    }
        |  }){
        |    childOpt{
        |       c
        |    }
        |  }
        |}""".stripMargin,
      project
    )

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ChildToParent").as[Int]) should be(Vector(1))

    val res = server.executeQuerySimple(
      s"""
         |mutation {
         |  updateParent(
         |  where: {p: "p1"}
         |  data:{
         |    childOpt: {upsert: {
         |    where: {c: "c1"}
         |    update: {c: "updated C"}
         |    create :{c: "DOES NOT MATTER"}
         |    }}
         |  }){
         |    childOpt {
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    res.toString should be("""{"data":{"updateParent":{"childOpt":{"c":"updated C"}}}}""")

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "Parent").as[Int]) should be(Vector(1))
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "Child").as[Int]) should be(Vector(1))
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_RelayId").as[Int]) should be(Vector(2))
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ChildToParent").as[Int]) should be(Vector(1))
  }

  "a P1 to C1!  relation with the parent not already in a relation" should "work in a nested mutation by unique with create" in {
    val project = SchemaDsl() { schema =>
      val parent = schema.model("Parent").field_!("p", _.String, isUnique = true)
      val child  = schema.model("Child").field_!("c", _.String, isUnique = true)
      child.oneToOneRelation_!("parentReq", "childOpt", parent, isRequiredOnFieldB = false)
    }
    database.setup(project)

    server.executeQuerySimple(
      """mutation {
        |  createParent(data: {
        |    p: "p1"
        |
        |  }){
        |    p
        |  }
        |}""".stripMargin,
      project
    )

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ChildToParent").as[Int]) should be(Vector(0))

    val res = server.executeQuerySimple(
      s"""
         |mutation {
         |  updateParent(
         |  where: {p: "p1"}
         |  data:{
         |    childOpt: {upsert: {
         |    where: {c: "DOES NOT EXIST"}
         |    update: {c: "DOES NOT MATTER"}
         |    create :{c: "new C"}
         |    }}
         |  }){
         |    childOpt {
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    res.toString should be("""{"data":{"updateParent":{"childOpt":{"c":"new C"}}}}""")

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "Parent").as[Int]) should be(Vector(1))
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "Child").as[Int]) should be(Vector(1))
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_RelayId").as[Int]) should be(Vector(2))
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ChildToParent").as[Int]) should be(Vector(1))
  }

  "a PM to C1  relation with the parent already in a relation" should "work through a nested mutation by unique for create" in {
    val project = SchemaDsl() { schema =>
      val child  = schema.model("Child").field_!("c", _.String, isUnique = true)
      val parent = schema.model("Parent").field_!("p", _.String, isUnique = true)
      parent.oneToManyRelation("childrenOpt", "parentOpt", child)
    }
    database.setup(project)

    server
      .executeQuerySimple(
        """mutation {
          |  createParent(data: {
          |    p: "p1"
          |    childrenOpt: {
          |      create: [{c: "c1"}, {c: "c2"}]
          |    }
          |  }){
          |    childrenOpt{
          |       c
          |    }
          |  }
          |}""".stripMargin,
        project
      )

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ParentToChild").as[Int]) should be(Vector(2))

    val res = server.executeQuerySimple(
      s"""
         |mutation {
         |  updateParent(
         |  where: { p: "p1"}
         |  data:{
         |    childrenOpt: {upsert: [{
         |    where: {c: "DOES NOT EXIST"}
         |    update: {c: "DOES NOT MATTER"}
         |    create :{c: "new C"}
         |    }]}
         |  }){
         |    childrenOpt {
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    res.toString should be("""{"data":{"updateParent":{"childrenOpt":[{"c":"c1"},{"c":"c2"},{"c":"new C"}]}}}""")

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "Parent").as[Int]) should be(Vector(1))
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "Child").as[Int]) should be(Vector(3))
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_RelayId").as[Int]) should be(Vector(4))
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ParentToChild").as[Int]) should be(Vector(3))
  }

  "a PM to C1  relation with the parent already in a relation" should "work through a nested mutation by unique for update" in {
    val project = SchemaDsl() { schema =>
      val child  = schema.model("Child").field_!("c", _.String, isUnique = true)
      val parent = schema.model("Parent").field_!("p", _.String, isUnique = true)
      parent.oneToManyRelation("childrenOpt", "parentOpt", child)
    }
    database.setup(project)

    server
      .executeQuerySimple(
        """mutation {
          |  createParent(data: {
          |    p: "p1"
          |    childrenOpt: {
          |      create: [{c: "c1"}, {c: "c2"}]
          |    }
          |  }){
          |    childrenOpt{
          |       c
          |    }
          |  }
          |}""".stripMargin,
        project
      )

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ParentToChild").as[Int]) should be(Vector(2))

    val res = server.executeQuerySimple(
      s"""
         |mutation {
         |  updateParent(
         |  where: { p: "p1"}
         |  data:{
         |    childrenOpt: {upsert: [{
         |    where: {c: "c1"}
         |    update: {c: "updated C"}
         |    create :{c: "DOES NOT MATTER"}
         |    }]}
         |  }){
         |    childrenOpt {
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    res.toString should be("""{"data":{"updateParent":{"childrenOpt":[{"c":"updated C"},{"c":"c2"}]}}}""")

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "Parent").as[Int]) should be(Vector(1))
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "Child").as[Int]) should be(Vector(2))
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_RelayId").as[Int]) should be(Vector(3))
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ParentToChild").as[Int]) should be(Vector(2))
  }

  "a P1! to CM  relation with the parent already in a relation" should "work through a nested mutation by unique for update" in {
    val project = SchemaDsl() { schema =>
      val parent = schema.model("Parent").field_!("p", _.String, isUnique = true)
      val child  = schema.model("Child").field_!("c", _.String, isUnique = true)
      child.oneToManyRelation_!("parentsOpt", "childReq", parent)
    }
    database.setup(project)

    server.executeQuerySimple(
      """mutation {
        |  createParent(data: {
        |    p: "p1"
        |    childReq: {
        |      create: {c: "c1"}
        |    }
        |  }){
        |    childReq{
        |       c
        |    }
        |  }
        |}""".stripMargin,
      project
    )

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ChildToParent").as[Int]) should be(Vector(1))

    val res = server.executeQuerySimple(
      s"""
         |mutation {
         |  updateParent(
         |  where: {p: "p1"}
         |  data:{
         |    childReq: {upsert: {
         |    where: {c: "c1"}
         |    update: {c: "updated C"}
         |    create :{c: "DOES NOT MATTER"}
         |    }}
         |  }){
         |    childReq {
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    res.toString should be("""{"data":{"updateParent":{"childReq":{"c":"updated C"}}}}""")

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "Parent").as[Int]) should be(Vector(1))
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "Child").as[Int]) should be(Vector(1))
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_RelayId").as[Int]) should be(Vector(2))
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ChildToParent").as[Int]) should be(Vector(1))
  }

  "a P1! to CM  relation with the parent already in a relation" should "work through a nested mutation by unique for create" in {
    val project = SchemaDsl() { schema =>
      val parent = schema.model("Parent").field_!("p", _.String, isUnique = true)
      val child  = schema.model("Child").field_!("c", _.String, isUnique = true)
      child.oneToManyRelation_!("parentsOpt", "childReq", parent)
    }
    database.setup(project)

    server.executeQuerySimple(
      """mutation {
        |  createParent(data: {
        |    p: "p1"
        |    childReq: {
        |      create: {c: "c1"}
        |    }
        |  }){
        |    childReq{
        |       c
        |    }
        |  }
        |}""".stripMargin,
      project
    )

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ChildToParent").as[Int]) should be(Vector(1))

    val res = server.executeQuerySimple(
      s"""
         |mutation {
         |  updateParent(
         |  where: {p: "p1"}
         |  data:{
         |    childReq: {upsert: {
         |    where: {c: "DOES NOT EXIST"}
         |    update: {c: "DOES NOT MATTER"}
         |    create :{c: "new C"}
         |    }}
         |  }){
         |    childReq {
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    res.toString should be("""{"data":{"updateParent":{"childReq":{"c":"new C"}}}}""")

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "Parent").as[Int]) should be(Vector(1))
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "Child").as[Int]) should be(Vector(2))
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_RelayId").as[Int]) should be(Vector(3))
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ChildToParent").as[Int]) should be(Vector(1))
  }

  "a P1 to CM  relation with the child already in a relation" should "work through a nested mutation by unique for create" in {
    val project = SchemaDsl() { schema =>
      val parent = schema.model("Parent").field_!("p", _.String, isUnique = true)
      val child  = schema.model("Child").field_!("c", _.String, isUnique = true)
      child.oneToManyRelation("parentsOpt", "childOpt", parent)
    }
    database.setup(project)

    server.executeQuerySimple(
      """mutation {
        |  createParent(data: {
        |    p: "p1"
        |    childOpt: {
        |      create: {c: "c1"}
        |    }
        |  }){
        |    childOpt{
        |       c
        |    }
        |  }
        |}""".stripMargin,
      project
    )

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ChildToParent").as[Int]) should be(Vector(1))

    val res = server.executeQuerySimple(
      s"""
         |mutation {
         |  updateParent(
         |    where: {p: "p1"}
         |    data:{
         |    childOpt: {upsert: {
         |    where: {c: "DOES NOT EXIST"}
         |    update: {c: "DOES NOT MATTER"}
         |    create :{c: "new C"}
         |    }}
         |  }){
         |    childOpt{
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    res.toString should be("""{"data":{"updateParent":{"childOpt":{"c":"new C"}}}}""")

    server.executeQuerySimple(s"""query{children{c, parentsOpt{p}}}""", project).toString should be(
      """{"data":{"children":[{"c":"c1","parentsOpt":[]},{"c":"new C","parentsOpt":[{"p":"p1"}]}]}}""")

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "Parent").as[Int]) should be(Vector(1))
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "Child").as[Int]) should be(Vector(2))
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_RelayId").as[Int]) should be(Vector(3))
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ChildToParent").as[Int]) should be(Vector(1))
  }

  "a P1 to CM  relation with the child already in a relation" should "work through a nested mutation by unique for update" in {
    val project = SchemaDsl() { schema =>
      val parent = schema.model("Parent").field_!("p", _.String, isUnique = true)
      val child  = schema.model("Child").field_!("c", _.String, isUnique = true)
      child.oneToManyRelation("parentsOpt", "childOpt", parent)
    }
    database.setup(project)

    server.executeQuerySimple(
      """mutation {
        |  createParent(data: {
        |    p: "p1"
        |    childOpt: {
        |      create: {c: "c1"}
        |    }
        |  }){
        |    childOpt{
        |       c
        |    }
        |  }
        |}""".stripMargin,
      project
    )

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ChildToParent").as[Int]) should be(Vector(1))

    val res = server.executeQuerySimple(
      s"""
         |mutation {
         |  updateParent(
         |    where: {p: "p1"}
         |    data:{
         |    childOpt: {upsert: {
         |    where: {c: "c1"}
         |    update: {c: "updated C"}
         |    create :{c: "DOES NOT MATTER"}
         |    }}
         |  }){
         |    childOpt{
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    res.toString should be("""{"data":{"updateParent":{"childOpt":{"c":"updated C"}}}}""")

    server.executeQuerySimple(s"""query{children{c, parentsOpt{p}}}""", project).toString should be(
      """{"data":{"children":[{"c":"updated C","parentsOpt":[{"p":"p1"}]}]}}""")

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "Parent").as[Int]) should be(Vector(1))
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "Child").as[Int]) should be(Vector(1))
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_RelayId").as[Int]) should be(Vector(2))
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ChildToParent").as[Int]) should be(Vector(1))
  }

  "a PM to CM  relation with the children already in a relation" should "work through a nested mutation by unique for update" in {
    val project = SchemaDsl() { schema =>
      val parent = schema.model("Parent").field_!("p", _.String, isUnique = true)
      val child  = schema.model("Child").field_!("c", _.String, isUnique = true).manyToManyRelation("parentsOpt", "childrenOpt", parent)
    }
    database.setup(project)

    server.executeQuerySimple(
      """mutation {
        |  createParent(data: {
        |    p: "p1"
        |    childrenOpt: {
        |      create: [{c: "c1"},{c: "c2"}]
        |    }
        |  }){
        |    childrenOpt{
        |       c
        |    }
        |  }
        |}""".stripMargin,
      project
    )

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ChildToParent").as[Int]) should be(Vector(2))

    val res = server.executeQuerySimple(
      s"""
         |mutation {
         |  updateParent(
         |  where: { p: "p1"}
         |  data:{
         |    childrenOpt: {upsert: [{
         |    where: {c: "c2"}
         |    update: {c: "updated C"}
         |    create :{c: "DOES NOT MATTER"}
         |    }]}
         |  }){
         |    childrenOpt{
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    res.toString should be("""{"data":{"updateParent":{"childrenOpt":[{"c":"c1"},{"c":"updated C"}]}}}""")

    server.executeQuerySimple(s"""query{children{c, parentsOpt{p}}}""", project).toString should be(
      """{"data":{"children":[{"c":"c1","parentsOpt":[{"p":"p1"}]},{"c":"updated C","parentsOpt":[{"p":"p1"}]}]}}""")

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "Parent").as[Int]) should be(Vector(1))
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "Child").as[Int]) should be(Vector(2))
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_RelayId").as[Int]) should be(Vector(3))
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ChildToParent").as[Int]) should be(Vector(2))
  }

  "a PM to CM  relation with the children already in a relation" should "work through a nested mutation by unique for create" in {
    val project = SchemaDsl() { schema =>
      val parent = schema.model("Parent").field_!("p", _.String, isUnique = true)
      val child  = schema.model("Child").field_!("c", _.String, isUnique = true).manyToManyRelation("parentsOpt", "childrenOpt", parent)
    }
    database.setup(project)

    server.executeQuerySimple(
      """mutation {
        |  createParent(data: {
        |    p: "p1"
        |    childrenOpt: {
        |      create: [{c: "c1"},{c: "c2"}]
        |    }
        |  }){
        |    childrenOpt{
        |       c
        |    }
        |  }
        |}""".stripMargin,
      project
    )

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ChildToParent").as[Int]) should be(Vector(2))

    val res = server.executeQuerySimple(
      s"""
         |mutation {
         |  updateParent(
         |  where: { p: "p1"}
         |  data:{
         |    childrenOpt: {upsert: [{
         |    where: {c: "DOES NOT EXIST"}
         |    update: {c: "DOES NOT MATTER"}
         |    create :{c: "updated C"}
         |    }]}
         |  }){
         |    childrenOpt{
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    res.toString should be("""{"data":{"updateParent":{"childrenOpt":[{"c":"c1"},{"c":"c2"},{"c":"updated C"}]}}}""")

    server.executeQuerySimple(s"""query{children{c, parentsOpt{p}}}""", project).toString should be(
      """{"data":{"children":[{"c":"c1","parentsOpt":[{"p":"p1"}]},{"c":"c2","parentsOpt":[{"p":"p1"}]},{"c":"updated C","parentsOpt":[{"p":"p1"}]}]}}""")

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "Parent").as[Int]) should be(Vector(1))
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "Child").as[Int]) should be(Vector(3))
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_RelayId").as[Int]) should be(Vector(4))
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ChildToParent").as[Int]) should be(Vector(3))
  }

  "a one to many relation" should "be upsertable by id through a nested mutation" in {
    val project = SchemaDsl() { schema =>
      val comment = schema.model("Comment").field("text", _.String)
      schema.model("Todo").oneToManyRelation("comments", "todo", comment)
    }
    database.setup(project)

    val createResult = server.executeQuerySimple(
      """mutation {
        |  createTodo(
        |    data: {
        |      comments: {
        |        create: [{text: "comment1"}, {text: "comment2"}]
        |      }
        |    }
        |  ){ 
        |    id 
        |    comments { id }
        |  } 
        |}""".stripMargin,
      project
    )

    val todoId     = createResult.pathAsString("data.createTodo.id")
    val comment1Id = createResult.pathAsString("data.createTodo.comments.[0].id")

    val result = server.executeQuerySimple(
      s"""mutation {
         |  updateTodo(
         |    where: {
         |      id: "$todoId"
         |    }
         |    data:{
         |      comments: {
         |        upsert: [
         |          {where: {id: "$comment1Id"}, update: {text: "update comment1"}, create: {text: "irrelevant"}},
         |          {where: {id: "non-existent-id"}, update: {text: "irrelevant"}, create: {text: "new comment3"}},
         |        ]
         |      }
         |    }
         |  ){
         |    comments {
         |      text
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    mustBeEqual(result.pathAsString("data.updateTodo.comments.[0].text").toString, """update comment1""")
    mustBeEqual(result.pathAsString("data.updateTodo.comments.[1].text").toString, """comment2""")
    mustBeEqual(result.pathAsString("data.updateTodo.comments.[2].text").toString, """new comment3""")
  }

  "a one to many relation" should "only update nodes that are connected" in {
    val project = SchemaDsl() { schema =>
      val comment = schema.model("Comment").field("text", _.String)
      schema.model("Todo").oneToManyRelation("comments", "todo", comment)
    }
    database.setup(project)

    val createResult = server.executeQuerySimple(
      """mutation {
        |  createTodo(
        |    data: {
        |      comments: {
        |        create: [{text: "comment1"}]
        |      }
        |    }
        |  ){ 
        |    id 
        |    comments { id }
        |  } 
        |}""".stripMargin,
      project
    )
    val todoId     = createResult.pathAsString("data.createTodo.id")
    val comment1Id = createResult.pathAsString("data.createTodo.comments.[0].id")

    val commentResult = server.executeQuerySimple(
      """mutation {
        |  createComment(
        |    data: {
        |      text: "comment2"
        |    }
        |  ){
        |    id
        |  }
        |}""".stripMargin,
      project
    )
    val comment2Id = commentResult.pathAsString("data.createComment.id")

    val result = server.executeQuerySimple(
      s"""mutation {
         |  updateTodo(
         |    where: {
         |      id: "$todoId"
         |    }
         |    data:{
         |      comments: {
         |        upsert: [
         |          {where: {id: "$comment1Id"}, update: {text: "update comment1"}, create: {text: "irrelevant"}},
         |          {where: {id: "$comment2Id"}, update: {text: "irrelevant"}, create: {text: "new comment3"}},
         |        ]
         |      }
         |    }
         |  ){
         |    comments {
         |      text
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    mustBeEqual(result.pathAsString("data.updateTodo.comments.[0].text").toString, """update comment1""")
    mustBeEqual(result.pathAsString("data.updateTodo.comments.[1].text").toString, """new comment3""")
  }

  "a one to many relation" should "generate helpfull error messages" in {
    val project = SchemaDsl() { schema =>
      val comment = schema.model("Comment").field("text", _.String).field("uniqueComment", _.String, isUnique = true)
      schema.model("Todo").field("uniqueTodo", _.String, isUnique = true).oneToManyRelation("comments", "todo", comment)
    }
    database.setup(project)

    val createResult = server.executeQuerySimple(
      """mutation {
        |  createTodo(
        |    data: {
        |      uniqueTodo: "todo"
        |      comments: {
        |        create: [{text: "comment1", uniqueComment: "comments"}]
        |      }
        |    }
        |  ){
        |    id
        |    comments { id }
        |  }
        |}""".stripMargin,
      project
    )
    val todoId     = createResult.pathAsString("data.createTodo.id")
    val comment1Id = createResult.pathAsString("data.createTodo.comments.[0].id")

    server.executeQuerySimpleThatMustFail(
      s"""mutation {
         |  updateTodo(
         |    where: {
         |      id: "$todoId"
         |    }
         |    data:{
         |      comments: {
         |        upsert: [
         |          {where: {id: "NotExistant"}, update: {text: "update comment1"}, create: {text: "irrelevant", uniqueComment: "comments"}},
         |        ]
         |      }
         |    }
         |  ){
         |    comments {
         |      text
         |    }
         |  }
         |}
      """.stripMargin,
      project,
      errorCode = 3010,
      errorContains = "A unique constraint would be violated on Comment. Details: Field name = uniqueComment"
    )
  }

  "a deeply nested mutation" should "execute all levels of the mutation" ignore {
    val project = SchemaDsl() { schema =>
      val list = schema.model("List").field_!("name", _.String)
      val todo = schema.model("Todo").field_!("title", _.String)
      val tag  = schema.model("Tag").field_!("name", _.String)

      list.oneToManyRelation("todos", "list", todo)
      todo.oneToManyRelation("tags", "todo", tag)
    }
    database.setup(project)

    val createMutation =
      """
        |mutation  {
        |  createList(data: {
        |    name: "the list",
        |    todos: {
        |      create: [
        |        {
        |          title: "the todo"
        |          tags: {
        |            create: [
        |              {name: "the tag"}
        |            ]
        |          }
        |        }
        |      ]
        |    }
        |  }) {
        |    id
        |    todos {
        |      id
        |      tags {
        |        id
        |      }
        |    }
        |  }
        |}
      """.stripMargin

    val createResult = server.executeQuerySimple(createMutation, project)
    val listId       = createResult.pathAsString("data.createList.id")
    val todoId       = createResult.pathAsString("data.createList.todos.[0].id")
    val tagId        = createResult.pathAsString("data.createList.todos.[0].tags.[0].id")

    val updateMutation =
      s"""
         |mutation  {
         |  updateList(
         |    where: {
         |      id: "$listId"
         |    }
         |    data: {
         |      todos: {
         |        upsert: [
         |          {
         |            where: { id: "$todoId" }
         |            create: { title: "irrelevant" }
         |            update: {
         |              tags: {
         |                upsert: [
         |                  {
         |                    where: { id: "$tagId" }
         |                    update: { name: "updated tag" }
         |                    create: { name: "irrelevant" }
         |                  },
         |                  {
         |                    where: { id: "non-existent-id" }
         |                    update: { name: "irrelevant" }
         |                    create: { name: "new tag" }
         |                  },
         |                ]
         |              }
         |            }
         |          }
         |        ]
         |      }
         |    }
         |  ) {
         |    name
         |    todos {
         |      title
         |      tags {
         |        name
         |      }
         |    }
         |  }
         |}
      """.stripMargin

    val result = server.executeQuerySimple(updateMutation, project)
    result.pathAsString("data.updateList.todos.[0].tags.[0].name") should equal("updated tag")
    result.pathAsString("data.updateList.todos.[0].tags.[1].name") should equal("new tag")
  }

  "a deeply nested mutation with upsert" should "work on miss on id" in {
    val project = SchemaDsl() { schema =>
      val list = schema.model("List").field_!("name", _.String)
      val todo = schema.model("Todo").field_!("title", _.String)
      val tag  = schema.model("Tag").field_!("name", _.String)

      list.oneToManyRelation("todos", "list", todo)
      todo.oneToManyRelation("tags", "todo", tag)
    }
    database.setup(project)

    val createMutation =
      """
        |mutation  {
        |  createList(data: {
        |    name: "the list",
        |    todos: {
        |      create: [
        |        {
        |          title: "the todo"
        |          tags: {
        |            create: [
        |              {name: "the tag"}
        |            ]
        |          }
        |        }
        |      ]
        |    }
        |  }) {
        |    id
        |    todos {
        |      id
        |      tags {
        |        id
        |      }
        |    }
        |  }
        |}
      """.stripMargin

    val createResult = server.executeQuerySimple(createMutation, project)
    val listId       = createResult.pathAsString("data.createList.id")
    val todoId       = createResult.pathAsString("data.createList.todos.[0].id")
    val tagId        = createResult.pathAsString("data.createList.todos.[0].tags.[0].id")

    val updateMutation =
      s"""
         |mutation  {
         |  updateList(
         |    where: {
         |      id: "$listId"
         |    }
         |    data: {
         |      todos: {
         |        upsert: [
         |          {
         |            where: { id: "Does not Exist" }
         |            create: { title: "new todo" tags: { create: [ {name: "the tag"}]}}
         |            update: { title: "updated todo"}
         |          }
         |        ]
         |      }
         |    }
         |  ) {
         |    name
         |    todos {
         |      title
         |      tags {
         |        name
         |      }
         |    }
         |  }
         |}
      """.stripMargin

    val result = server.executeQuerySimple(updateMutation, project)
    result.pathAsString("data.updateList.todos.[0].tags.[0].name") should equal("the tag")
  }

}
