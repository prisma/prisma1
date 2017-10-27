package cool.graph

import org.scalatest.{FlatSpec, Matchers}

class UtilsSpec extends FlatSpec with Matchers {

  implicit val caseClassFormat = cool.graph.JsonFormats.CaseClassFormat
  import spray.json._

  "CaseClassFormat" should "format simple case class" in {
    case class Simple(string: String, int: Int)

    val instance = Simple("a", 1)

    val json = instance.asInstanceOf[Product].toJson.toString
    json should be("""{"string":"a","int":1}""")
  }

  "CaseClassFormat" should "format complex case class" in {
    case class Simple(string: String, int: Int)
    case class Complex(int: Int, simple: Simple)

    val instance = Complex(1, Simple("a", 2))

    val json = instance.asInstanceOf[Product].toJson.toString
    json should be("""{"int":1,"simple":"..."}""")
  }

  "CaseClassFormat" should "format complex case class with id" in {
    case class Simple(id: String, string: String, int: Int)
    case class Complex(int: Int, simple: Simple)

    val instance = Complex(1, Simple("id1", "a", 2))

    val json = instance.asInstanceOf[Product].toJson.toString
    json should be("""{"int":1,"simple":"id1"}""")
  }

}
