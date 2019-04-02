package com.prisma.api.mutations.embedded.nestedMutations.nonEmbeddedToEmbeddedToNonEmbedded

import com.prisma.api.ApiSpecBase
import com.prisma.api.mutations.nonEmbedded.nestedMutations.SchemaBaseV11
import com.prisma.shared.models.ConnectorCapability.EmbeddedTypesCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class UpdateWithNestedUpdateMutationInsideEmbeddedUpdateSpec extends FlatSpec with Matchers with ApiSpecBase with SchemaBaseV11 {
  override def runOnlyForCapabilities = Set(EmbeddedTypesCapability)

  //Fixme Todo

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
      .query(
        """mutation {
          |  updateParent(
          |  where:{p:"p1"}
          |  data: {
          |    children: {update:{
          |       where:{c: "c1"}
          |       data:{
          |           friendReq:{update:{f: "f2"}}
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
        project
      )

    update.toString() should be("""{"data":{"updateParent":{"p":"p1","children":[{"c":"c1","friendReq":{"f":"f2"}}]}}}""")

    server.query("query{friends{f}}", project).toString should be("""{"data":{"friends":[{"f":"f2"}]}}""")
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
          |    children: {update:{
          |       where:{c: "c1"}
          |       data:{
          |           friendOpt:{update: {f: "f2"}}
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

    update.toString should include("""{"data":{"updateParent":{"p":"p1","children":[{"c":"c1","friendOpt":{"f":"f2"}}]}}}""")

    server.query("query{friends{f}}", project).toString should be("""{"data":{"friends":[{"f":"f2"}]}}""")

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
          |    children: {update:{
          |       where:{c: "c1"}
          |       data:{
          |           friendsOpt:{update:{ where:{f: "f1"} data:{f:"fUpdated"}}}
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

    update.toString should include("""{"data":{"updateParent":{"p":"p1","children":[{"c":"c1","friendsOpt":[{"f":"fUpdated"}]}]}}}""")

    server.query("query{friends{f}}", project).toString should be("""{"data":{"friends":[{"f":"fUpdated"}]}}""")
  }
}
