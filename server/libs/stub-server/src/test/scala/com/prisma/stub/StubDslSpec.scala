package cool.graph.stub

import cool.graph.stub.StubDsl.Default.Request
import org.specs2.mutable.Specification

class StubDslSpec extends Specification {

  val path   = "some/freaking/path"
  val params = Map("a" -> 1)

  val responseBody = "the body"
  val response     = StaticStubResponse(333, responseBody)

  "using the default stub DSL" should {
    "produce a stub response with headers" in {
      val stub: Stub = Request("POST", path).stub(200, responseBody, Map("X-Test" -> "Test"))
      stub.stubbedResponse.headers("X-Test") must equalTo("Test")
      stub.stubbedResponse.body must equalTo(responseBody)
    }
  }
}
