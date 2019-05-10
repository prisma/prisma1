package com.prisma.api.mutations.embedded.nestedMutations.nonEmbeddedToEmbedded

import com.prisma.api.ApiSpecBase
import com.prisma.api.mutations.nonEmbedded.nestedMutations.SchemaBaseV11
import com.prisma.shared.models.ConnectorCapability.EmbeddedTypesCapability
import com.prisma.shared.models.Project
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class EmbeddedNestedUpdateManyMutationInsideUpdateSpec extends FlatSpec with Matchers with ApiSpecBase with SchemaBaseV11 {
  override def runOnlyForCapabilities = Set(EmbeddedTypesCapability)

  "A 1-n relation" should "error if trying to use nestedUpdateMany" in {
    val project = SchemaDsl.fromStringV11() { embeddedP1opt }
    database.setup(project)

    val parent1Id = server
      .query(
        """mutation {
          |  createParent(data: {p: "p1"})
          |  {
          |    id
          |  }
          |}""".stripMargin,
        project
      )
      .pathAsString("data.createParent.id")

    val res = server.queryThatMustFail(
      s"""
         |mutation {
         |  updateParent(
         |  where:{id: "$parent1Id"}
         |  data:{
         |    p: "p2"
         |    childOpt: {updateMany: {
         |        where:{c: "c"}
         |        data: {c: "newC"}
         |    
         |    }}
         |  }){
         |    childOpt {
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
      project,
      errorCode = 0,
      errorContains = """Reason: 'childOpt.updateMany' Field 'updateMany' is not defined in the input type 'ChildUpdateOneInput'."""
    )
  }

  "a PM to CM  relation " should "work" in {
    val project = SchemaDsl.fromStringV11() { embeddedPM }
    database.setup(project)

    setupData(project)

    server.query(
      s"""
         |mutation {
         |  updateParent(
         |    where: {p: "p1"}
         |    data:{
         |    childrenOpt: {updateMany: {
         |        where: {c_contains:"c"}
         |        data: {test: "updated"}
         |    }}
         |  }){
         |    childrenOpt {
         |      c
         |      test
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    dataResolver(project).countByTable(project.schema.getModelByName_!("Parent").dbName).await should be(2)

    server.query("query{parents{p,childrenOpt{c, test}}}", project).toString() should be(
      """{"data":{"parents":[{"p":"p1","childrenOpt":[{"c":"c1","test":"updated"},{"c":"c2","test":"updated"}]},{"p":"p2","childrenOpt":[{"c":"c3","test":null},{"c":"c4","test":null}]}]}}""")
  }

  "a PM to CM  relation " should "work with several updateManys" in {
    val project = SchemaDsl.fromStringV11() { embeddedPM }
    database.setup(project)

    setupData(project)

    server.query(
      s"""
         |mutation {
         |  updateParent(
         |    where: {p: "p1"}
         |    data:{
         |    childrenOpt: {updateMany: [
         |    {
         |        where: {c_contains:"1"}
         |        data: {test: "updated1"}
         |    },
         |    {
         |        where: {c_contains:"2"}
         |        data: {test: "updated2"}
         |    }
         |    ]}
         |  }){
         |    childrenOpt {
         |      c
         |      test
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    dataResolver(project).countByTable(project.schema.getModelByName_!("Parent").dbName).await should be(2)

    server.query("query{parents{p,childrenOpt{c, test}}}", project).toString() should be(
      """{"data":{"parents":[{"p":"p1","childrenOpt":[{"c":"c1","test":"updated1"},{"c":"c2","test":"updated2"}]},{"p":"p2","childrenOpt":[{"c":"c3","test":null},{"c":"c4","test":null}]}]}}""")
  }

  "a PM to CM relation " should "work with empty Filter" in {
    val project = SchemaDsl.fromStringV11() { embeddedPM }
    database.setup(project)

    setupData(project)

    server.query(
      s"""
         |mutation {
         |  updateParent(
         |    where: {p: "p1"}
         |    data:{
         |    childrenOpt: {updateMany: [
         |    {
         |        where: {}
         |        data: {test: "updated1"}
         |    }
         |    ]}
         |  }){
         |    childrenOpt {
         |      c
         |      test
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    dataResolver(project).countByTable(project.schema.getModelByName_!("Parent").dbName).await should be(2)

    server.query("query{parents{p,childrenOpt{c, test}}}", project).toString() should be(
      """{"data":{"parents":[{"p":"p1","childrenOpt":[{"c":"c1","test":"updated1"},{"c":"c2","test":"updated1"}]},{"p":"p2","childrenOpt":[{"c":"c3","test":null},{"c":"c4","test":null}]}]}}""")
  }

  "a PM to CM  relation " should "not change anything when there is no hit" in {
    val project = SchemaDsl.fromStringV11() { embeddedPM }
    database.setup(project)

    setupData(project)

    server.query(
      s"""
         |mutation {
         |  updateParent(
         |    where: {p: "p1"}
         |    data:{
         |    childrenOpt: {updateMany: [
         |    {
         |        where: {c_contains:"3"}
         |        data: {test: "updated3"}
         |    },
         |    {
         |        where: {c_contains:"4"}
         |        data: {test: "updated4"}
         |    }
         |    ]}
         |  }){
         |    childrenOpt {
         |      c
         |      test
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    dataResolver(project).countByTable(project.schema.getModelByName_!("Parent").dbName).await should be(2)

    server.query("query{parents{p,childrenOpt{c, test}}}", project).toString() should be(
      """{"data":{"parents":[{"p":"p1","childrenOpt":[{"c":"c1","test":null},{"c":"c2","test":null}]},{"p":"p2","childrenOpt":[{"c":"c3","test":null},{"c":"c4","test":null}]}]}}""")
  }

  //optional ordering

  "a PM to CM  relation " should "work when multiple filters hit" in {
    val project = SchemaDsl.fromStringV11() { embeddedPM }
    database.setup(project)

    setupData(project)

    server.queryThatMustFail(
      s"""
         |mutation {
         |  updateParent(
         |    where: {p: "p1"}
         |    data:{
         |    childrenOpt: {updateMany: [
         |    {
         |        where: {c_contains:"c"}
         |        data: {test: "updated1"}
         |    },
         |    {
         |        where: {c_contains:"c1"}
         |        data: {test: "updated2"}
         |    }
         |    ]}
         |  }){
         |    childrenOpt {
         |      c
         |      test
         |    }
         |  }
         |}
      """.stripMargin,
      project,
      errorCode = 3043,
      errorContains =
        """You have several updates affecting the same area of the document underlying Parent. MongoMessage: Update created a conflict at 'childrenOpt.0.test'"""
    )
  }

  private def setupData(project: Project) = {
    server.query(
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

    server.query(
      """mutation {
        |  createParent(data: {
        |    p: "p2"
        |    childrenOpt: {
        |      create: [{c: "c3"},{c: "c4"}]
        |    }
        |  }){
        |    childrenOpt{
        |       c
        |    }
        |  }
        |}""".stripMargin,
      project
    )
  }

}
