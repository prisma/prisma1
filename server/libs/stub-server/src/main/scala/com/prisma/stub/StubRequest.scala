package com.prisma.stub

import javax.servlet.http.HttpServletRequest

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
