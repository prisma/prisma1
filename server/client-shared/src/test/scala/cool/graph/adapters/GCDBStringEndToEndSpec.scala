package cool.graph.adapters

import cool.graph.GCDataTypes._
import cool.graph.shared.models.TypeIdentifier
import cool.graph.shared.models.TypeIdentifier.TypeIdentifier
import org.scalactic.{Bad, Good}
import org.scalatest.{FlatSpec, Matchers}

class GCDBStringEndToEndSpec extends FlatSpec with Matchers {

  val string   = "{\"testValue\": 1}"
  val int      = "234"
  val float    = "2.234324324"
  val boolean  = "true"
  val password = "2424sdfasg234222434sg"
  val id       = "2424sdfasg234222434sg"
  val datetime = "2018"
  val enum     = "HA"
  val json     = "{\"testValue\":1}"
  val json2    = "[]"

  val strings    = "[\"testValue\",\"testValue\"]"
  val ints       = "[1,2,3,4]"
  val ints2      = "[]"
  val floats     = "[1.23123,2343.2343242]"
  val booleans   = "[true,false]"
  val passwords  = "[\"totallysafe\",\"totallysafe2\"]"
  val ids        = "[\"ctotallywrwqresafe\",\"cwwerwertotallysafe2\"]"
  val datetimes  = "[\"2018\",\"2019\"]"
  val datetimes2 = "[]"
  val enums      = "[HA,NO]"
  val jsons      = "[{\"testValue\":1},{\"testValue\":1}]"
  val jsons2     = "[]"

  val nullValue = "null"

  "It should take a String Default or MigrationValue for a non-list field and" should "convert it to a DBString and Back" in {
    println("Single Values")
    forthAndBack(string, TypeIdentifier.String, false) should be(string)
    forthAndBack(int, TypeIdentifier.Int, false) should be(int)
    forthAndBack(float, TypeIdentifier.Float, false) should be(float)
    forthAndBack(boolean, TypeIdentifier.Boolean, false) should be(boolean)
    forthAndBack(password, TypeIdentifier.Password, false) should be(password)
    forthAndBack(id, TypeIdentifier.GraphQLID, false) should be(id)
    forthAndBack(datetime, TypeIdentifier.DateTime, false) should be("2018-01-01T00:00:00.000")
    forthAndBack(enum, TypeIdentifier.Enum, false) should be(enum)
    forthAndBack(json, TypeIdentifier.Json, false) should be(json)
    forthAndBack(json2, TypeIdentifier.Json, false) should be(json2)

  }

  "It should take list String DefaultValue and" should "convert them to DBString and back without loss if the type and list status are correct." in {
    println("List Values")
    forthAndBack(strings, TypeIdentifier.String, true) should be(strings)
    forthAndBack(ints, TypeIdentifier.Int, true) should be(ints)
    forthAndBack(ints2, TypeIdentifier.Int, true) should be(ints2)
    forthAndBack(floats, TypeIdentifier.Float, true) should be(floats)
    forthAndBack(booleans, TypeIdentifier.Boolean, true) should be(booleans)
    forthAndBack(passwords, TypeIdentifier.Password, true) should be(passwords)
    forthAndBack(ids, TypeIdentifier.GraphQLID, true) should be(ids)
    forthAndBack(datetimes, TypeIdentifier.DateTime, true) should be("[\"2018-01-01T00:00:00.000\",\"2019-01-01T00:00:00.000\"]")
    forthAndBack(datetimes2, TypeIdentifier.DateTime, true) should be(datetimes2)
    forthAndBack(enums, TypeIdentifier.Enum, true) should be(enums)
    forthAndBack(jsons, TypeIdentifier.Json, true) should be(jsons)
    forthAndBack(jsons2, TypeIdentifier.Json, true) should be(jsons2) // Todo this has wrong GCValues in transition

  }

  "Nullvalue" should "work for every type and cardinality" in {
    println("Null Values")
    forthAndBack(nullValue, TypeIdentifier.String, false) should be(nullValue)
    forthAndBack(nullValue, TypeIdentifier.Int, false) should be(nullValue)
    forthAndBack(nullValue, TypeIdentifier.Float, false) should be(nullValue)
    forthAndBack(nullValue, TypeIdentifier.Boolean, false) should be(nullValue)
    forthAndBack(nullValue, TypeIdentifier.Password, false) should be(nullValue)
    forthAndBack(nullValue, TypeIdentifier.GraphQLID, false) should be(nullValue)
    forthAndBack(nullValue, TypeIdentifier.DateTime, false) should be(nullValue)
    forthAndBack(nullValue, TypeIdentifier.Enum, false) should be(nullValue)
    forthAndBack(nullValue, TypeIdentifier.Json, false) should be(nullValue)
    //    lists
    forthAndBack(nullValue, TypeIdentifier.String, true) should be(nullValue)
    forthAndBack(nullValue, TypeIdentifier.Int, true) should be(nullValue)
    forthAndBack(nullValue, TypeIdentifier.Float, true) should be(nullValue)
    forthAndBack(nullValue, TypeIdentifier.Boolean, true) should be(nullValue)
    forthAndBack(nullValue, TypeIdentifier.Password, true) should be(nullValue)
    forthAndBack(nullValue, TypeIdentifier.GraphQLID, true) should be(nullValue)
    forthAndBack(nullValue, TypeIdentifier.DateTime, true) should be(nullValue)
    forthAndBack(nullValue, TypeIdentifier.Enum, true) should be(nullValue)
    forthAndBack(nullValue, TypeIdentifier.Json, true) should be(nullValue)
  }

  def forthAndBack(input: String, typeIdentifier: TypeIdentifier, isList: Boolean) = {
    val converterStringSangria   = StringSangriaValueConverter(typeIdentifier, isList)
    val converterSangriaGCValue  = GCSangriaValueConverter(typeIdentifier, isList)
    val converterStringDBGCValue = GCStringDBConverter(typeIdentifier, isList)

    val stringInput = input
    //String to SangriaValue
    val sangriaValueForth = converterStringSangria.from(input)

    //SangriaValue to GCValue
    val gcValueForth = converterSangriaGCValue.toGCValue(sangriaValueForth.get)

    //GCValue to DBString
    val dbString = converterStringDBGCValue.fromGCValue(gcValueForth.get)

    //DBString to GCValue
    val gcValueBack = converterStringDBGCValue.toGCValueCanReadOldAndNewFormat(dbString)

    //GCValue to SangriaValue
    val sangriaValueBack = converterSangriaGCValue.fromGCValue(gcValueBack.get)

    //SangriaValue to String
    val stringOutput = converterStringSangria.to(sangriaValueBack)

    println("In: " + stringInput + " GCForth: " + gcValueForth + " DBString: " + dbString + " GCValueBack: " + gcValueBack + " Out: " + stringOutput)

    stringOutput
  }

}
