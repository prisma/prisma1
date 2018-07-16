package com.prisma.api.filters

import com.prisma.api.ApiSpecBase
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest._

class ExtendedRelationFilterSpec extends FlatSpec with Matchers with ApiSpecBase {

  override def runSuiteOnlyForActiveConnectors = true

  val project = SchemaDsl.fromString() { """type Artist {
                                         |  id: ID! @unique
                                         |  ArtistId: Int! @unique
                                         |  Name: String!
                                         |  Albums: [Album!]!
                                         |}
                                         |
                                         |type Album {
                                         |  id: ID! @unique
                                         |  AlbumId: Int! @unique
                                         |  Title: String!
                                         |  Artist: Artist!
                                         |  Tracks: [Track!]!
                                         |}
                                         |
                                         |type Genre {
                                         |  id: ID! @unique
                                         |  GenreId: Int! @unique
                                         |  Name: String!
                                         |  Tracks: [Track!]!
                                         |}
                                         |
                                         |type MediaType {
                                         |  id: ID! @unique
                                         |  MediaTypeId: Int! @unique
                                         |  Name: String!
                                         |  Tracks: [Track!]!
                                         |}
                                         |
                                         |type Track {
                                         |  id: ID! @unique
                                         |  TrackId: Int! @unique
                                         |  Name: String!
                                         |  Album: Album!
                                         |  MediaType: MediaType!
                                         |  Genre: Genre!
                                         |  Composer: String
                                         |  Milliseconds: Int!
                                         |  Bytes: Int!
                                         |  UnitPrice: Float!
                                         |}
                                         |""" }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(project)
  }

  override def beforeEach() = {
    super.beforeEach()
    database.truncateProjectTables(project)

    // add data
    server.query("""mutation {createGenre(data: {Name: "Genre1", GenreId: 1}){Name}}""", project = project)
    server.query("""mutation {createGenre(data: {Name: "Genre2", GenreId: 2}){Name}}""", project = project)
    server.query("""mutation {createGenre(data: {Name: "Genre3", GenreId: 3}){Name}}""", project = project)
    server.query("""mutation {createGenre(data: {Name: "GenreThatIsNotUsed", GenreId: 4}){Name}}""", project = project)

    server.query("""mutation {createMediaType(data: {Name: "MediaType1", MediaTypeId: 1}){Name}}""", project = project)
    server.query("""mutation {createMediaType(data: {Name: "MediaType2", MediaTypeId: 2}){Name}}""", project = project)
    server.query("""mutation {createMediaType(data: {Name: "MediaType3", MediaTypeId: 3}){Name}}""", project = project)
    server.query("""mutation {createMediaType(data: {Name: "MediaTypeThatIsNotUsed", MediaTypeId: 4}){Name}}""", project = project)

    server.query(
      """mutation completeArtist {createArtist(data:{
        |                         Name: "CompleteArtist"
        |                         ArtistId: 1,
        |                         Albums: {create: [
        |                                   {Title: "Album1",
        |                                    AlbumId: 1,
        |                                    Tracks:{create: [
        |                                             {
        |                                               Name:"Track1",
        |                                               TrackId: 1,
        |                                               Composer: "Composer1",
        |                                               Milliseconds: 10000,
        |                                               Bytes: 512,
        |                                               UnitPrice: 1.51,
        |                                               Genre: {connect: {GenreId: 1}},
        |                                               MediaType: {connect: {MediaTypeId: 1}}
        |                                             }
        |                                    ]}
        |                          }]}
        |}){Name}}""",
      project = project
    )

    server.query(
      """mutation artistWithoutAlbums {createArtist(data:{
        |                         Name: "ArtistWithoutAlbums"
        |                         ArtistId: 2
        |}){Name}}""",
      project = project
    )

    server.query(
      """mutation artistWithAlbumButWithoutTracks {createArtist(data:{
        |                         Name: "ArtistWithOneAlbumWithoutTracks"
        |                         ArtistId: 3,
        |                         Albums: {create: [
        |                                   {Title: "AlbumWithoutTracks",
        |                                    AlbumId: 2
        |                          }]}
        |}){Name}}""",
      project = project
    )

    server.query(
      """mutation completeArtist2 {createArtist(data:{
        |                         Name: "CompleteArtist2"
        |                         ArtistId: 4,
        |                         Albums: {create: [
        |                                   {Title: "Album3",
        |                                    AlbumId: 3,
        |                                    Tracks:{create: [
        |                                             {
        |                                               Name:"Track2",
        |                                               TrackId: 2,
        |                                               Composer: "Composer1",
        |                                               Milliseconds: 11000,
        |                                               Bytes: 1024,
        |                                               UnitPrice: 2.51,
        |                                               Genre: {connect: {GenreId: 2}},
        |                                               MediaType: {connect: {MediaTypeId: 2}}
        |                                             },
        |                                             {
        |                                               Name:"Track3",
        |                                               TrackId: 3,
        |                                               Composer: "Composer2",
        |                                               Milliseconds: 9000,
        |                                               Bytes: 24,
        |                                               UnitPrice: 5.51,
        |                                               Genre: {connect: {GenreId: 3}},
        |                                               MediaType: {connect: {MediaTypeId: 3}}
        |                                             }
        |                                    ]}
        |                          }]}
        |}){Name}}""",
      project = project
    )

  }

  "simple scalar filter" should "work" in {
    server.query("""query {artists(where:{ArtistId: 1}){Name}}""", project = project).toString should be("""{"data":{"artists":[{"Name":"CompleteArtist"}]}}""")
  }

  "1 level 1-relation filter" should "work" in {
    server.query(query = """{albums(where:{Artist:{Name: "CompleteArtist"}}){AlbumId}}""", project = project).toString should be(
      """{"data":{"albums":[{"AlbumId":1}]}}""")
  }
