package com.prisma.stub

import com.prisma.stub.StubDsl.Default.Request
import org.specs2.mutable.Specification

class StubDslSpec extends Specification {

  val uri = "postgres://tim:\"N8cl%&eHEn<{?1yYkzG>*ks=\"@tims-test-db.clfeqqifnebj.eu-west-1.rds.amazonaws.com/postgres?ssl=1"

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
