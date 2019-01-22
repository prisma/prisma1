package com.prisma.akkautil.http

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, ResponseEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.prisma.akkautil.SingleThreadedActorSystem
import com.prisma.akkautil.specs2.AcceptanceSpecification
import org.specs2.matcher.MatchResult
import org.specs2.specification.AfterAll

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class ServerExecutorSpec extends AcceptanceSpecification with AfterAll {
  def is = s2"""
    The ServerExecutor must
      execute a single server correctly $singleServerExecution
      execute a single server with prefix correctly $singleServerPrefixExecution
      merge routes from multiple servers correctly $mergeRoutesTest
  """

  implicit val system       = SingleThreadedActorSystem("server-spec")
  implicit val materializer = ActorMaterializer()

  case class TestServer(
      innerRoutes: Route,
      prefix: String = "",
      check: Unit => Future[_] = _ => Future.successful(())
  ) extends Server

  def withServerExecutor(servers: Server*)(checkFn: ServerExecutor => MatchResult[Any]): MatchResult[Any] = {
    val server = ServerExecutor(port = 8000 + new scala.util.Random().nextInt(50000), servers: _*)

    try {
      Await.result(server.start, 5.seconds)
      checkFn(server)
    } finally {
      server.stopBlocking()
    }
  }

  def bodyAsString(entity: ResponseEntity): String = {
    Await.result(Unmarshal(entity).to[String], 5.seconds)
  }

  implicit class TestServerExecutorExtensions(executor: ServerExecutor) {
    def makeRequest(path: String): HttpResponse = {
      Await.result(Http().singleRequest(HttpRequest(uri = s"http://localhost:${executor.port}/${path.stripPrefix("/")}")), 5.second)
    }
  }

  override def afterAll = Await.result(system.terminate(), 15.seconds)

  val route: Route = get {
    pathPrefix("firstPrefix") {
      complete("firstPrefixRouteResult")
    } ~ pathPrefix("secondPrefix") {
      complete("secondPrefixRouteResult")
    }
  }

  def singleServerExecution: MatchResult[Any] = {
    val testServer = TestServer(route)

    withServerExecutor(testServer) { server =>
      val statusResult = server.makeRequest("/status")
      statusResult.status.intValue() mustEqual 200

      val firstRouteResult = server.makeRequest("/firstPrefix")
      firstRouteResult.status.intValue() mustEqual 200
      bodyAsString(firstRouteResult.entity) mustEqual "firstPrefixRouteResult"

      val secondRouteResult = server.makeRequest("/secondPrefix")
      secondRouteResult.status.intValue() mustEqual 200
      bodyAsString(secondRouteResult.entity) mustEqual "secondPrefixRouteResult"
    }
  }

  def singleServerPrefixExecution: MatchResult[Any] = {
    val testServer = TestServer(route, prefix = "testPrefix")

    withServerExecutor(testServer) { server =>
      val statusResult = server.makeRequest("/status")
      statusResult.status.intValue() mustEqual 200

      val firstRouteErrorResult = server.makeRequest("/firstPrefix")
      firstRouteErrorResult.status.intValue() mustEqual 404

      val firstRouteSuccessResult = server.makeRequest("/testPrefix/firstPrefix")
      firstRouteSuccessResult.status.intValue() mustEqual 200
      bodyAsString(firstRouteSuccessResult.entity) mustEqual "firstPrefixRouteResult"
    }
  }

  def mergeRoutesTest: MatchResult[Any] = {
    val testServer      = TestServer(route, prefix = "v1")
    val otherTestServer = TestServer(route ~ path("surprise") { complete("much surprise, wow") }, prefix = "v2")

    withServerExecutor(testServer, otherTestServer) { server =>
      val statusResult = server.makeRequest("/status")
      statusResult.status.intValue() mustEqual 200

      val firstRouteErrorResult = server.makeRequest("/firstPrefix")
      firstRouteErrorResult.status.intValue() mustEqual 404

      val firstRouteSurpriseErrorResult = server.makeRequest("/v1/surprise")
      firstRouteSurpriseErrorResult.status.intValue() mustEqual 404

      val firstRouteSuccessResult = server.makeRequest("/v2/surprise")
      firstRouteSuccessResult.status.intValue() mustEqual 200
      bodyAsString(firstRouteSuccessResult.entity) mustEqual "much surprise, wow"
    }
  }
}
