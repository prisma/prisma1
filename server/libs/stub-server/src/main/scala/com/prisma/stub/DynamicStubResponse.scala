package com.prisma.stub

case class DynamicStubResponse(fn: (StubRequest) => StaticStubResponse) extends StubResponse {

  def getStaticStubResponse(stubRequest: StubRequest) = fn(stubRequest)

  override def status: Int                  = sys.error("not implemented... please use getStaticStubResponse")
  override def body: String                 = sys.error("not implemented... please use getStaticStubResponse")
  override def headers: Map[String, String] = sys.error("not implemented... please use getStaticStubResponse")
}
