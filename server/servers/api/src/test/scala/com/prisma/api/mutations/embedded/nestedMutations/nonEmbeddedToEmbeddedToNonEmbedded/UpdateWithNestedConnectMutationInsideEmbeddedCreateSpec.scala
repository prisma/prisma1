package com.prisma.api.mutations.embedded.nestedMutations.nonEmbeddedToEmbeddedToNonEmbedded

import com.prisma.api.ApiSpecBase
import com.prisma.api.mutations.nonEmbedded.nestedMutations.{SchemaBase, SchemaBaseV11}
import com.prisma.shared.models.ConnectorCapability.EmbeddedTypesCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class UpdateWithNestedConnectMutationInsideEmbeddedCreateSpec extends FlatSpec with Matchers with ApiSpecBase with SchemaBaseV11 {
  override def runOnlyForCapabilities = Set(EmbeddedTypesCapability)

  "a FriendReq relation" should "be possible" in {

    val project = SchemaDsl.fromString() { embedddedToJoinFriendReq }

    database.setup(project)

    val create = server
      .query(
        """mutation {
          |  createParent(data: {
          |    p: "p1"
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

    create.toString should be("""{"data":{"createParent":{"p":"p1","children":[]}}}""")

    val create2 = server
      .query(
        """mutation {
          |  createFriend(data: {
          |    f: "f1"
          |  }){
          |    f
          |  }
          |}""",
        project
      )

    create2.toString should be("""{"data":{"createFriend":{"f":"f1"}}}""")

    val update = server
      .query(
        """mutation {
          |  updateParent(
          |  where:{p:"p1"}
          |  data: {
          |    children: {create:{
          |       c: "c1"
          |       friendReq:{connect:{f: "f1"}}
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

    update.toString should be("""{"data":{"updateParent":{"p":"p1","children":[{"c":"c1","friendReq":{"f":"f1"}}]}}}""")
  }

  //Fixme continue here
  "a FriendOpt relation" should "be possible" in {

    val project = SchemaDsl.fromString() { embedddedToJoinFriendOpt }

    database.setup(project)

    val create = server
      .query(
        """mutation {
          |  createParent(data: {
          |    p: "p1"
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

    create.toString should be("""{"data":{"createParent":{"p":"p1","children":[]}}}""")

    val create2 = server
      .query(
        """mutation {
          |  createFriend(data: {
          |    f: "f1"
          |  }){
          |    f
          |  }
          |}""",
        project
      )

    val update = server
      .query(
        """mutation {
          |  updateParent(
          |  where:{p:"p1"}
          |  data: {
          |    children: {create:{
          |       c: "c1"
          |       friendOpt:{connect:{f: "f1"}}
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

    update.toString should be("""{"data":{"updateParent":{"p":"p1","children":[{"c":"c1","friendOpt":{"f":"f1"}}]}}}""")
  }

  "a FriendsOpt relation" should "be possible" in {

    val project = SchemaDsl.fromString() { embedddedToJoinFriendsOpt }

    database.setup(project)

    val create = server
      .query(
        """mutation {
          |  createParent(data: {
          |    p: "p1"
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

    create.toString should be("""{"data":{"createParent":{"p":"p1","children":[]}}}""")

    val create2 = server
      .query(
        """mutation {
          |  createFriend(data: {
          |    f: "f1"
          |  }){
          |    f
          |  }
          |}""",
        project
      )

    val create3 = server
      .query(
        """mutation {
          |  createFriend(data: {
          |    f: "f2"
          |  }){
          |    f
          |  }
          |}""",
        project
      )

    val update = server
      .query(
        """mutation {
          |  updateParent(
          |  where:{p:"p1"}
          |  data: {
          |    children: {create:{
          |       c: "c1"
          |       friendsOpt:{connect:[{f: "f1"}, {f: "f2"}]}
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

    update.toString should be("""{"data":{"updateParent":{"p":"p1","children":[{"c":"c1","friendsOpt":[{"f":"f1"},{"f":"f2"}]}]}}}""")
  }

}
