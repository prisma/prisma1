package com.prisma.image

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.prisma.graphql.GraphQlClientImpl
import play.api.libs.json.Json

import scala.concurrent.Future

object Version {

  // todo overhaul
  def check()(implicit system: ActorSystem, materializer: ActorMaterializer): Future[_] = {
    import system.dispatcher
    val client = GraphQlClientImpl("https://check-update.graph.cool", Map.empty, Http()(system))

    client
      .sendQuery("""
        |mutation {
        |  checkUpdate(version: "1.0.0") {
        |    newestVersion,
        |    isUpToDate
        |  }
        |}
      """.stripMargin)
      .flatMap { resp =>
        if (resp.is200) {
//          val json         = Json.parse(resp.body)
//          val updateStatus = (json \ "data" \ "checkUpdate" \ "isUpToDate").as[Boolean]
        } else {}

        Future.successful(())
      }
      .recoverWith {
        case _ =>
//          println("Unable to fetch version info.")
          Future.successful(())
      }
  }
}
