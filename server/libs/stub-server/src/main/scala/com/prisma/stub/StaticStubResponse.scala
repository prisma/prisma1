package com.prisma.stub

case class StaticStubResponse(status: Int, body: String, headers: Map[String, String] = Map.empty) extends StubResponse {
  override def getStaticStubResponse(stubRequest: StubRequest): StaticStubResponse = this
}
