package com.prisma.api.mutations.embedded.nestedMutations.nonEmbeddedToEmbeddedToNonEmbedded

import com.prisma.api.ApiSpecBase
import com.prisma.api.mutations.nonEmbedded.nestedMutations.SchemaBaseV11
import com.prisma.shared.models.ConnectorCapability.EmbeddedTypesCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class UpdateWithNestedDeleteMutationInsideEmbeddedUpdateSpec extends FlatSpec with Matchers with ApiSpecBase with SchemaBaseV11 {
  override def runOnlyForCapabilities = Set(EmbeddedTypesCapability)

  "a FriendReq relation" should "be possible" in {

    val project = SchemaDsl.fromStringV11() { embedddedToJoinFriendReq }

    database.setup(project)

    val create = server
      .query(
        """mutation {
          |  createParent(
          |  
          |  data: {
          |    p: "p1"
          |    children: {create:{
          |       c: "c1"
          |       friendReq:{create:{f: "f1"}}
          |    }}
          |  }){
          |    p
          |    children{
          |       id
          |       c
          |       friendReq{
          |         f
          |       }
          |
          |    }
          |  }
          |}""",
        project
      )

    val idOfC1 = create.pathAsString("data.createParent.children.[0].id")

    val update = server
      .queryThatMustFail(
        s"""mutation {
          |  updateParent(
          |  where:{p:"p1"}
          |  data: {
          |    children: {update:{
          |       where:{id: "$idOfC1"}
          |       data:{
          |           friendReq:{delete:true}
          |       }
          |    }}
          |  }){
          |    p
          |    children{
          |       c
          |       friendReq{
          |         f
          |       }
          |
          |    }
          |  }
          |}""",
        project,
        0,
        errorContains = """Reason: 'children.update[0].data.friendReq.delete' Field 'delete' is not defined in the input type 'FriendUpdateOneRequiredInput'."""
      )

    server.query("query{friends{f}}", project).toString should be("""{"data":{"friends":[{"f":"f1"}]}}""")
  }

  "a FriendOpt relation" should "be possible" in {

    val project = SchemaDsl.fromStringV11() { embedddedToJoinFriendOpt }

    database.setup(project)

    val create = server
      .query(
        """mutation {
          |  createParent(
          |  
          |  data: {
          |    p: "p1"
          |    children: {create:{
          |       c: "c1"
          |       friendOpt:{create:{f: "f1"}}
          |    }}
          |  }){
          |    p
          |    children{
          |       id
          |       c
          |       friendOpt{
          |         f
          |       }
          |
          |    }
          |  }
          |}""",
        project
      )

    val idOfC1 = create.pathAsString("data.createParent.children.[0].id")

    val update = server
      .query(
        s"""mutation {
          |  updateParent(
          |  where:{p:"p1"}
          |  data: {
          |    children: {update:{
          |       where:{id: "$idOfC1"}
          |       data:{
          |           friendOpt:{delete: true}
          |       }
          |    }}
          |  }){
          |    p
          |    children{
          |       c
          |       friendOpt{
          |         f
          |       }
          |
          |    }
          |  }
          |}""",
        project
      )

    update.toString should include("""{"data":{"updateParent":{"p":"p1","children":[{"c":"c1","friendOpt":null}]}}}""")

    server.query("query{friends{f}}", project).toString should be("""{"data":{"friends":[]}}""")

  }

  "a FriendsOpt relation" should "be possible" in {

    val project = SchemaDsl.fromStringV11() { embedddedToJoinFriendsOpt }

    database.setup(project)

    val create = server
      .query(
        """mutation {
          |  createParent(
          |  data: {
          |    p: "p1"
          |    children: {create:{
          |       c: "c1"
          |       friendsOpt:{create:{f: "f1"}}
          |    }}
          |  }){
          |    p
          |    children{
          |       id
          |       c
          |       friendsOpt{
          |         f
          |       }
          |
          |    }
          |  }
          |}""",
        project
      )

    val idOfC1 = create.pathAsString("data.createParent.children.[0].id")

    val update = server
      .query(
        s"""mutation {
          |  updateParent(
          |  where:{p:"p1"}
          |  data: {
          |    children: {update:{
          |       where:{id: "$idOfC1"}
          |       data:{
          |           friendsOpt:{delete:{f: "f1"}}
          |       }
          |    }}
          |  }){
          |    p
          |    children{
          |       c
          |       friendsOpt{
          |         f
          |       }
          |
          |    }
          |  }
          |}""",
        project
      )

    update.toString should include("""{"data":{"updateParent":{"p":"p1","children":[{"c":"c1","friendsOpt":[]}]}}}""")

    server.query("query{friends{f}}", project).toString should be("""{"data":{"friends":[]}}""")
  }

}
