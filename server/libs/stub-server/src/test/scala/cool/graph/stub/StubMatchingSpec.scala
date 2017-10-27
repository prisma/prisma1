package cool.graph.stub

import org.specs2.mutable.Specification

class StubMatchingSpec extends Specification {
  sequential

  val em = Map.empty[String, String]

  "a stub that does not match Path and Method" should {
    "return a DoesNotMatch if the Method does not match" in {
      val aufgabenStub = Stub("GET", "/some/path", em, body = "", StaticStubResponse(200, """aufgaben-response"""))
      val request      = StubRequest("POST", "/some/path", Map("list_id" -> "1"), body = "")
      val matchResult  = StubMatching.matchStub(aufgabenStub, request)
      matchResult.isMatch must beFalse
      matchResult.rank mustEqual 2 // path + query string
    }
    "return a DoesNotMatch if the Path does not match" in {
      val aufgabenStub = Stub("GET", "/some/pathZZ", em, body = "", StaticStubResponse(200, """aufgaben-response"""))
      val request      = StubRequest("GET", "/some/path", Map("list_id" -> "1"), body = "")
      val matchResult  = StubMatching.matchStub(aufgabenStub, request)
      matchResult.isMatch must beFalse
      matchResult.rank mustEqual 1 // method
    }
    "return a DoesNotMatch event if the queryString matches" in {
      val aufgabenStub =
        Stub("GET", "/some/pathZZ", Map("list_id" -> "1"), body = "", StaticStubResponse(200, """aufgaben-response"""))
      val request     = StubRequest("GET", "/some/path", Map("list_id" -> "1"), body = "")
      val matchResult = StubMatching.matchStub(aufgabenStub, request)
      matchResult.isMatch must beFalse
      matchResult.rank mustEqual 2 // method + query string
    }
  }

  "a stub that matches Path and Method, but NOT QueryString" should {
    "return a MatchResult with rank 2 if Path and Method match" in {
      val aufgabenStub = Stub("GET", "/some/path", em, body = "", StaticStubResponse(200, """aufgaben-response"""))
      val request      = StubRequest("GET", "/some/path", Map("list_id" -> "1"), body = "")
      val matchResult  = StubMatching.matchStub(aufgabenStub, request)
      matchResult.isMatch must beTrue
      matchResult.rank mustEqual 2
    }
    "return a MatchResult with rank 2 if Path and Method match" in {
      val aufgabenStub = Stub("GET", "/some/path", em, body = "", StaticStubResponse(200, """aufgaben-response"""))
      val request      = StubRequest("GET", "/some/path", em, body = "")
      val matchResult  = StubMatching.matchStub(aufgabenStub, request)
      matchResult.isMatch must beTrue
      matchResult.rank mustEqual 2
    }
    "return a DoesNotMatch if path Path and Method match and QueryString does not match" in {
      val aufgabenStub =
        Stub("GET", "/some/path?param=value", em, body = "", StaticStubResponse(200, """aufgaben-response"""))
      val request     = StubRequest("GET", "/some/path?blub=bla", em, body = "")
      val matchResult = StubMatching.matchStub(aufgabenStub, request)
      matchResult.isMatch must beFalse
    }
  }
  "a stub that matches Path, Method and the QueryString at least partially" should {
    "return a MatchResult with rank 3, when the QueryString matches on one param" in {
      val aufgabenStub =
        Stub("GET", "/some/path", Map("list_id" -> "1"), body = "", StaticStubResponse(200, "less-matching"))
      val request     = StubRequest("GET", "/some/path", Map("list_id" -> "1", "foo" -> "bar"), body = "")
      val matchResult = StubMatching.matchStub(aufgabenStub, request)
      matchResult.isMatch must beTrue
      matchResult.rank mustEqual 3 // method + path + 1 query param
    }
    "return a Match, where the rank equals the number of params matching + 1" in {
      val aufgabenStub = Stub("GET", "/some/path", Map("list_id" -> "1", "foo" -> "bar"), body = "", StaticStubResponse(200, """aufgaben-response"""))
      val request      = StubRequest("GET", "/some/path", Map("list_id" -> "1", "foo" -> "bar"), body = "")
      val matchResult  = StubMatching.matchStub(aufgabenStub, request)
      matchResult.isMatch must beTrue
      matchResult.rank mustEqual 4 // method + path + 2 query params
    }
    "return a Match even if one query value is the string representation of the other" in {
      val aufgabenStub =
        Stub("GET", "/some/path", Map("list_id" -> 1), body = "", StaticStubResponse(200, """aufgaben-response"""))
      val request     = StubRequest("GET", "/some/path", Map("list_id" -> "1"), body = "")
      val matchResult = StubMatching.matchStub(aufgabenStub, request)
      matchResult.isMatch must beTrue
      matchResult.rank mustEqual 3 // method + path + 1 query param
    }
  }

  "a stub for a PATCH/POST request should" should {
    "return a MatchResult with rank 1, when the body matches" in {
      val body                 = """{ "foo" : "bar", "x" : "y" }"""
      val bodyInDifferentOrder = """{ "x" : "y", "foo" : "bar" }"""
      val stub                 = Stub("POST", "/path", em, body = body, StaticStubResponse(200, "less-matching"))
      val request              = StubRequest("POST", "/path", em, body = bodyInDifferentOrder)
      val matchResult          = StubMatching.matchStub(stub, request)
      matchResult.isMatch must beTrue
    }
    "return DoesNotMatch, when body does NOT match" in {
      val stub        = Stub("POST", "/path", em, body = "foo", StaticStubResponse(200, "less-matching"))
      val request     = StubRequest("POST", "/path", em, body = "bar")
      val matchResult = StubMatching.matchStub(stub, request)
      matchResult.isMatch must beFalse
    }
  }

  "StubMatching.matchStubs" should {
    val stubRequest = StubRequest("GET", "/path", Map("foo_id" -> "1"), body = "")

    val matchingStubRank2 = Stub("GET", "/path", queryMap = Map("foo_id" -> "1"), body = "", StaticStubResponse(200, "this-must-match-with-rank-2"))
    val matchingStubRank1 =
      Stub("GET", "/path", queryMap = em, body = "", StaticStubResponse(200, "this-must-match-with-rank-1"))
    val nonMatchingStub =
      Stub("POST", "/path", queryMap = em, body = "", StaticStubResponse(200, "this-must-not-match"))

    val matchResults =
      StubMatching.matchStubs(stubRequest, List(nonMatchingStub, matchingStubRank1, matchingStubRank2))

    "sort the MatchResult according to their rank" in {
      val first  = matchResults(0)
      val second = matchResults(1)
      val third  = matchResults(2)
      first.rank must beGreaterThan(second.rank)
      second.rank must beGreaterThan(third.rank)
    }
    "retain stubs that are not matching" in {
      matchResults must haveSize(3)
      matchResults.last.isMatch must beFalse
      matchResults.last.noMatchMessage must contain("expected request method")
    }
  }
}
