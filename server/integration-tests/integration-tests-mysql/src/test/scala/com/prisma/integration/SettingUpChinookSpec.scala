package com.prisma.integration

import com.prisma.api.import_export.BulkImport
import org.scalatest.{FlatSpec, Matchers}
import java.io.FileInputStream

import play.api.libs.json.Json

class SettingUpChinookSpec extends FlatSpec with Matchers with IntegrationBaseSpec {

  "Importing Chinook" should "work " ignore {

    val schema =
      """type Artist {
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
        |  Artist: Artist!
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

//    importFile("nodes01.json")
//    importFile("nodes02.json")
//    importFile("relations01.json")
//    importFile("relations02.json")
//    importFile("lists01.json")

    def runquery = {
      val starttime = System.currentTimeMillis()

      //    apiServer.query("""query{artists(where:{Albums_some:{Tracks_some:{Milliseconds_gt: 500000}}}){Name}}""", project)
      //    apiServer.query("""query{artists(where:{Albums_some:{Title_starts_with: "B" Title_ends_with:"C"}}){Name}}""", project)
//      """query prisma_deeplyNested {tracks(where: {Album_some:{ MediaType:{Name_starts_with:""}, Genre:{Name_starts_with:""}}}) { id}}""",
//      """query prisma_deeplyNested {tracks(where: {Album:{ Artist:{Name_starts_with:"artist123"}}}) { id}}""",

      apiServer.query(
        """query {artists(where: {Albums_none:{ Tracks_every:{Milliseconds_gt:130000}}}){
          | Name
          | Albums(first:5){
          |     Title
          |     Tracks(first:5){
          |       Name
          |     }
          | }
          |}}""".stripMargin,
        project
      )

      val endtime = System.currentTimeMillis()

      println("duration: " + (endtime - starttime))
    }

    runquery

//    for (a <- 1 to 50) {
//      runquery
//    }
  }
}
