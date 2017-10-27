package cool.graph.util

import cool.graph.util.json.Json._
import org.scalatest.{Matchers, WordSpec}
import spray.json._

class JsonStringExtensionsSpec extends WordSpec with Matchers {

  "pathAs" should {
    "get string" in {
      """{"a": "b"}""".parseJson.pathAsString("a") should be("b")
    }

    "get string nested in array" in {
      val json = """{"a": ["b", "c"]}""".parseJson
      json.pathAsString("a.[0]") should be("b")
      json.pathAsString("a.[1]") should be("c")
    }

    "get string nested in object in array" in {
      val json = """{"a": [{"b":"c"}, {"b":"d"}]}""".parseJson
      json.pathAsString("a.[0].b") should be("c")
      json.pathAsString("a.[1].b") should be("d")
    }
  }

}
