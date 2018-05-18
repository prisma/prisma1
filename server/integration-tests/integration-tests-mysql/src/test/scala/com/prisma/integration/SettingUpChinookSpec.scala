package com.prisma.integration

import com.prisma.api.import_export.BulkImport
import org.scalatest.{FlatSpec, Matchers}
import java.io.FileInputStream

import play.api.libs.json.Json

class SettingUpChinookSpec extends FlatSpec with Matchers with IntegrationBaseSpec {

  "Importing Chinook" should "work " ignore {

    val schema =
      """type Artist {
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
        |"""

    val (project, _) = setupProject(schema)

    val importer = new BulkImport(project)

    def importFile(fileName: String) = {
      val path   = "/Users/matthias/repos/github.com/graphcool/framework/server/integration-tests/integration-tests-mysql/src/test/scala/com/prisma/integration/" + fileName
      val stream = new FileInputStream(path)
      val json   = try { Json.parse(stream) } finally { stream.close() }
      importer.executeImport(json).await(50)
    }

    importFile("nodes01.json")
    importFile("nodes02.json")
    importFile("relations01.json")
    importFile("relations02.json")
    importFile("lists01.json")

    val starttime = System.currentTimeMillis()

    apiServer.query("""query{artists(where:{Albums_some:{Tracks_some:{Milliseconds_gt: 500000}}}){Name}}""", project)
//    apiServer.query("""query{artists(where:{Albums_some:{Title_starts_with: "B" Title_ends_with:"C"}}){Name}}""", project)

    val endtime = System.currentTimeMillis()

    println("duration: " + (endtime - starttime))
//    select *
//      from Artist
//      where (
//        exists (
//          select *
//            from  Album as  Album_Artist
//            inner join  _AlbumToArtist
//            on  Album_Artist.id =  _AlbumToArtist.A
//            where _AlbumToArtist.B = Artist.id
//            and (
//            exists (
//              select *
//                from  Track as  Track_Album_Artist
//                inner join  _AlbumToTrack
//                on  Track_Album_Artist.id = _AlbumToTrack.B
//                where  _AlbumToTrack.A =  Album_Artist.id
//                and ( Track_Album_Artist. Milliseconds > 500000)))))
//    order by Artist.id asc;

//    explain select name
//    from Artist
//      where (
//        exists (
//          select *
//            from  Album as  Album_Artist
//            inner join  _AlbumToArtist
//            on  Album_Artist.id =  _AlbumToArtist.A
//            where _AlbumToArtist.B = Artist.id
//            and (
//            exists (
//              select *
//                from  Track as  Track_Album_Artist
//                inner join  _AlbumToTrack
//                on  Track_Album_Artist.id = _AlbumToTrack.B
//                where  _AlbumToTrack.A =  Album_Artist.id
//                and ( Track_Album_Artist. Milliseconds > 500000)))))
//    order by Artist.name asc;
//
//
//    explain
//    Select distinct Artist.name
//    From Artist
//      join _AlbumToArtist on _AlbumToArtist.B = Artist.`id`
//    join Album 			on _AlbumToArtist.A = Album.`id`
//    join _AlbumToTrack  on _AlbumToTrack.A = Album.`id`
//    join Track 			on _AlbumToTrack.B = Track.`id`
//    Where Track.`Milliseconds` > 500000
//    And Artist.Name = 'Kurt'
//    order by Artist.name asc;

  }
}
