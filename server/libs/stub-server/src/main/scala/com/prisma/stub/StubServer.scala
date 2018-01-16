package cool.graph.stub

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import cool.graph.stub.StubMatching.MatchResult
import org.eclipse.jetty.server.handler.AbstractHandler
import org.eclipse.jetty.server.{Server, Request => JettyRequest}

import scala.collection.mutable

case class StubServer(stubs: List[Stub], port: Int, stubNotFoundStatusCode: Int) {
  org.eclipse.jetty.util.log.Log.setLog(new JustWarningsLogger)
  val server        = new Server(port)
  def createHandler = StubServerHandler(stubs, stubNotFoundStatusCode)

  def start: Unit = server.start
  def stop: Unit  = server.stop

  def requests                      = handler.requests
  def lastRequest                   = handler.requests.head
  def lastRequest(path: String)     = handler.requests.filter(_.path == path).head
  def requestCount(stub: Stub): Int = handler.requestCount(stub)
  val handler                       = createHandler
  server.setHandler(handler)
}

case class StubServerHandler(stubs: List[Stub], stubNotFoundStatusCode: Int) extends AbstractHandler {
  var requests: List[StubRequest] = List()

  def handle(target: String, baseRequest: JettyRequest, servletRequest: HttpServletRequest, response: HttpServletResponse): Unit = {
    val stubResponse = try {
      val stubRequest = StubRequest.fromHttpRequest(servletRequest)
      requests = stubRequest :: requests
      stubResponseForRequest(stubRequest)
    } catch {
      case e: Throwable => failedResponse(e)
    }
    response.setContentType("application/json")
    response.setStatus(stubResponse.status)
    stubResponse.headers.foreach(kv => response.setHeader(kv._1, kv._2))
    response.getWriter.print(stubResponse.body)
    baseRequest.setHandled(true)
  }

  def stubResponseForRequest(stubRequest: StubRequest): StaticStubResponse = {
    val matches      = StubMatching.matchStubs(stubRequest, stubs)
    val topCandidate = matches.find(_.isMatch)
    topCandidate match {
      case Some(result) =>
        recordStubHit(result.stub)
        result.stub.stubbedResponse.getStaticStubResponse(stubRequest)
      case None =>
        noMatchResponse(stubRequest, matches)
    }
  }

  def failedResponse(e: Throwable) = {
    e.printStackTrace()
    StaticStubResponse(stubNotFoundStatusCode, s"Stub Matching failed with the following exception: ${e.toString}")
  }

  def noMatchResponse(request: StubRequest, notMatches: List[MatchResult]) = {
    val queryString = request.queryMap.map { case (k, v) => s"$k=$v" }.foldLeft("?") { case (acc, x) => s"$acc&$x" }
    val noMatchReasons = if (stubs.isEmpty) {
      """ "There are no registered stubs in the server!" """
    } else {
      notMatches.map(x => s""" "${x.noMatchMessage}" """).mkString(",\n")
    }
    val responseJson = {
      s"""{
         | "message": "No stub found for request [URL: ${request.path}$queryString] [METHOD: ${request.httpMethod}}] [BODY: ${request.body}]",
         | "noMatchReasons" : [
         |  $noMatchReasons
         | ]
         |}""".stripMargin
    }
    StaticStubResponse(stubNotFoundStatusCode, responseJson)
  }

  def requestCount(stub: Stub): Int = requestCountMap.getOrElse(stub, 0)

  private def recordStubHit(stub: Stub): Unit = {
    val numberOfRequests = requestCountMap.getOrElse(stub, 0)
    requestCountMap.update(stub, numberOfRequests + 1)
  }
  private val requestCountMap: mutable.Map[Stub, Int] = mutable.Map.empty
}
