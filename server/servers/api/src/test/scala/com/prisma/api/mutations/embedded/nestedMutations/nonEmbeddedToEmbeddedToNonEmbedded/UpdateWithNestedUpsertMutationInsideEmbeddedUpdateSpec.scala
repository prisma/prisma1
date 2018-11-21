package com.prisma.api.mutations.embedded.nestedMutations.nonEmbeddedToEmbeddedToNonEmbedded

import com.prisma.api.ApiSpecBase
import com.prisma.api.mutations.nonEmbedded.nestedMutations.SchemaBase
import com.prisma.shared.models.ConnectorCapability.EmbeddedTypesCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class UpdateWithNestedUpsertMutationInsideEmbeddedUpdateSpec extends FlatSpec with Matchers with ApiSpecBase with SchemaBase {
  override def runOnlyForCapabilities = Set(EmbeddedTypesCapability)

  "a FriendReq relation" should "be possible for the Update branch" in {

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
          |           friendReq:{upsert:{
          |             create:{f:"SHOULD NOT MATTER"},
          |             update:{f:"fUpdated"}
          |           }}
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

    update.toString should be("""{"data":{"updateParent":{"p":"p1","children":[{"c":"c1","friendReq":{"f":"fUpdated"}}]}}}""")

    server.query("query{friends{f}}", project).toString should be("""{"data":{"friends":[{"f":"fUpdated"}]}}""")
  }

  "a FriendOpt relation" should "be possible for the Update branch" in {

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
          |           friendOpt:{upsert:{
          |             create:{f:"SHOULD NOT MATTER"},
          |             update:{f:"fUpdated"}
          |           }}
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

    update.toString should be("""{"data":{"updateParent":{"p":"p1","children":[{"c":"c1","friendOpt":{"f":"fUpdated"}}]}}}""")

    server.query("query{friends{f}}", project).toString should be("""{"data":{"friends":[{"f":"fUpdated"}]}}""")
  }

  "a FriendOpt relation" should "be possible for the Create branch" in {

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
          |           friendOpt:{upsert:{
          |             create:{f:"fCreated"},
          |             update:{f:"SHOULD NOT MATTER"}
          |           }}
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

    update.toString should be("""{"data":{"updateParent":{"p":"p1","children":[{"c":"c1","friendOpt":{"f":"fCreated"}}]}}}""")

    server.query("query{friends{f}}", project).toString should be("""{"data":{"friends":[{"f":"fCreated"}]}}""")
  }

  "a FriendsOpt relation" should "be possible for the Create branch" in {

    val project = SchemaDsl.fromString() { embedddedToJoinFriendsOpt }

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
          |       friendsOpt: {create:{f:"f1"}}
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
          |           friendsOpt:{upsert:{
          |             where:{f:"DOES NOT EXIST"}
          |             create:{f:"fCreated"},
          |             update:{f:"SHOULD NOT MATTER"}
          |           }}
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

    update.toString should be("""{"data":{"updateParent":{"p":"p1","children":[{"c":"c1","friendsOpt":[{"f":"f1"},{"f":"fCreated"}]}]}}}""")

    server.query("query{friends{f}}", project).toString should be("""{"data":{"friends":[{"f":"f1"},{"f":"fCreated"}]}}""")
  }
}
