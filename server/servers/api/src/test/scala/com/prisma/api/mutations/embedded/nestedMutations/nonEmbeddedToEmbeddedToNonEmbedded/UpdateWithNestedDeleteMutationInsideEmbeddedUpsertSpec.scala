package com.prisma.api.mutations.embedded.nestedMutations.nonEmbeddedToEmbeddedToNonEmbedded

import com.prisma.api.ApiSpecBase
import com.prisma.api.mutations.nonEmbedded.nestedMutations.{SchemaBase, SchemaBaseV11}
import com.prisma.shared.models.ConnectorCapability.EmbeddedTypesCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class UpdateWithNestedDeleteMutationInsideEmbeddedUpsertSpec extends FlatSpec with Matchers with ApiSpecBase with SchemaBaseV11 {
  override def runOnlyForCapabilities = Set(EmbeddedTypesCapability)

  "a FriendReq relation" should "be possible" in {

    val project = SchemaDsl.fromString() { embedddedToJoinFriendReq }

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

    create.toString should be("""{"data":{"createParent":{"p":"p1","children":[{"c":"c1","friendReq":{"f":"f1"}}]}}}""")

    val update = server
      .queryThatMustFail(
        """mutation {
          |  updateParent(
          |  where:{p:"p1"}
          |  data: {
          |    children: {upsert:{
          |       where:{c: "c1"}
          |       create:{ c: "cNew", friendReq:{connect:{f: "SHOULD NOT MATTER"}}}
          |       update:{ friendReq:{delete:true}}
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
        errorContains =
          """Reason: 'children.upsert[0].update.friendReq.delete' Field 'delete' is not defined in the input type 'FriendUpdateOneRequiredInput'."""
      )

    server.query("query{friends{f}}", project).toString should be("""{"data":{"friends":[{"f":"f1"}]}}""")
  }

  "a FriendOpt relation" should "be possible" in {

    val project = SchemaDsl.fromString() { embedddedToJoinFriendOpt }

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

    create.toString should be("""{"data":{"createParent":{"p":"p1","children":[{"c":"c1","friendOpt":{"f":"f1"}}]}}}""")

    val update = server
      .query(
        """mutation {
          |  updateParent(
          |  where:{p:"p1"}
          |  data: {
          |    children: {upsert:{
          |       where:{c: "c1"}
          |       create:{ c: "cNew", friendOpt:{connect:{f: "SHOULD NOT MATTER"}}}
          |       update:{ friendOpt:{delete:true}}
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

    val project = SchemaDsl.fromString() { embedddedToJoinFriendsOpt }

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

    create.toString should be("""{"data":{"createParent":{"p":"p1","children":[{"c":"c1","friendsOpt":[{"f":"f1"}]}]}}}""")

    val update = server
      .query(
        """mutation {
          |  updateParent(
          |  where:{p:"p1"}
          |  data: {
          |    children: {upsert:{
          |       where:{c: "c1"}
          |       create:{ c: "cNew", friendsOpt:{connect:{f: "SHOULD NOT MATTER"}}}
          |       update:{ friendsOpt:{delete:{f:"f1"}}}
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
