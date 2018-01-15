package com.prisma.stub

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
