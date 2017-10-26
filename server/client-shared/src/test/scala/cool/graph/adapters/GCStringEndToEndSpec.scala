package cool.graph.adapters

import cool.graph.GCDataTypes._
import cool.graph.shared.models.TypeIdentifier
import cool.graph.shared.models.TypeIdentifier.TypeIdentifier
import org.scalatest.{FlatSpec, Matchers}

class GCStringEndToEndSpec extends FlatSpec with Matchers {

  val string    = Some("{\"testValue\": 1}")
  val int       = Some("234")
  val float     = Some("2.234324324")
  val boolean   = Some("true")
  val password  = Some("2424sdfasg234222434sg")
  val id        = Some("2424sdfasg234222434sg")
  val datetime  = Some("2018")
  val datetime2 = Some("2018-01-01T00:00:00.000")

  val enum  = Some("HA")
  val json  = Some("{\"testValue\":1}")
  val json2 = Some("[]")

  val strings    = Some("[\"testValue\",\"testValue\"]")
  val strings2   = Some("[\" s \\\"a\\\" s\"]")
  val ints       = Some("[1,2,3,4]")
  val floats     = Some("[1.23123,2343.2343242]")
  val booleans   = Some("[true,false]")
  val passwords  = Some("[\"totallysafe\",\"totallysafe2\"]")
  val ids        = Some("[\"ctotallywrwqresafe\",\"cwwerwertotallysafe2\"]")
  val datetimes  = Some("[\"2018\",\"2019\"]")
  val datetimes2 = Some("[\"2018-01-01T00:00:00.000\"]")
  val datetimes3 = Some("[]")
  val enums      = Some("[HA,NO]")
  val enums2     = Some("[]")
  val jsons      = Some("[{\"testValue\":1},{\"testValue\":1}]")
  val jsons2     = Some("[]")

  val nullValue: Option[String] = None

  "It should take a String Default or MigrationValue for a non-list field and" should "convert it into Sangria AST and Back" in {
    println("SingleValues")
    forthAndBackOptional(string, TypeIdentifier.String, false) should be(string)
    forthAndBackOptional(int, TypeIdentifier.Int, false) should be(int)
    forthAndBackOptional(float, TypeIdentifier.Float, false) should be(float)
    forthAndBackOptional(boolean, TypeIdentifier.Boolean, false) should be(boolean)
    forthAndBackOptional(password, TypeIdentifier.Password, false) should be(password)
    forthAndBackOptional(id, TypeIdentifier.GraphQLID, false) should be(id)
    forthAndBackOptional(datetime, TypeIdentifier.DateTime, false) should be(Some("2018-01-01T00:00:00.000"))
    forthAndBackOptional(datetime2, TypeIdentifier.DateTime, false) should be(Some("2018-01-01T00:00:00.000"))
    forthAndBackOptional(enum, TypeIdentifier.Enum, false) should be(enum)
    forthAndBackOptional(json, TypeIdentifier.Json, false) should be(json)
    forthAndBackOptional(json2, TypeIdentifier.Json, false) should be(json2)
  }

  "It should take list GCValues and" should "convert them to String and back without loss if the type and list status are correct." in {
    println("ListValues")
    forthAndBackOptional(strings, TypeIdentifier.String, true) should be(strings)
    forthAndBackOptional(strings2, TypeIdentifier.String, true) should be(strings2)
    forthAndBackOptional(ints, TypeIdentifier.Int, true) should be(ints)
    forthAndBackOptional(floats, TypeIdentifier.Float, true) should be(floats)
    forthAndBackOptional(booleans, TypeIdentifier.Boolean, true) should be(booleans)
    forthAndBackOptional(passwords, TypeIdentifier.Password, true) should be(passwords)
    forthAndBackOptional(ids, TypeIdentifier.GraphQLID, true) should be(ids)
    forthAndBackOptional(datetimes, TypeIdentifier.DateTime, true) should be(Some("[\"2018-01-01T00:00:00.000\",\"2019-01-01T00:00:00.000\"]"))
    forthAndBackOptional(datetimes2, TypeIdentifier.DateTime, true) should be(Some("[\"2018-01-01T00:00:00.000\"]"))
    forthAndBackOptional(datetimes3, TypeIdentifier.DateTime, true) should be(Some("[]"))
    forthAndBackOptional(enums, TypeIdentifier.Enum, true) should be(enums)
    forthAndBackOptional(enums2, TypeIdentifier.Enum, true) should be(enums2)
    forthAndBackOptional(jsons, TypeIdentifier.Json, true) should be(jsons)
    forthAndBackOptional(jsons2, TypeIdentifier.Json, true) should be(jsons2)
  }

  "Nullvalue" should "work for every type and cardinality" in {
    println("NullValues")
    forthAndBackOptional(nullValue, TypeIdentifier.String, false) should be(nullValue)
    forthAndBackOptional(nullValue, TypeIdentifier.Int, false) should be(nullValue)
    forthAndBackOptional(nullValue, TypeIdentifier.Float, false) should be(nullValue)
    forthAndBackOptional(nullValue, TypeIdentifier.Boolean, false) should be(nullValue)
    forthAndBackOptional(nullValue, TypeIdentifier.Password, false) should be(nullValue)
    forthAndBackOptional(nullValue, TypeIdentifier.GraphQLID, false) should be(nullValue)
    forthAndBackOptional(nullValue, TypeIdentifier.DateTime, false) should be(nullValue)
    forthAndBackOptional(nullValue, TypeIdentifier.Enum, false) should be(nullValue)
    forthAndBackOptional(nullValue, TypeIdentifier.Json, false) should be(nullValue)
    //    lists
    forthAndBackOptional(nullValue, TypeIdentifier.String, true) should be(nullValue)
    forthAndBackOptional(nullValue, TypeIdentifier.Int, true) should be(nullValue)
    forthAndBackOptional(nullValue, TypeIdentifier.Float, true) should be(nullValue)
    forthAndBackOptional(nullValue, TypeIdentifier.Boolean, true) should be(nullValue)
    forthAndBackOptional(nullValue, TypeIdentifier.Password, true) should be(nullValue)
    forthAndBackOptional(nullValue, TypeIdentifier.GraphQLID, true) should be(nullValue)
    forthAndBackOptional(nullValue, TypeIdentifier.DateTime, true) should be(nullValue)
    forthAndBackOptional(nullValue, TypeIdentifier.Enum, true) should be(nullValue)
    forthAndBackOptional(nullValue, TypeIdentifier.Json, true) should be(nullValue)
  }

  def forthAndBackOptional(input: Option[String], typeIdentifier: TypeIdentifier, isList: Boolean) = {
    val converterString          = GCStringConverter(typeIdentifier, isList)
    var database: Option[String] = None

    val gcValueForth: Option[GCValue] = input.map(x => converterString.toGCValue(x).get)

    database = gcValueForth.flatMap(converterString.fromGCValueToOptionalString)

    val gcValueBack = database.map(x => converterString.toGCValue(x).get)

    val output = gcValueBack.flatMap(converterString.fromGCValueToOptionalString)

    println("IN: " + input + " GCValueForth: " + gcValueForth + " Database: " + database + " GCValueBack: " + gcValueBack + " OUT: " + output)

    output
  }
}
