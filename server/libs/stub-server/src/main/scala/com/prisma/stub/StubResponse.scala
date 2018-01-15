package com.prisma.stub

trait StubResponse {
  def getStaticStubResponse(stubRequest: StubRequest): StaticStubResponse

  def status: Int
  def body: String
  def headers: Map[String, String]
}
