package com.prisma.api.mutations.embedded

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability.EmbeddedTypesCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class MongoPrototypingSpec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities = Set(EmbeddedTypesCapability)

  "Simple unique index" should "work" in {

    val project = SchemaDsl.fromString() {
      """
        |type Top {
        |   id: ID! @unique
        |   unique: Int! @unique
        |   name: String!
        |}"""
    }

    database.setup(project)

    server.query(
      s"""mutation {
         |   createTop(data: {
         |   unique: 11111,
         |   name: "Top"
         |}){
         |  unique,
         |  name
         |}}""",
      project
    )

    server.queryThatMustFail(
      s"""mutation {
         |   createTop(data: {
         |   unique: 11111,
         |   name: "Top"
         |}){
         |  unique,
         |  name
         |}}""",
      project,
      3010,
      errorContains = """A unique constraint would be violated on Top. Details: Field name = unique"""
    )
  }

  //Fixme https://jira.mongodb.org/browse/SERVER-1068
  "Unique indexes on embedded types" should "work" ignore {
    val project = SchemaDsl.fromString() {
      """
        |type Parent{
        |    id: ID! @unique
        |    name: String @unique
        |    children: [Child]
        |}
        |
        |type Child @embedded{
        |    name: String @unique
        |}
        |"""
    }

    database.setup(project)

    val create1 = server.query(
      s"""mutation {
         |   createParent(data: {
         |   name: "Dad",
         |   children: {create: [{ name: "Daughter"}]}
         |}){
         |  name,
         |  children{ name}
         |}}""",
      project
    )

    create1.toString should be("""{"data":{"createParent":{"name":"Dad","children":[{"name":"Daughter"}]}}}""")

    val create2 = server.query(
      s"""mutation {
         |   createParent(data: {
         |   name: "Dad2",
         |   children: {create: [{ name: "Daughter"}, { name: "Daughter"}]}
         |}){
         |  name,
         |  children{ name}
         |}}""",
      project
    )

    create2.toString should be("""{"data":{"createParent":{"name":"Dad2","children":[{"name":"Daughter"},{"name":"Daughter"}]}}}""")

    val create3 = server.query(
      s"""mutation {
         |   createParent(data: {
         |   name: "Dad",
         |   children: {create: [{ name: "Daughter"}]}
         |}){
         |  name,
         |  children{ name}
         |}}""",
      project
    )

    create3.toString should be("""{"data":{"createParent":{"name":"Dad2","children":[{"name":"Daughter"},{"name":"Daughter"}]}}}""")

    val update1 = server.query(
      s"""mutation {
         |   updateParent(
         |   where: {name: "Dad"}
         |   data: {
         |      children: {create: [{ name: "Daughter2"}]}
         |}){
         |  name,
         |  children{ name}
         |}}""",
      project
    )

    update1.toString should be("""{"data":{"updateParent":{"name":"Dad","children":[{"name":"Daughter"},{"name":"Daughter2"}]}}}""")

    val update2 = server.query(
      s"""mutation {
         |   updateParent(
         |   where: {name: "Dad"}
         |   data: {
         |      children: {create: [{ name: "Daughter"}]}
         |}){
         |  name,
         |  children{ name}
         |}}""",
      project
    )

    update2.toString should be("""{"data":{"updateParent":{"name":"Dad","children":[{"name":"Daughter"},{"name":"Daughter2"}]}}}""")
  }

  "Field names starting with a capital letter" should "not error" in {

    val project = SchemaDsl.fromString() {
      """type Artist {
            id: ID! @unique
            ArtistId: Int! @unique
            Name: String!
            Albums: [Album!]! @mongoRelation(field: "Albums")
          }
          
          type Album {
            id: ID! @unique
            AlbumId: Int! @unique
            Title: String!
            Tracks: [Track!]!
          }
          
          type Track @embedded{
            TrackId: Int!
            Name: String!
            MediaType: MediaType! @mongoRelation(field: "MediaType")
            Genre: Genre! @mongoRelation(field: "Genre")
            Composer: String
            Milliseconds: Int!
            Bytes: Int!
            UnitPrice: Float!
          }
          
          type Genre {
            id: ID! @unique
            GenreId: Int! @unique
            Name: String!
          }
          
          type MediaType {
            id: ID! @unique
            MediaTypeId: Int! @unique
            Name: String!
          }"""
    }

    database.setup(project)

    server.query("""mutation createmediaType{createMediaType(data:{MediaTypeId:10 Name: "10"}){id}}""", project)
    server.query("""mutation creategenre{createGenre(data:{GenreId:83 Name: "83"}){id}}""", project)

    val query = """mutation createContent {
                  |        createArtist(
                  |          data: {
                  |            ArtistId: 1
                  |            Name: "artist1"
                  |            Albums: {
                  |              create: [
                  |                {
                  |            AlbumId: 1
                  |            Title: "artist1album1"
                  |            Tracks: {
                  |              create: [
                  |                {
                  |                  TrackId: 2
                  |                  Name: "track2"
                  |                  Composer: "track2composer"
                  |                  Milliseconds: 473598
                  |                  Bytes: 4476226
                  |                  UnitPrice: 4.83
                  |                  Genre: { connect: { GenreId: 83 } }
                  |                  MediaType: { connect: { MediaTypeId: 10 } }
                  |                },
                  |                {
                  |                  TrackId: 3
                  |                  Name: "track3"
                  |                  Composer: "track3composer"
                  |                  Milliseconds: 607845
                  |                  Bytes: 2990084
                  |                  UnitPrice: 2.75
                  |                  Genre: { connect: { GenreId: 83 } }
                  |                  MediaType: { connect: { MediaTypeId: 10 } }
                  |                }
                  |             ]
                  |            }
                  |                },
                  |            {
                  |            AlbumId: 2
                  |            Title: "artist1album2"
                  |            Tracks: {
                  |              create: [
                  |                {
                  |                  TrackId: 4
                  |                  Name: "track4"
                  |                  Composer: "track4composer"
                  |                  Milliseconds: 4734598
                  |                  Bytes: 44762264
                  |                  UnitPrice: 4.831
                  |                  Genre: { connect: { GenreId: 83 } }
                  |                  MediaType: { connect: { MediaTypeId: 10 } }
                  |                },
                  |                {
                  |                  TrackId: 5
                  |                  Name: "track5"
                  |                  Composer: "track5composer"
                  |                  Milliseconds: 6075845
                  |                  Bytes: 29900845
                  |                  UnitPrice: 2.755
                  |                  Genre: { connect: { GenreId: 83 } }
                  |                  MediaType: { connect: { MediaTypeId: 10 } }
                  |                }
                  |             ]
                  |            }}
                  |           ]
                  |              }
                  |          }){
                  |          ArtistId
                  |          Name
                  |          Albums {
                  |            AlbumId
                  |            Title
                  |            Tracks {
                  |               TrackId
                  |               Name
                  |               Genre { GenreId }
                  |               MediaType{ MediaTypeId }
                  |            }
                  |          }
                  |  }}"""

    val res = server.query(query, project)

    res.toString should be(
      """{"data":{"createArtist":{"ArtistId":1,"Name":"artist1","Albums":[{"AlbumId":1,"Title":"artist1album1","Tracks":[{"TrackId":2,"Name":"track2","Genre":{"GenreId":83},"MediaType":{"MediaTypeId":10}},{"TrackId":3,"Name":"track3","Genre":{"GenreId":83},"MediaType":{"MediaTypeId":10}}]},{"AlbumId":2,"Title":"artist1album2","Tracks":[{"TrackId":4,"Name":"track4","Genre":{"GenreId":83},"MediaType":{"MediaTypeId":10}},{"TrackId":5,"Name":"track5","Genre":{"GenreId":83},"MediaType":{"MediaTypeId":10}}]}]}}}""")
  }

  "Relations on embedded types" should "be indexed" in {

    val project = SchemaDsl.fromString() {
      """
        |type A {
        |   id: ID! @unique
        |   u: Int! @unique
        |   name: String!
        |   emb:  AEmbedded @relation(name: "AEmbeddedOnA")
        |   embs: [AEmbedded!]! @relation(name: "AEmbeddedsOnA")
        |}
        |
        |type AA {
        |   id: ID! @unique
        |   u: Int! @unique
        |   name: String!
        |   emb:  AEmbedded @relation(name: "AAEmbeddedOnA")
        |}
        |
        |type AEmbedded @embedded {
        |   u: Int! @unique
        |   name: String!
        |   bs: [B!]!
        |}
        |
        |type B {
        |   id: ID! @unique
        |   u: Int! @unique
        |   name: String!
        |}"""
    }

    database.setup(project)

  }

}
