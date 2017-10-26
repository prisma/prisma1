package cool.graph.adapters

import cool.graph.GCDataTypes._
import cool.graph.shared.models.TypeIdentifier
import cool.graph.shared.models.TypeIdentifier.TypeIdentifier
import org.scalatest.{FlatSpec, Matchers}
import sangria.ast.{AstNode, Value}
import spray.json.JsValue

class GCDBValueEndToEndSpec extends FlatSpec with Matchers {

  val string   = "{\"testValue\": 1}"
  val int      = "234"
  val float    = "2.234324324"
  val boolean  = "true"
  val password = "2424sdfasg234222434sg"
  val id       = "2424sdfasg234222434sg"
  val datetime = "2018"
  val enum     = "HA"
  val json     = "{\"testValue\":1}"

  val strings   = "[\"testValue\", \"testValue\"]"
  val ints      = "[1, 2, 3, 4]"
  val floats    = "[1.23123, 2343.2343242]"
  val booleans  = "[true, false]"
  val passwords = "[\"totallysafe\", \"totallysafe2\"]"
  val ids       = "[\"ctotallywrwqresafe\", \"cwwerwertotallysafe2\"]"
  val datetimes = "[\"2018\", \"2019\"]"
  val enums     = "[HA, NO]"
  val jsons     = "[{\"testValue\":1},{\"testValue\":1}]"

  val nullValue = "null"

  // Work in Progress

//  "It should take a String Default or MigrationValue for a non-list field and" should "convert it into Sangria AST and Back" in {
//    forthAndBack(string, TypeIdentifier.String, false) should be(Result.Equal)
//    forthAndBack(int, TypeIdentifier.Int, false) should be(Result.Equal)
//    forthAndBack(float, TypeIdentifier.Float, false) should be(Result.Equal)
//    forthAndBack(boolean, TypeIdentifier.Boolean, false) should be(Result.Equal)
//    forthAndBack(password, TypeIdentifier.Password, false) should be(Result.Equal)
//    forthAndBack(id, TypeIdentifier.GraphQLID, false) should be(Result.Equal)
////    forthAndBack(datetime, TypeIdentifier.DateTime, false) should be(Result.Equal)
//    forthAndBack(enum, TypeIdentifier.Enum, false) should be(Result.Equal)
//    forthAndBack(json, TypeIdentifier.Json, false) should be(Result.Equal)
//  }
//
//  "It should take list GCValues and" should "convert them to String and back without loss if the type and list status are correct." in {
//
//    forthAndBack(strings, TypeIdentifier.String, true) should be(Result.Equal)
//    forthAndBack(ints, TypeIdentifier.Int, true) should be(Result.Equal)
//    forthAndBack(floats, TypeIdentifier.Float, true) should be(Result.Equal)
//    forthAndBack(booleans, TypeIdentifier.Boolean, true) should be(Result.Equal)
//    forthAndBack(passwords, TypeIdentifier.Password, true) should be(Result.Equal)
//    forthAndBack(ids, TypeIdentifier.GraphQLID, true) should be(Result.Equal)
//    //forthAndBack(datetimes, TypeIdentifier.DateTime, true) should be(Result.Equal)
//    forthAndBack(enums, TypeIdentifier.Enum, true) should be(Result.Equal)
//    forthAndBack(jsons, TypeIdentifier.Json, true) should be(Result.Equal)
//  }
//
//  "Nullvalue" should "work for every type and cardinality" in {
//    forthAndBack(nullValue, TypeIdentifier.String, false) should be(Result.Equal)
//    forthAndBack(nullValue, TypeIdentifier.Int, false) should be(Result.Equal)
//    forthAndBack(nullValue, TypeIdentifier.Float, false) should be(Result.Equal)
//    forthAndBack(nullValue, TypeIdentifier.Boolean, false) should be(Result.Equal)
//    forthAndBack(nullValue, TypeIdentifier.Password, false) should be(Result.Equal)
//    forthAndBack(nullValue, TypeIdentifier.GraphQLID, false) should be(Result.Equal)
//    forthAndBack(nullValue, TypeIdentifier.DateTime, false) should be(Result.Equal)
//    forthAndBack(nullValue, TypeIdentifier.Enum, false) should be(Result.Equal)
//    forthAndBack(nullValue, TypeIdentifier.Json, false) should be(Result.Equal)
//    //    lists
//    forthAndBack(nullValue, TypeIdentifier.String, true) should be(Result.Equal)
//    forthAndBack(nullValue, TypeIdentifier.Int, true) should be(Result.Equal)
//    forthAndBack(nullValue, TypeIdentifier.Float, true) should be(Result.Equal)
//    forthAndBack(nullValue, TypeIdentifier.Boolean, true) should be(Result.Equal)
//    forthAndBack(nullValue, TypeIdentifier.Password, true) should be(Result.Equal)
//    forthAndBack(nullValue, TypeIdentifier.GraphQLID, true) should be(Result.Equal)
//    forthAndBack(nullValue, TypeIdentifier.DateTime, true) should be(Result.Equal)
//    forthAndBack(nullValue, TypeIdentifier.Enum, true) should be(Result.Equal)
//    forthAndBack(nullValue, TypeIdentifier.Json, true) should be(Result.Equal)
//  }
//
//  def forthAndBack(input: String, typeIdentifier: TypeIdentifier, isList: Boolean) = {
//    val converterStringSangria  = StringSangriaValueConverter(typeIdentifier, isList)
//    val converterSangriaGCValue = GCSangriaValueConverter(typeIdentifier, isList)
//    val converterDBValueGCValue = GCDBValueConverter(typeIdentifier, isList)
//
//    val stringInput = input
//    //String to SangriaValue
//    val sangriaValueForth: Value = converterStringSangria.from(input).get
//
//    //SangriaValue to GCValue
//    val gcValueForth: GCValue = converterSangriaGCValue.from(sangriaValueForth).get
//
//    //GCValue to DBValue
//    val dbString: JsValue = converterDBValueGCValue.to(gcValueForth)
//
//    //DBValue to GCValue
//    val gcValueBack: GCValue = converterDBValueGCValue.from(dbString).get
//
//    //GCValue to SangriaValue
//    val sangriaValueBack: Value = converterSangriaGCValue.to(gcValueBack)
//    println(sangriaValueBack)
//
//    //SangriaValue to String
//    val stringOutput: String = converterStringSangria.to(sangriaValueBack)
//
//    println(s"In: |$stringInput| Out: |$stringOutput|")
//    if (stringInput != stringOutput) {
//      sys.error(s"In was: |$stringInput| but out was: |$stringOutput|")
//    }
//    if (stringInput == stringOutput) Result.Equal else Result.NotEqual
//
//  }
//
//  object Result extends Enumeration {
//    val Equal, BadError, NotEqual = Value
//  }

}
