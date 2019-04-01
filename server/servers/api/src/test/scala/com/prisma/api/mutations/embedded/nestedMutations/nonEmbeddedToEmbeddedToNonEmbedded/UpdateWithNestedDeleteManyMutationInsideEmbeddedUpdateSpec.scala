package com.prisma.api.mutations.embedded.nestedMutations.nonEmbeddedToEmbeddedToNonEmbedded

import com.prisma.api.ApiSpecBase
import com.prisma.api.mutations.nonEmbedded.nestedMutations.{SchemaBase, SchemaBaseV11}
import com.prisma.shared.models.ConnectorCapability.{EmbeddedTypesCapability, JoinRelationLinksCapability}
import com.prisma.shared.models.Project
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class UpdateWithNestedDeleteManyMutationInsideEmbeddedUpdateSpec extends FlatSpec with Matchers with ApiSpecBase with SchemaBaseV11 {
  override def runOnlyForCapabilities = Set(JoinRelationLinksCapability, EmbeddedTypesCapability)

  "a PM to CM  relation " should "work" in {
    val project = SchemaDsl.fromString() { embedddedToJoinFriendsOpt }
    database.setup(project)

    setUpData(project)

    server.query(
      s"""
         |mutation {
         |  updateParent(
         |    where: {p: "p1"}
         |    data:{
         |    children: {update: {
         |        where: {c:"c1"}
         |        data: {
         |          friendsOpt: {deleteMany:{
         |              f_contains: "1"
         |          }}
         |        }
         |    }}
         |  }){
         |    children {
         |      c
         |      friendsOpt{
         |         f
         |      }
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    dataResolver(project).countByTable(project.schema.getModelByName_!("Parent").dbName).await should be(2)
    dataResolver(project).countByTable(project.schema.getModelByName_!("Friend").dbName).await should be(7)

    server.query("query{parents{p,children{c, friendsOpt{f}}}}", project).toString() should be(
      """{"data":{"parents":[{"p":"p1","children":[{"c":"c1","friendsOpt":[{"f":"f2"}]},{"c":"c2","friendsOpt":[{"f":"f3"},{"f":"f4"}]}]},{"p":"p2","children":[{"c":"c3","friendsOpt":[{"f":"f5"},{"f":"f6"}]},{"c":"c4","friendsOpt":[{"f":"f7"},{"f":"f8"}]}]}]}}""")
  }

  "a PM to CM  relation " should "work with multiple filters" in {
    val project = SchemaDsl.fromString() { embedddedToJoinFriendsOpt }
    database.setup(project)

    setUpData(project)

    server.query(
      s"""
         |mutation {
         |  updateParent(
         |    where: {p: "p1"}
         |    data:{
         |    children: {update: {
         |        where: {c:"c1"}
         |        data: {
         |          friendsOpt: {deleteMany:[
         |            {f_contains: "1"},
         |            {f_contains: "2"}
         |          ]}
         |        }
         |    }}
         |  }){
         |    children {
         |      c
         |      friendsOpt{
         |         f
         |      }
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    dataResolver(project).countByTable(project.schema.getModelByName_!("Parent").dbName).await should be(2)

    server.query("query{parents{p,children{c, friendsOpt{f}}}}", project).toString() should be(
      """{"data":{"parents":[{"p":"p1","children":[{"c":"c1","friendsOpt":[]},{"c":"c2","friendsOpt":[{"f":"f3"},{"f":"f4"}]}]},{"p":"p2","children":[{"c":"c3","friendsOpt":[{"f":"f5"},{"f":"f6"}]},{"c":"c4","friendsOpt":[{"f":"f7"},{"f":"f8"}]}]}]}}""")
  }

  "a PM to CM  relation " should "work with empty filter" in {
    val project = SchemaDsl.fromString() { embedddedToJoinFriendsOpt }
    database.setup(project)

    setUpData(project)

    server.query(
      s"""
         |mutation {
         |  updateParent(
         |    where: {p: "p1"}
         |    data:{
         |    children: {update: {
         |        where: {c:"c1"}
         |        data: {
         |          friendsOpt: {deleteMany:[
         |            {}
         |          ]}
         |        }
         |    }}
         |  }){
         |    children {
         |      c
         |      friendsOpt{
         |         f
         |      }
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    dataResolver(project).countByTable(project.schema.getModelByName_!("Parent").dbName).await should be(2)
    dataResolver(project).countByTable(project.schema.getModelByName_!("Friend").dbName).await should be(6)

    server.query("query{parents{p,children{c, friendsOpt{f}}}}", project).toString() should be(
      """{"data":{"parents":[{"p":"p1","children":[{"c":"c1","friendsOpt":[]},{"c":"c2","friendsOpt":[{"f":"f3"},{"f":"f4"}]}]},{"p":"p2","children":[{"c":"c3","friendsOpt":[{"f":"f5"},{"f":"f6"}]},{"c":"c4","friendsOpt":[{"f":"f7"},{"f":"f8"}]}]}]}}""")
  }

  "a PM to CM  relation " should "work when there is no hit" in {
    val project = SchemaDsl.fromString() { embedddedToJoinFriendsOpt }
    database.setup(project)

    setUpData(project)

    server.query(
      s"""
         |mutation {
         |  updateParent(
         |    where: {p: "p1"}
         |    data:{
         |    children: {update: {
         |        where: {c:"c1"}
         |        data: {
         |          friendsOpt: {deleteMany:[
         |            {
         |             f_contains: "Z"
         |            }
         |          ]}
         |        }
         |    }}
         |  }){
         |    children {
         |      c
         |      friendsOpt{
         |         f
         |      }
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    dataResolver(project).countByTable(project.schema.getModelByName_!("Parent").dbName).await should be(2)
    dataResolver(project).countByTable(project.schema.getModelByName_!("Friend").dbName).await should be(8)

    server.query("query{parents{p,children{c, friendsOpt{f}}}}", project).toString() should be(
      """{"data":{"parents":[{"p":"p1","children":[{"c":"c1","friendsOpt":[{"f":"f1"},{"f":"f2"}]},{"c":"c2","friendsOpt":[{"f":"f3"},{"f":"f4"}]}]},{"p":"p2","children":[{"c":"c3","friendsOpt":[{"f":"f5"},{"f":"f6"}]},{"c":"c4","friendsOpt":[{"f":"f7"},{"f":"f8"}]}]}]}}""")
  }

  private def setUpData(project: Project) = {
    server.query(
      """mutation {
        |  createParent(data: {
        |    p: "p1"
        |    children: {
        |      create: [
        |       {
        |         c: "c1"
        |         friendsOpt: {create:[{f: "f1"},{f: "f2"}]}
        |       },
        |       {
        |         c: "c2"
        |         friendsOpt: {create:[{f: "f3"},{f: "f4"}]}
        |       }
        |      ]
        |    }
        |  }){
        |    children{
        |       c
        |       friendsOpt{
        |         f
        |       }
        |    }
        |  }
        |}""".stripMargin,
      project
    )

    server.query(
      """mutation {
        |  createParent(data: {
        |    p: "p2"
        |    children: {
        |      create: [
        |       {
        |         c: "c3"
        |         friendsOpt: {create:[{f: "f5"},{f: "f6"}]}
        |       },
        |       {
        |         c: "c4"
        |         friendsOpt: {create:[{f: "f7"},{f: "f8"}]}
        |       }
        |      ]
        |    }
        |  }){
        |    children{
        |       c
        |       friendsOpt{
        |         f
        |       }
        |    }
        |  }
        |}""".stripMargin,
      project
    )
  }
}
