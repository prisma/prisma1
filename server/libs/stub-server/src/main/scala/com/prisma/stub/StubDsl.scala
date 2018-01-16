package cool.graph.stub

object StubDsl {
  object Default {
    object Request {
      def apply(httpMethod: String, pathAndQuery: String): Request = withBody(httpMethod, pathAndQuery, "")

      def withBody(httpMethod: String, pathAndQuery: String, body: String): Request = {
        val path        = pathAndQuery.takeWhile(_ != '?')
        val queryString = pathAndQuery.dropWhile(_ != '?').drop(1)
        val queryParams = QueryString.queryStringToMap(queryString)
        Request(httpMethod, path, queryParams, body)
      }
    }

    case class Request(httpMethod: String, path: String, queryParams: Map[String, Any] = Map.empty, body: String = "") {
      def stub(response: StubResponse): Stub = {
        Stub(httpMethod, path, queryParams, body, response)
      }

      def stub(status: Int, body: String, headers: Map[String, String] = Map.empty): Stub = {
        stub(StaticStubResponse(status, body, headers))
      }

      def stub(fn: (StubRequest) => StaticStubResponse): Stub = {
        stub(DynamicStubResponse(fn))
      }
    }
  }
}