//
//  "1 level m-relation filter" should "work for _every, _some and _none" in {
//
//    server.query(query = """{blogs(where:{posts_some:{popularity_gte: 5}}){name}}""", project = project).toString should be(
//      """{"data":{"blogs":[{"name":"blog 1"},{"name":"blog 2"}]}}""")
//
//    server.query(query = """{blogs(where:{posts_some:{popularity_gte: 50}}){name}}""", project = project).toString should be(
//      """{"data":{"blogs":[{"name":"blog 2"}]}}""")
//
//    server.query(query = """{blogs(where:{posts_every:{popularity_gte: 2}}){name}}""", project = project).toString should be(
//      """{"data":{"blogs":[{"name":"blog 1"},{"name":"blog 2"}]}}""")
//
//    server.query(query = """{blogs(where:{posts_every:{popularity_gte: 3}}){name}}""", project = project).toString should be(
//      """{"data":{"blogs":[{"name":"blog 2"}]}}""")
//
//    server.query(query = """{blogs(where:{posts_none:{popularity_gte: 50}}){name}}""", project = project).toString should be(
//      """{"data":{"blogs":[{"name":"blog 1"}]}}""")
//
//    server.query(query = """{blogs(where:{posts_none:{popularity_gte: 5}}){name}}""", project = project).toString should be("""{"data":{"blogs":[]}}""")
//  }
//
//  "2 level m-relation filter" should "work for _every, _some and _none" in {
//
//    // some|some
//    server.query(query = """{blogs(where:{posts_some:{comments_some: {likes: 0}}}){name}}""", project = project).toString should be(
//      """{"data":{"blogs":[{"name":"blog 1"}]}}""")
//
//    server.query(query = """{blogs(where:{posts_some:{comments_some: {likes: 1}}}){name}}""", project = project).toString should be("""{"data":{"blogs":[]}}""")
//
//    // some|every
//    server.query(query = """{blogs(where:{posts_some:{comments_every: {likes_gte: 0}}}){name}}""", project = project).toString should be(
//      """{"data":{"blogs":[{"name":"blog 1"},{"name":"blog 2"}]}}""")
//
//    server.query(query = """{blogs(where:{posts_some:{comments_every: {likes: 0}}}){name}}""", project = project).toString should be(
//      """{"data":{"blogs":[]}}""")
//
//    // some|none
//    server.query(query = """{blogs(where:{posts_some:{comments_none: {likes: 0}}}){name}}""", project = project).toString should be(
//      """{"data":{"blogs":[{"name":"blog 1"},{"name":"blog 2"}]}}""")
//
//    server.query(query = """{blogs(where:{posts_some:{comments_none: {likes_gte: 0}}}){name}}""", project = project).toString should be(
//      """{"data":{"blogs":[]}}""")
//
//    // every|some
//    server.query(query = """{blogs(where:{posts_every:{comments_some: {likes: 10}}}){name}}""", project = project).toString should be(
//      """{"data":{"blogs":[{"name":"blog 1"}]}}""")
//
//    server.query(query = """{blogs(where:{posts_every:{comments_some: {likes: 0}}}){name}}""", project = project).toString should be(
//      """{"data":{"blogs":[]}}""")
//
//    // every|every
//    server.query(query = """{blogs(where:{posts_every:{comments_every: {likes_gte: 0}}}){name}}""", project = project).toString should be(
//      """{"data":{"blogs":[{"name":"blog 1"},{"name":"blog 2"}]}}""")
//
//    server.query(query = """{blogs(where:{posts_every:{comments_every: {likes: 0}}}){name}}""", project = project).toString should be(
//      """{"data":{"blogs":[]}}""")
//
//    // every|none
//    server.query(query = """{blogs(where:{posts_every:{comments_none: {likes_gte: 100}}}){name}}""", project = project).toString should be(
//      """{"data":{"blogs":[{"name":"blog 1"}]}}""")
//
//    server.query(query = """{blogs(where:{posts_every:{comments_none: {likes: 0}}}){name}}""", project = project).toString should be(
//      """{"data":{"blogs":[{"name":"blog 2"}]}}""")
//
//    // none|some
//    server.query(query = """{blogs(where:{posts_none:{comments_some: {likes_gte: 100}}}){name}}""", project = project).toString should be(
//      """{"data":{"blogs":[{"name":"blog 1"}]}}""")
//
//    server.query(query = """{blogs(where:{posts_none:{comments_some: {likes: 0}}}){name}}""", project = project).toString should be(
//      """{"data":{"blogs":[{"name":"blog 2"}]}}""")
//
//    // none|every
//    server.query(query = """{blogs(where:{posts_none:{comments_every: {likes_gte: 11}}}){name}}""", project = project).toString should be(
//      """{"data":{"blogs":[{"name":"blog 1"}]}}""")
//
//    server.query(query = """{blogs(where:{posts_none:{comments_every: {likes_gte: 0}}}){name}}""", project = project).toString should be(
//      """{"data":{"blogs":[]}}""")
//
//    // none|none
//    server.query(query = """{blogs(where:{posts_none:{comments_none: {likes_gte: 0}}}){name}}""", project = project).toString should be(
//      """{"data":{"blogs":[{"name":"blog 1"},{"name":"blog 2"}]}}""")
//
//    server.query(query = """{blogs(where:{posts_none:{comments_none: {likes_gte: 11}}}){name}}""", project = project).toString should be(
//      """{"data":{"blogs":[{"name":"blog 2"}]}}""")
//  }
//
//  "crazy filters" should "work" in {
//
//    server
//      .query(
//        query = """{posts(where: {
//                |  blog: {
//                |    posts_some: {
//                |      popularity_gte: 5
//                |    }
//                |    name_contains: "Blog 1"
//                |  }
//                |  comments_none: {
//                |    likes_gte: 5
//                |  }
//                |  comments_some: {
//                |    likes_lte: 2
//                |  }
//                |}) {
//                |  title
//                |}}""".stripMargin,
//        project = project
//      )
//      .toString should be("""{"data":{"posts":[]}}""")
//
//  }
}
