package com.prisma.api.filters.nonEmbedded

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability.JoinRelationLinksCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import com.prisma.{IgnoreMongo, IgnoreMySql, IgnorePostgres}
import org.scalatest._

class ExtendedRelationFilterSpec extends FlatSpec with Matchers with ApiSpecBase {

  override def runOnlyForCapabilities = Set(JoinRelationLinksCapability)

  val project = SchemaDsl.fromStringV11() { """type Artist {
                                         |  id: ID! @id
                                         |  ArtistId: Int! @unique
                                         |  Name: String!
                                         |  Albums: [Album]
                                         |}
                                         |
                                         |type Album {
                                         |  id: ID! @id
                                         |  AlbumId: Int! @unique
                                         |  Title: String!
                                         |  Artist: Artist! @relation(link: INLINE)
                                         |  Tracks: [Track]
                                         |}
                                         |
                                         |type Genre {
                                         |  id: ID! @id
                                         |  GenreId: Int! @unique
                                         |  Name: String!
                                         |  Tracks: [Track]
                                         |}
                                         |
                                         |type MediaType {
                                         |  id: ID! @id
                                         |  MediaTypeId: Int! @unique
                                         |  Name: String!
                                         |  Tracks: [Track]
                                         |}
                                         |
                                         |type Track {
                                         |  id: ID! @id
                                         |  TrackId: Int! @unique
                                         |  Name: String!
                                         |  Album: Album! @relation(link: INLINE)
                                         |  MediaType: MediaType! @relation(link: INLINE)
                                         |  Genre: Genre! @relation(link: INLINE)
                                         |  Composer: String
                                         |  Milliseconds: Int!
                                         |  Bytes: Int!
                                         |  UnitPrice: Float!
                                         |}
                                         |""" }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(project)

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
        |                                   {Title: "TheAlbumWithoutTracks",
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

    server.query(
      """mutation completeArtist3 {createArtist(data:{
        |                         Name: "CompleteArtistWith2Albums"
        |                         ArtistId: 5,
        |                         Albums: {create: [
        |                                   {Title: "Album4",
        |                                    AlbumId: 4,
        |                                    Tracks:{create: [
        |                                             {
        |                                               Name:"Track4",
        |                                               TrackId: 4,
        |                                               Composer: "Composer1",
        |                                               Milliseconds: 15000,
        |                                               Bytes: 10024,
        |                                               UnitPrice: 12.51,
        |                                               Genre: {connect: {GenreId: 1}},
        |                                               MediaType: {connect: {MediaTypeId: 1}}
        |                                             },
        |                                             {
        |                                               Name:"Track5",
        |                                               TrackId: 5,
        |                                               Composer: "Composer2",
        |                                               Milliseconds: 19000,
        |                                               Bytes: 240,
        |                                               UnitPrice: 0.51,
        |                                               Genre: {connect: {GenreId: 1}},
        |                                               MediaType: {connect: {MediaTypeId: 1}}
        |                                             }
        |                                           ]}
        |                                   },
        |                                   {Title: "Album5",
        |                                    AlbumId: 5,
        |                                    Tracks:{create: [
        |                                             {
        |                                               Name:"Track6",
        |                                               TrackId: 6,
        |                                               Composer: "Composer1",
        |                                               Milliseconds: 100,
        |                                               Bytes: 724,
        |                                               UnitPrice: 31.51,
        |                                               Genre: {connect: {GenreId: 2}},
        |                                               MediaType: {connect: {MediaTypeId: 3}}
        |                                             },
        |                                             {
        |                                               Name:"Track7",
        |                                               TrackId: 7,
        |                                               Composer: "Composer3",
        |                                               Milliseconds: 100,
        |                                               Bytes: 2400,
        |                                               UnitPrice: 5.51,
        |                                               Genre: {connect: {GenreId: 1}},
        |                                               MediaType: {connect: {MediaTypeId: 1}}
        |                                             }
        |                                    ]}
        |                          }
        |                          ]}
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

  // MySql is case insensitive and Postgres case sensitive

  "MySql 1 level m-relation filter" should "work for _every, _some and _none" taggedAs (IgnorePostgres, IgnoreMongo) in {

    server.query(query = """{artists(where:{Albums_some:{Title_starts_with: "album"}}){Name}}""", project = project).toString should be(
      """{"data":{"artists":[{"Name":"CompleteArtist"},{"Name":"CompleteArtist2"},{"Name":"CompleteArtistWith2Albums"}]}}""")

    server.query(query = """{artists(where:{Albums_some:{Title_starts_with: "t"}}){Name}}""", project = project).toString should be(
      """{"data":{"artists":[{"Name":"ArtistWithOneAlbumWithoutTracks"}]}}""")

    server.query(query = """{artists(where:{Albums_every:{Title_contains: "album"}}){Name}}""", project = project).toString should be(
      """{"data":{"artists":[{"Name":"CompleteArtist"},{"Name":"ArtistWithoutAlbums"},{"Name":"ArtistWithOneAlbumWithoutTracks"},{"Name":"CompleteArtist2"},{"Name":"CompleteArtistWith2Albums"}]}}""")

    server.query(query = """{artists(where:{Albums_every:{Title_not_contains: "the"}}){Name}}""", project = project).toString should be(
      """{"data":{"artists":[{"Name":"CompleteArtist"},{"Name":"ArtistWithoutAlbums"},{"Name":"CompleteArtist2"},{"Name":"CompleteArtistWith2Albums"}]}}""")

    server.query(query = """{artists(where:{Albums_none:{Title_contains: "the"}}){Name}}""", project = project).toString should be(
      """{"data":{"artists":[{"Name":"CompleteArtist"},{"Name":"ArtistWithoutAlbums"},{"Name":"CompleteArtist2"},{"Name":"CompleteArtistWith2Albums"}]}}""")

    server.query(query = """{artists(where:{Albums_none:{Title_contains: "album"}}){Name}}""", project = project).toString should be(
      """{"data":{"artists":[{"Name":"ArtistWithoutAlbums"}]}}""")
  }

  "PostGres 1 level m-relation filter" should "work for  _some" taggedAs (IgnoreMySql) in {

    server.query(query = """{artists(where:{Albums_some:{Title_starts_with: "Album"}}){Name}}""", project = project).toString should be(
      """{"data":{"artists":[{"Name":"CompleteArtist"},{"Name":"CompleteArtist2"},{"Name":"CompleteArtistWith2Albums"}]}}""")

    server.query(query = """{artists(where:{Albums_some:{Title_starts_with: "T"}}){Name}}""", project = project).toString should be(
      """{"data":{"artists":[{"Name":"ArtistWithOneAlbumWithoutTracks"}]}}""")
  }

  "PostGres 1 level m-relation filter" should "work for _every" taggedAs (IgnoreMySql, IgnoreMongo) in {
    server.query(query = """{artists(where:{Albums_every:{Title_contains: "Album"}}){Name}}""", project = project).toString should be(
      """{"data":{"artists":[{"Name":"CompleteArtist"},{"Name":"ArtistWithoutAlbums"},{"Name":"ArtistWithOneAlbumWithoutTracks"},{"Name":"CompleteArtist2"},{"Name":"CompleteArtistWith2Albums"}]}}""")

    server.query(query = """{artists(where:{Albums_every:{Title_not_contains: "The"}}){Name}}""", project = project).toString should be(
      """{"data":{"artists":[{"Name":"CompleteArtist"},{"Name":"ArtistWithoutAlbums"},{"Name":"CompleteArtist2"},{"Name":"CompleteArtistWith2Albums"}]}}""")
  }

  "PostGres 1 level m-relation filter" should "work for _none" taggedAs (IgnoreMySql, IgnoreMongo) in {
    server.query(query = """{artists(where:{Albums_none:{Title_contains: "The"}}){Name}}""", project = project).toString should be(
      """{"data":{"artists":[{"Name":"CompleteArtist"},{"Name":"ArtistWithoutAlbums"},{"Name":"CompleteArtist2"},{"Name":"CompleteArtistWith2Albums"}]}}""")

    server.query(query = """{artists(where:{Albums_none:{Title_contains: "Album"}}){Name}}""", project = project).toString should be(
      """{"data":{"artists":[{"Name":"ArtistWithoutAlbums"}]}}""")
  }

  "2 level m-relation filter" should "work for some/some" in {

    // some|some
    server.query(query = """{artists(where:{Albums_some:{Tracks_some: {Milliseconds_lte: 9000}}}){Name}}""", project = project).toString should be(
      """{"data":{"artists":[{"Name":"CompleteArtist2"},{"Name":"CompleteArtistWith2Albums"}]}}""")

    server.query(query = """{artists(where:{Albums_some:{Tracks_some: {Bytes: 512}}}){Name}}""", project = project).toString should be(
      """{"data":{"artists":[{"Name":"CompleteArtist"}]}}""")
  }

  "2 level m-relation filter" should "work for _every, _some and _none" taggedAs (IgnoreMongo) in {
    // some|every
    server.query(query = """{artists(where:{Albums_some:{Tracks_every: {UnitPrice_gt: 2.50}}}){Name}}""", project = project).toString should be(
      """{"data":{"artists":[{"Name":"ArtistWithOneAlbumWithoutTracks"},{"Name":"CompleteArtist2"},{"Name":"CompleteArtistWith2Albums"}]}}""")

    server.query(query = """{artists(where:{Albums_some:{Tracks_every: {Milliseconds_gt: 9000}}}){Name}}""", project = project).toString should be(
      """{"data":{"artists":[{"Name":"CompleteArtist"},{"Name":"ArtistWithOneAlbumWithoutTracks"},{"Name":"CompleteArtistWith2Albums"}]}}""")

    // some|none
    server.query(query = """{artists(where:{Albums_some:{Tracks_none: {Milliseconds_lte: 9000}}}){Name}}""", project = project).toString should be(
      """{"data":{"artists":[{"Name":"CompleteArtist"},{"Name":"ArtistWithOneAlbumWithoutTracks"},{"Name":"CompleteArtistWith2Albums"}]}}""")

    server.query(query = """{artists(where:{Albums_some:{Tracks_none: {UnitPrice_lt: 2.0}}}){Name}}""", project = project).toString should be(
      """{"data":{"artists":[{"Name":"ArtistWithOneAlbumWithoutTracks"},{"Name":"CompleteArtist2"},{"Name":"CompleteArtistWith2Albums"}]}}""")

    // every|some
    server.query(query = """{artists(where:{Albums_every:{Tracks_some: {Bytes_lt: 1000}}}){Name}}""", project = project).toString should be(
      """{"data":{"artists":[{"Name":"CompleteArtist"},{"Name":"ArtistWithoutAlbums"},{"Name":"CompleteArtist2"},{"Name":"CompleteArtistWith2Albums"}]}}""")

    server.query(query = """{artists(where:{Albums_every:{Tracks_some: {Composer: "Composer3"}}}){Name}}""", project = project).toString should be(
      """{"data":{"artists":[{"Name":"ArtistWithoutAlbums"}]}}""")

    // every|every
    server.query(query = """{artists(where:{Albums_every:{Tracks_every: {Bytes_lte: 10000}}}){Name}}""", project = project).toString should be(
      """{"data":{"artists":[{"Name":"CompleteArtist"},{"Name":"ArtistWithoutAlbums"},{"Name":"ArtistWithOneAlbumWithoutTracks"},{"Name":"CompleteArtist2"}]}}""")

    server.query(query = """{artists(where:{Albums_every:{Tracks_every: {TrackId_in: [4,5,6,7]}}}){Name}}""", project = project).toString should be(
      """{"data":{"artists":[{"Name":"ArtistWithoutAlbums"},{"Name":"ArtistWithOneAlbumWithoutTracks"},{"Name":"CompleteArtistWith2Albums"}]}}""")

    // every|none
    server.query(query = """{artists(where:{Albums_every:{Tracks_none: {UnitPrice_lte: 1}}}){Name}}""", project = project).toString should be(
      """{"data":{"artists":[{"Name":"CompleteArtist"},{"Name":"ArtistWithoutAlbums"},{"Name":"ArtistWithOneAlbumWithoutTracks"},{"Name":"CompleteArtist2"}]}}""")

    server.query(query = """{artists(where:{Albums_every:{Tracks_none: {Composer: "Composer2"}}}){Name}}""", project = project).toString should be(
      """{"data":{"artists":[{"Name":"CompleteArtist"},{"Name":"ArtistWithoutAlbums"},{"Name":"ArtistWithOneAlbumWithoutTracks"}]}}""")

    // none|some
    server.query(query = """{artists(where:{Albums_none:{Tracks_some: {UnitPrice_lt: 1}}}){Name}}""", project = project).toString should be(
      """{"data":{"artists":[{"Name":"CompleteArtist"},{"Name":"ArtistWithoutAlbums"},{"Name":"ArtistWithOneAlbumWithoutTracks"},{"Name":"CompleteArtist2"}]}}""")

    server.query(query = """{artists(where:{Albums_none:{Tracks_some: {Composer: "Composer2"}}}){Name}}""", project = project).toString should be(
      """{"data":{"artists":[{"Name":"CompleteArtist"},{"Name":"ArtistWithoutAlbums"},{"Name":"ArtistWithOneAlbumWithoutTracks"}]}}""")

    // none|every
    server.query(query = """{artists(where:{Albums_none:{Tracks_every: {UnitPrice_gte: 5}}}){Name}}""", project = project).toString should be(
      """{"data":{"artists":[{"Name":"CompleteArtist"},{"Name":"ArtistWithoutAlbums"},{"Name":"CompleteArtist2"}]}}""")

    server.query(query = """{artists(where:{Albums_none:{Tracks_every: {Name_starts_with: "Track"}}}){Name}}""", project = project).toString should be(
      """{"data":{"artists":[{"Name":"ArtistWithoutAlbums"}]}}""")

    // none|none
    server.query(query = """{artists(where:{Albums_none:{Tracks_none: {Bytes_lt: 100}}}){Name}}""", project = project).toString should be(
      """{"data":{"artists":[{"Name":"ArtistWithoutAlbums"},{"Name":"CompleteArtist2"}]}}""")

    server.query(query = """{artists(where:{Albums_none:{Tracks_none: {Bytes_gte: 100}}}){Name}}""", project = project).toString should be(
      """{"data":{"artists":[{"Name":"CompleteArtist"},{"Name":"ArtistWithoutAlbums"},{"Name":"CompleteArtist2"},{"Name":"CompleteArtistWith2Albums"}]}}""")
  }

  "2 level m-relation filters that have subfilters that are connected with an implicit AND" should "work for _some" in {

    server
      .query(query = """{albums(where:{Tracks_some:{MediaType: {Name: "MediaType1"},Genre: {Name: "Genre1"}}}){Title}}""", project = project)
      .toString should be("""{"data":{"albums":[{"Title":"Album1"},{"Title":"Album4"},{"Title":"Album5"}]}}""")

  }

  "2 level m-relation filters that have subfilters that are connected with an implicit AND" should "work for _every" taggedAs (IgnoreMongo) in {

    server
      .query(query = """{albums(where:{Tracks_every:{MediaType: {Name: "MediaType1"},Genre: {Name: "Genre1"}}}){Title}}""", project = project)
      .toString should be("""{"data":{"albums":[{"Title":"Album1"},{"Title":"TheAlbumWithoutTracks"},{"Title":"Album4"}]}}""")

  }

  "2 level m-relation filters that have subfilters that are connected with an explicit AND" should "work for _some" in {

    server
      .query(query = """{albums(where:{Tracks_some:{AND:[{MediaType: {Name: "MediaType1"}},{Genre: {Name: "Genre1"}}]}}){Title}}""", project = project)
      .toString should be("""{"data":{"albums":[{"Title":"Album1"},{"Title":"Album4"},{"Title":"Album5"}]}}""")

    server
      .query(query = """{albums(where:{Tracks_some:{AND:[{MediaType: {Name: "MediaType2"}}]}}){Title}}""", project = project)
      .toString should be("""{"data":{"albums":[{"Title":"Album3"}]}}""")

    server
      .query(query = """{albums(where:{Tracks_some:{AND:[]}}){Title}}""", project = project)
      .toString should be("""{"data":{"albums":[{"Title":"Album1"},{"Title":"Album3"},{"Title":"Album4"},{"Title":"Album5"}]}}""")

  }

  "2 level m-relation filters that have subfilters that are connected with an explicit AND" should "work for _every" taggedAs (IgnoreMongo) in {

    server
      .query(query = """{albums(where:{Tracks_every:{AND:[{MediaType: {Name: "MediaType1"}},{Genre: {Name: "Genre1"}}]}}){Title}}""", project = project)
      .toString should be("""{"data":{"albums":[{"Title":"Album1"},{"Title":"TheAlbumWithoutTracks"},{"Title":"Album4"}]}}""")

    server
      .query(query = """{albums(where:{Tracks_every:{AND:[{MediaType: {Name: "MediaType2"}}]}}){Title}}""", project = project)
      .toString should be("""{"data":{"albums":[{"Title":"TheAlbumWithoutTracks"}]}}""")

    server
      .query(query = """{albums(where:{Tracks_every:{AND:[]}}){Title}}""", project = project)
      .toString should be(
      """{"data":{"albums":[{"Title":"Album1"},{"Title":"TheAlbumWithoutTracks"},{"Title":"Album3"},{"Title":"Album4"},{"Title":"Album5"}]}}""")
  }

  "2 level m-relation filters that have subfilters that are connected with an explicit OR" should "work" taggedAs (IgnoreMongo) in {

    server
      .query(query = """{albums(where:{Tracks_some:{OR:[{MediaType: {Name: "MediaType1"}},{Genre: {Name: "Genre2"}}]}}){Title}}""", project = project)
      .toString should be("""{"data":{"albums":[{"Title":"Album1"},{"Title":"Album3"},{"Title":"Album4"},{"Title":"Album5"}]}}""")

    server
      .query(query = """{albums(where:{Tracks_every:{OR:[{MediaType: {Name: "MediaType1"}},{Genre: {Name: "Genre2"}}]}}){Title}}""", project = project)
      .toString should be("""{"data":{"albums":[{"Title":"Album1"},{"Title":"TheAlbumWithoutTracks"},{"Title":"Album4"},{"Title":"Album5"}]}}""")

    server
      .query(query = """{albums(where:{Tracks_some:{OR:[{MediaType: {Name: "MediaType2"}}]}}){Title}}""", project = project)
      .toString should be("""{"data":{"albums":[{"Title":"Album3"}]}}""")

    server
      .query(query = """{albums(where:{Tracks_every:{OR:[{MediaType: {Name: "MediaType2"}}]}}){Title}}""", project = project)
      .toString should be("""{"data":{"albums":[{"Title":"TheAlbumWithoutTracks"}]}}""")

    server
      .query(query = """{albums(where:{Tracks_some:{OR:[]}}){Title}}""", project = project)
      .toString should be("""{"data":{"albums":[]}}""")

    server
      .query(query = """{albums(where:{Tracks_every:{OR:[]}}){Title}}""", project = project)
      .toString should be("""{"data":{"albums":[{"Title":"TheAlbumWithoutTracks"}]}}""")
  }

  "2 level m-relation filters that have subfilters that are connected with an explicit NOT" should "work" taggedAs (IgnoreMongo) in {

    server
      .query(query = """{albums(where:{Tracks_some:{NOT:[{MediaType: {Name: "MediaType1"}},{Genre: {Name: "Genre1"}}]}}){Title}}""", project = project)
      .toString should be("""{"data":{"albums":[{"Title":"Album3"},{"Title":"Album5"}]}}""")

    server
      .query(query = """{albums(where:{Tracks_every:{NOT:[{MediaType: {Name: "MediaType1"}},{Genre: {Name: "Genre1"}}]}}){Title}}""", project = project)
      .toString should be("""{"data":{"albums":[{"Title":"TheAlbumWithoutTracks"},{"Title":"Album3"}]}}""")

    server
      .query(query = """{albums(where:{Tracks_some:{NOT:[{MediaType: {Name: "MediaType2"}}]}}){Title}}""", project = project)
      .toString should be("""{"data":{"albums":[{"Title":"Album1"},{"Title":"Album3"},{"Title":"Album4"},{"Title":"Album5"}]}}""")

    server
      .query(query = """{albums(where:{Tracks_every:{NOT:[{MediaType: {Name: "MediaType2"}}]}}){Title}}""", project = project)
      .toString should be("""{"data":{"albums":[{"Title":"Album1"},{"Title":"TheAlbumWithoutTracks"},{"Title":"Album4"},{"Title":"Album5"}]}}""")

    server
      .query(query = """{albums(where:{Tracks_some:{NOT:[]}}){Title}}""", project = project)
      .toString should be("""{"data":{"albums":[{"Title":"Album1"},{"Title":"Album3"},{"Title":"Album4"},{"Title":"Album5"}]}}""")

    server
      .query(query = """{albums(where:{Tracks_every:{NOT:[]}}){Title}}""", project = project)
      .toString should be(
      """{"data":{"albums":[{"Title":"Album1"},{"Title":"TheAlbumWithoutTracks"},{"Title":"Album3"},{"Title":"Album4"},{"Title":"Album5"}]}}""")
  }

  "3 level filters" should "work" in {
    val result = server.query(
      """{
        |  genres(where: {
        |    Tracks_some: {
        |      Album: {
        |        Artist: {
        |          ArtistId: 1
        |        }
        |      }
        |    }
        |  }){
        |    GenreId
        |  }
        |}
        |
      """.stripMargin,
      project
    )

    result.pathAsJsValue("data") shouldBe ("""{"genres":[{"GenreId":1}]}""".parseJson)
  }

  "an empty _none filter" should "return all nodes that do have an empty relation" taggedAs (IgnoreMongo) in {
    server.query(query = """{genres(where:{Tracks_none:{}}){Name}}""", project = project).toString should be(
      """{"data":{"genres":[{"Name":"GenreThatIsNotUsed"}]}}""")
  }

  "an empty _some filter" should "return all nodes that do have a non-empty relation" in {
    server.query(query = """{genres(where:{Tracks_some:{}}){Name}}""", project = project).toString should be(
      """{"data":{"genres":[{"Name":"Genre1"},{"Name":"Genre2"},{"Name":"Genre3"}]}}""")
  }
}
