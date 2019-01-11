package com.prisma.api.mutations.embedded.nestedMutations.nonEmbeddedToEmbeddedToNonEmbedded

import com.prisma.api.ApiSpecBase
import com.prisma.api.mutations.nonEmbedded.nestedMutations.SchemaBase
import com.prisma.shared.models.ConnectorCapability.EmbeddedTypesCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class UpdateWithNestedConnectMutationInsideEmbeddedUpsertSpec extends FlatSpec with Matchers with ApiSpecBase with SchemaBase {
  override def runOnlyForCapabilities = Set(EmbeddedTypesCapability)

  "a FriendReq relation" should "be possible for the Update Branch" in {

    val project = SchemaDsl.fromString() { embedddedToJoinFriendReq }

    database.setup(project)

    val create = server
      .query(
        """mutation {
          |  createParent(data: {
          |    p: "p1"
          |    children:{create:{
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

    val create2 = server.query("""mutation { createFriend(data: {f: "f2"}){f}}""", project)

    create2.toString should be("""{"data":{"createFriend":{"f":"f2"}}}""")

    val create3 = server.query("""mutation { createFriend(data: {f: "f3"}){f}}""", project)

    create3.toString should be("""{"data":{"createFriend":{"f":"f3"}}}""")

    val update = server
      .query(
        """mutation {
          |  updateParent(
          |  where:{p:"p1"}
          |  data: {
          |    children: {upsert:{
          |       where:{c: "c1"}
          |       create:{ c: "cNew", friendReq:{connect:{f: "f2"}}}
          |       update:{ friendReq:{connect:{f: "f3"}}}
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

    update.toString should be("""{"data":{"updateParent":{"p":"p1","children":[{"c":"c1","friendReq":{"f":"f3"}}]}}}""")
  }

  "a FriendReq relation" should "be possible for the Create Branch" in {

    val project = SchemaDsl.fromString() { embedddedToJoinFriendReq }

    database.setup(project)

    val create = server
      .query(
        """mutation {
          |  createParent(data: {
          |    p: "p1"
          |    children:{create:{
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

    val create2 = server.query("""mutation { createFriend(data: {f: "f2"}){f}}""", project)

    create2.toString should be("""{"data":{"createFriend":{"f":"f2"}}}""")

    val create3 = server.query("""mutation { createFriend(data: {f: "f3"}){f}}""", project)

    create3.toString should be("""{"data":{"createFriend":{"f":"f3"}}}""")

    val update = server
      .query(
        """mutation {
          |  updateParent(
          |  where:{p:"p1"}
          |  data: {
          |    children: {upsert:{
          |       where:{c: "c2"}
          |       create:{ c: "cNew", friendReq:{connect:{f: "f2"}}}
          |       update:{ friendReq:{connect:{f: "f3"}}}
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

    update.toString should be("""{"data":{"updateParent":{"p":"p1","children":[{"c":"c1","friendReq":{"f":"f1"}},{"c":"cNew","friendReq":{"f":"f2"}}]}}}""")
  }

  "a FriendOpt relation" should "be possible for the UPDATE branch" in {

    val project = SchemaDsl.fromString() { embedddedToJoinFriendOpt }

    database.setup(project)

    val create = server
      .query(
        """mutation {
          |  createParent(data: {
          |    p: "p1"
          |    children:{create:{
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

    val create2 = server.query("""mutation { createFriend(data: {f: "f2"}){f}}""", project)

    create2.toString should be("""{"data":{"createFriend":{"f":"f2"}}}""")

    val create3 = server.query("""mutation { createFriend(data: {f: "f3"}){f}}""", project)

    create3.toString should be("""{"data":{"createFriend":{"f":"f3"}}}""")

    val update = server
      .query(
        """mutation {
          |  updateParent(
          |  where:{p:"p1"}
          |  data: {
          |    children: {upsert:{
          |       where:{c: "c1"}
          |       create:{ c: "cNew", friendOpt:{connect:{f: "f2"}}}
          |       update:{ friendOpt:{connect:{f: "f3"}}}
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

    update.toString should be("""{"data":{"updateParent":{"p":"p1","children":[{"c":"c1","friendOpt":{"f":"f3"}}]}}}""")
  }

  "a FriendOpt relation" should "be possible for the CREATE branch" in {

    val project = SchemaDsl.fromString() { embedddedToJoinFriendOpt }

    database.setup(project)

    val create = server
      .query(
        """mutation {
          |  createParent(data: {
          |    p: "p1"
          |    children:{create:{
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

    val create2 = server.query("""mutation { createFriend(data: {f: "f2"}){f}}""", project)

    create2.toString should be("""{"data":{"createFriend":{"f":"f2"}}}""")

    val create3 = server.query("""mutation { createFriend(data: {f: "f3"}){f}}""", project)

    create3.toString should be("""{"data":{"createFriend":{"f":"f3"}}}""")

    val update = server
      .query(
        """mutation {
          |  updateParent(
          |  where:{p:"p1"}
          |  data: {
          |    children: {upsert:{
          |       where:{c: "c2"}
          |       create:{ c: "cNew", friendOpt:{connect:{f: "f2"}}}
          |       update:{ friendOpt:{connect:{f: "f3"}}}
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

    update.toString should be("""{"data":{"updateParent":{"p":"p1","children":[{"c":"c1","friendOpt":{"f":"f1"}},{"c":"cNew","friendOpt":{"f":"f2"}}]}}}""")
  }

  "a FriendsOpt relation" should "be possible for the Update branch" in {

    val project = SchemaDsl.fromString() { embedddedToJoinFriendsOpt }

    database.setup(project)

    val create = server
      .query(
        """mutation {
          |  createParent(data: {
          |    p: "p1"
          |    children:{create:{
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

    val create2 = server.query("""mutation { createFriend(data: {f: "f2"}){f}}""", project)

    create2.toString should be("""{"data":{"createFriend":{"f":"f2"}}}""")

    val create3 = server.query("""mutation { createFriend(data: {f: "f3"}){f}}""", project)

    create3.toString should be("""{"data":{"createFriend":{"f":"f3"}}}""")

    val update = server
      .query(
        """mutation {
          |  updateParent(
          |  where:{p:"p1"}
          |  data: {
          |    children: {upsert:{
          |       where:{c: "c1"}
          |       create:{ c: "cNew", friendsOpt:{connect:{f: "f2"}}}
          |       update:{ friendsOpt:{connect:{f: "f3"}}}
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

    update.toString should be("""{"data":{"updateParent":{"p":"p1","children":[{"c":"c1","friendsOpt":[{"f":"f1"},{"f":"f3"}]}]}}}""")
  }

  "a FriendsOpt relation" should "be possible for the CREATE branch" in {

    val project = SchemaDsl.fromString() { embedddedToJoinFriendsOpt }

    database.setup(project)

    val create = server
      .query(
        """mutation {
          |  createParent(data: {
          |    p: "p1"
          |    children:{create:{
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

    val create2 = server.query("""mutation { createFriend(data: {f: "f2"}){f}}""", project)

    create2.toString should be("""{"data":{"createFriend":{"f":"f2"}}}""")

    val create3 = server.query("""mutation { createFriend(data: {f: "f3"}){f}}""", project)

    create3.toString should be("""{"data":{"createFriend":{"f":"f3"}}}""")

    val update = server
      .query(
        """mutation {
          |  updateParent(
          |  where:{p:"p1"}
          |  data: {
          |    children: {upsert:{
          |       where:{c: "c2"}
          |       create:{ c: "cNew", friendsOpt:{connect:{f: "f2"}}}
          |       update:{ friendsOpt:{connect:{f: "f3"}}}
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

    update.toString should be(
      """{"data":{"updateParent":{"p":"p1","children":[{"c":"c1","friendsOpt":[{"f":"f1"}]},{"c":"cNew","friendsOpt":[{"f":"f2"}]}]}}}""")
  }
}
