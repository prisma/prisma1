package com.prisma.api.mutations.embedded.nestedMutations.nonEmbeddedToEmbeddedToNonEmbedded

import com.prisma.IgnoreMongo
import com.prisma.api.ApiSpecBase
import com.prisma.api.mutations.nonEmbedded.nestedMutations.SchemaBase
import com.prisma.shared.models.ApiConnectorCapability.EmbeddedTypesCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class UpdateWithEmbeddedNestedCreateMutationInsideUpdateSpec extends FlatSpec with Matchers with ApiSpecBase with SchemaBase {
  override def runOnlyForCapabilities = Set(EmbeddedTypesCapability)

  "a FriendReq relation" should "be possible" in {

    val project = SchemaDsl.fromString() { embedddedToJoinFriendReq }

    database.setup(project)

    val create = server
      .query(
        """mutation {
          |  createParent(data: {
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
          |       friendReq:{create:{f: "f2"}}
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

    update.toString should be("""{"data":{"updateParent":{"p":"p1","children":[{"c":"c1","friendReq":{"f":"f2"}}]}}}""")
  }

  "a FriendReq relation" should "be possible if updating a unique along the path" taggedAs (IgnoreMongo) in {
    val project = SchemaDsl.fromString() { embedddedToJoinFriendReq }

    database.setup(project)

    val create = server
      .query(
        """mutation {
          |  createParent(data: {
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
          |        c: "c2"
          |       friendReq:{create:{f: "f2"}}
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

    update.toString should be("""{"data":{"updateParent":{"p":"p1","children":[{"c":"c2","friendReq":{"f":"f2"}}]}}}""")
  }

  "a FriendOpt relation" should "be possible" in {

    val project = SchemaDsl.fromString() { embedddedToJoinFriendOpt }

    database.setup(project)

    val create = server
      .query(
        """mutation {
          |  createParent(data: {
          |    p: "p1"
          |    children: {create:{
          |       c: "c1"
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

    create.toString should be("""{"data":{"createParent":{"p":"p1","children":[{"c":"c1","friendOpt":null}]}}}""")

    val update = server
      .query(
        """mutation {
          |  updateParent(
          |  where:{p:"p1"}
          |  data: {
          |    children: {update:{
          |       where:{c: "c1"}
          |       data:{
          |       friendOpt:{create:{f: "f1"}}
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

    update.toString should be("""{"data":{"updateParent":{"p":"p1","children":[{"c":"c1","friendOpt":{"f":"f1"}}]}}}""")
  }

}
