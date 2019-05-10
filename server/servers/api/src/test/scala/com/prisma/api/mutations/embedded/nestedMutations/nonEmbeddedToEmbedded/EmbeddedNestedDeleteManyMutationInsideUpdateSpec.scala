package com.prisma.api.mutations.embedded.nestedMutations.nonEmbeddedToEmbedded

import com.prisma.api.ApiSpecBase
import com.prisma.api.mutations.nonEmbedded.nestedMutations.SchemaBaseV11
import com.prisma.shared.models.ConnectorCapability.{EmbeddedTypesCapability, JoinRelationLinksCapability}
import com.prisma.shared.models.Project
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class EmbeddedNestedDeleteManyMutationInsideUpdateSpec extends FlatSpec with Matchers with ApiSpecBase with SchemaBaseV11 {
  override def runOnlyForCapabilities = Set(EmbeddedTypesCapability)

  "A 1-n relation" should "error if trying to use nestedDeleteMany" in {
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
         |    childOpt: {deleteMany: {
         |        where:{c: "c"}
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
      errorContains = """ Reason: 'childOpt.deleteMany' Field 'deleteMany' is not defined in the input type 'ChildUpdateOneInput'."""
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
         |    childrenOpt: {deleteMany: {
         |          c_contains:"c1"
         |      }
         |    }
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
      """{"data":{"parents":[{"p":"p1","childrenOpt":[{"c":"c2","test":null}]},{"p":"p2","childrenOpt":[{"c":"c3","test":null},{"c":"c4","test":null}]}]}}""")
  }

  "a PM to CM  relation " should "work with several deleteManys" in {
    val project = SchemaDsl.fromStringV11() { embeddedPM }
    database.setup(project)

    setupData(project)

    server.query(
      s"""
         |mutation {
         |  updateParent(
         |    where: {p: "p1"}
         |    data:{
         |    childrenOpt: {deleteMany: [
         |    {
         |        c_contains:"1"
         |    },
         |    {
         |        c_contains:"3"
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
      """{"data":{"parents":[{"p":"p1","childrenOpt":[{"c":"c2","test":null}]},{"p":"p2","childrenOpt":[{"c":"c3","test":null},{"c":"c4","test":null}]}]}}""")
  }

  "a PM to CM  relation " should "work with empty Filter" in {
    val project = SchemaDsl.fromStringV11() { embeddedPM }
    database.setup(project)

    setupData(project)

    server.query(
      s"""
         |mutation {
         |  updateParent(
         |    where: {p: "p1"}
         |    data:{
         |    childrenOpt: {deleteMany: [
         |    {}
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
      """{"data":{"parents":[{"p":"p1","childrenOpt":[]},{"p":"p2","childrenOpt":[{"c":"c3","test":null},{"c":"c4","test":null}]}]}}""")
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
         |    childrenOpt: {deleteMany: [
         |    {
         |        c_contains:"3"
         |    },
         |    {
         |        c_contains:"4"
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

  "Deleting toMany relations if they have a id" should "work" in {

    val project = SchemaDsl.fromStringV11() {
      """type Top {
        |   id: ID! @id
        |   unique: Int! @unique
        |   name: String!
        |   middle: [Middle]
        |}
        |
        |type Middle @embedded{
        |   id: ID! @id
        |   int: Int!
        |   name: String!
        |}"""
    }

    database.setup(project)

    val res = server.query(
      s"""mutation {
         |   createTop(data: {
         |   unique: 1,
         |   name: "Top",
         |   middle: {create:[{
         |      int: 11,
         |      name: "Middle"
         |   },
         |   {
         |      int: 12,
         |      name: "Middle2"
         |   }
         |
         |   ]}
         |}){
         |  unique,
         |  middle{
         |    int
         |  }
         |}}""".stripMargin,
      project
    )

    res.toString should be("""{"data":{"createTop":{"unique":1,"middle":[{"int":11},{"int":12}]}}}""")

    val middleId = server.query("""query{top(where:{unique: 1}){middle{id}}}""", project).pathAsString("""data.top.middle.[0].id""")

    val res2 = server.query(
      s"""mutation {
         |   updateTop(
         |   where:{unique: 1}
         |   data: {
         |      name: "Top2",
         |      middle: {delete:{id:"$middleId"}}
         |}){
         |  unique,
         |  middle{
         |    int
         |  }
         |}}""".stripMargin,
      project
    )

    res2.toString should be("""{"data":{"updateTop":{"unique":1,"middle":[{"int":12}]}}}""")
  }

}
