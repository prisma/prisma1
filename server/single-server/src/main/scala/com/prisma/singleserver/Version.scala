package cool.graph.singleserver

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import cool.graph.graphql.GraphQlClientImpl
import spray.json._

import scala.concurrent.Future

object Version {
  import DefaultJsonProtocol._

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
          val json = resp.body.parseJson
          val updateStatus = json.asJsObject
            .fields("data")
            .asJsObject()
            .fields("checkUpdate")
            .asJsObject()
            .fields("isUpToDate")
            .convertTo[Boolean]

          if (updateStatus) println("Version is up to date.")
          else println("Update available.")
        } else {
          println("Unable to fetch version info.")
        }

        Future.successful(())
      }
      .recoverWith {
        case _ =>
          println("Unable to fetch version info.")
          Future.successful(())
      }
  }
}
