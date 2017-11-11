package cool.graph.adapters

import cool.graph.GCDataTypes._
import cool.graph.shared.models.TypeIdentifier
import cool.graph.shared.models.TypeIdentifier.TypeIdentifier
import org.scalactic.{Bad, Good}
import org.scalatest.{FlatSpec, Matchers}

class StringSangriaValuesConverterSpec extends FlatSpec with Matchers {

  val string   = "{\"testValue\": 1}"
  val int      = "234"
  val float    = "2.234324324"
  val boolean  = "true"
  val password = "2424sdfasg234222434sg"
  val id       = "2424sdfasg234222434sg"
  val datetime = "2018"
  val enum     = "HA"
  val json     = "{\"testValue\": 1}"
  val json2    = "[]"

  val strings   = "[\"testValue\",\"testValue\"]"
  val ints      = "[1,2,3,4]"
  val floats    = "[1.23123,2343.2343242]"
  val booleans  = "[true,false]"
  val passwords = "[\"totallysafe\",\"totallysafe2\"]"
  val ids       = "[\"ctotallywrwqresafe\",\"cwwerwertotallysafe2\"]"
  val datetimes = "[\"2018\",\"2019\"]"
  val enums     = "[HA,NO]"
  val jsons     = "[{\"testValue\":1},{\"testValue\":1}]"
  val jsons2    = "[]"

  val nullValue = "null"

  "It should take a String Default or MigrationValue for a non-list field and" should "convert it into Sangria AST and Back" in {
    println("SingleValues")
    forthAndBack(string, TypeIdentifier.String, false) should be(Result.Equal)
    forthAndBack(int, TypeIdentifier.Int, false) should be(Result.Equal)
    forthAndBack(float, TypeIdentifier.Float, false) should be(Result.Equal)
    forthAndBack(boolean, TypeIdentifier.Boolean, false) should be(Result.Equal)
    forthAndBack(password, TypeIdentifier.Password, false) should be(Result.Equal)
    forthAndBack(id, TypeIdentifier.GraphQLID, false) should be(Result.Equal)
    forthAndBack(datetime, TypeIdentifier.DateTime, false) should be(Result.Equal)
    forthAndBack(enum, TypeIdentifier.Enum, false) should be(Result.Equal)
    forthAndBack(json, TypeIdentifier.Json, false) should be(Result.Equal)
    forthAndBack(json2, TypeIdentifier.Json, false) should be(Result.Equal)

  }

  "It should take list GCValues and" should "convert them to String and back without loss if the type and list status are correct." in {
    println("ListValues")
    forthAndBack(strings, TypeIdentifier.String, true) should be(Result.Equal)
    forthAndBack(ints, TypeIdentifier.Int, true) should be(Result.Equal)
    forthAndBack(floats, TypeIdentifier.Float, true) should be(Result.Equal)
    forthAndBack(booleans, TypeIdentifier.Boolean, true) should be(Result.Equal)
    forthAndBack(passwords, TypeIdentifier.Password, true) should be(Result.Equal)
    forthAndBack(ids, TypeIdentifier.GraphQLID, true) should be(Result.Equal)
    forthAndBack(datetimes, TypeIdentifier.DateTime, true) should be(Result.Equal)
    forthAndBack(enums, TypeIdentifier.Enum, true) should be(Result.Equal)
    forthAndBack(jsons, TypeIdentifier.Json, true) should be(Result.Equal)
    forthAndBack(jsons2, TypeIdentifier.Json, true) should be(Result.Equal)

  }

  "Nullvalue" should "work for every type and cardinality" in {
    println("NullValues")
    forthAndBack(nullValue, TypeIdentifier.String, false) should be(Result.Equal)
    forthAndBack(nullValue, TypeIdentifier.Int, false) should be(Result.Equal)
    forthAndBack(nullValue, TypeIdentifier.Float, false) should be(Result.Equal)
    forthAndBack(nullValue, TypeIdentifier.Boolean, false) should be(Result.Equal)
    forthAndBack(nullValue, TypeIdentifier.Password, false) should be(Result.Equal)
    forthAndBack(nullValue, TypeIdentifier.GraphQLID, false) should be(Result.Equal)
    forthAndBack(nullValue, TypeIdentifier.DateTime, false) should be(Result.Equal)
    forthAndBack(nullValue, TypeIdentifier.Enum, false) should be(Result.Equal)
    forthAndBack(nullValue, TypeIdentifier.Json, false) should be(Result.Equal)
    //    lists
    forthAndBack(nullValue, TypeIdentifier.String, true) should be(Result.Equal)
    forthAndBack(nullValue, TypeIdentifier.Int, true) should be(Result.Equal)
    forthAndBack(nullValue, TypeIdentifier.Float, true) should be(Result.Equal)
    forthAndBack(nullValue, TypeIdentifier.Boolean, true) should be(Result.Equal)
    forthAndBack(nullValue, TypeIdentifier.Password, true) should be(Result.Equal)
    forthAndBack(nullValue, TypeIdentifier.GraphQLID, true) should be(Result.Equal)
    forthAndBack(nullValue, TypeIdentifier.DateTime, true) should be(Result.Equal)
    forthAndBack(nullValue, TypeIdentifier.Enum, true) should be(Result.Equal)
    forthAndBack(nullValue, TypeIdentifier.Json, true) should be(Result.Equal)
  }

  def forthAndBack(input: String, typeIdentifier: TypeIdentifier, isList: Boolean) = {
    val converter = StringSangriaValueConverter(typeIdentifier, isList)
    val forth     = converter.fromAbleToHandleJsonLists(input)
    forth match {
      case Bad(error) =>
        Result.BadError

      case Good(x) =>
        val forthAndBack = converter.to(x)
        println("IN: " + input + " SangriaValue: " + forth + " OUT: " + forthAndBack)

        if (forthAndBack == input) Result.Equal else Result.NotEqual
    }
  }

  object Result extends Enumeration {
    val Equal, BadError, NotEqual = Value
  }

}
