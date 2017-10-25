package cool.graph.stub

import java.util.function.BinaryOperator
import javax.servlet.http.HttpServletRequest

import scala.collection.SortedMap

trait RequestLike {
  def httpMethod: String
  def path: String
  def queryMap: Map[String, Any]
  def body: String

  val querySortedMap: SortedMap[String, Any] = QueryString.mapToSortedMap(queryMap)
  val queryString: String                    = QueryString.queryMapToString(queryMap)

  def isPostOrPatch: Boolean = httpMethod.equalsIgnoreCase("POST") || httpMethod.equalsIgnoreCase("PATCH")
}

case class Stub(
    httpMethod: String,
    path: String,
    queryMap: Map[String, Any],
    body: String,
    stubbedResponse: StubResponse,
    shouldCheckBody: Boolean = true
) extends RequestLike {
  def ignoreBody: Stub = copy(shouldCheckBody = false)
}

trait StubResponse {
  def getStaticStubResponse(stubRequest: StubRequest): StaticStubResponse

  def status: Int
  def body: String
  def headers: Map[String, String]
}

case class StaticStubResponse(status: Int, body: String, headers: Map[String, String] = Map.empty) extends StubResponse {
  override def getStaticStubResponse(stubRequest: StubRequest): StaticStubResponse = this
}
case class DynamicStubResponse(fn: (StubRequest) => StaticStubResponse) extends StubResponse {

  def getStaticStubResponse(stubRequest: StubRequest) = fn(stubRequest)

  override def status: Int                  = sys.error("not implemented... please use getStaticStubResponse")
  override def body: String                 = sys.error("not implemented... please use getStaticStubResponse")
  override def headers: Map[String, String] = sys.error("not implemented... please use getStaticStubResponse")
}

object JavaServletRequest {
  def body(request: HttpServletRequest): String = {
    val reduceFn = new BinaryOperator[String] {
      override def apply(acc: String, actual: String): String = acc + actual
    }
    request.getReader().lines().reduce("", reduceFn)
  }

  def headers(request: HttpServletRequest): Map[String, String] = {
    import scala.collection.mutable
    val map: mutable.Map[String, String] = mutable.Map.empty
    val headerNames                      = request.getHeaderNames;

    while (headerNames.hasMoreElements) {
      val key = Option(headerNames.nextElement());
      val value = for {
        k <- key
        v <- Option(request.getHeader(k))
      } yield v

      (key, value) match {
        case (Some(k), Some(v)) => map.put(k, v);
        case _                  => ()
      }
    }
    map.toMap
  }
}

object StubRequest {
  def fromHttpRequest(servletRequest: HttpServletRequest): StubRequest = {
    val body = JavaServletRequest.body(servletRequest)
    StubRequest(
      servletRequest.getMethod,
      servletRequest.getPathInfo,
      QueryString.queryStringToMap(servletRequest.getQueryString),
      body,
      JavaServletRequest.headers(servletRequest)
    )
  }

  def apply(
      httpMethod: String,
      path: String,
      queryMap: Map[String, Any],
      body: String
  ): StubRequest = StubRequest(httpMethod, path, queryMap, body, Map.empty)
}

case class StubRequest(
    httpMethod: String,
    path: String,
    queryMap: Map[String, Any],
    body: String,
    headers: Map[String, String]
) extends RequestLike
