package cool.graph.util.gcvalueconverters

import cool.graph.gc_values._
import cool.graph.shared.models.Field
import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.{JsNumber, JsObject, Json}
import cool.graph.shared.models.ProjectJsonFormatter._

class JsStringToGCValueSpec extends FlatSpec with Matchers {

  //the JsonFormatter can currently not read the defaultValue since it is defined as an GCValue on field


//  "The SchemaSerializer" should "be able to parse the old and the new format for Enums" in {
//
//    val fieldOld = Json.parse("""{
//                  |            "typeIdentifier": "Enum",
//                  |            "isSystem": false,
//                  |            "name": "canceledPeriods",
//                  |            "isReadonly": false,
//                  |            "relation": null,
//                  |            "isList": true,
//                  |            "isUnique": false,
//                  |            "isRequired": false,
//                  |            "description": null,
//                  |            "id": "cj5glw5r630kq0127ocb46v88",
//                  |            "enum": null,
//                  |            "constraints": [],
//                  |            "defaultValue": "[HA]",
//                  |            "relationSide": null,
//                  |            "isHidden": false
//                  |          }""".stripMargin)
//
//    fieldOld.as[Field].defaultValue.get should be(ListGCValue(Vector(EnumGCValue("HA"))))
//
//    val fieldNew = Json.parse("""{
//                     |            "typeIdentifier": "Enum",
//                     |            "isSystem": false,
//                     |            "name": "canceledPeriods",
//                     |            "isReadonly": false,
//                     |            "relation": null,
//                     |            "isList": true,
//                     |            "isUnique": false,
//                     |            "isRequired": false,
//                     |            "description": null,
//                     |            "id": "cj5glw5r630kq0127ocb46v88",
//                     |            "enum": null,
//                     |            "constraints": [],
//                     |            "defaultValue": ["HA"],
//                     |            "relationSide": null
//                     |          }""".stripMargin)
//
//    fieldNew.as[Field].defaultValue.get should be(ListGCValue(Vector(EnumGCValue("HA"))))
//  }
//
//  "The SchemaSerializer" should "be able to parse the old and the new format for String" in {
//
//    val fieldOld = Json.parse("""{
//                     |            "typeIdentifier": "String",
//                     |            "isSystem": false,
//                     |            "name": "canceledPeriods",
//                     |            "isReadonly": false,
//                     |            "relation": null,
//                     |            "isList": true,
//                     |            "isUnique": false,
//                     |            "isRequired": false,
//                     |            "description": null,
//                     |            "id": "cj5glw5r630kq0127ocb46v88",
//                     |            "enum": null,
//                     |            "constraints": [],
//                     |            "defaultValue": "[\"HALLO, SIE\"]",
//                     |            "relationSide": null
//                     |          }""".stripMargin)
//
//    fieldOld.as[Field].defaultValue.get should be(ListGCValue(Vector(StringGCValue("HALLO, SIE"))))
//
//    val fieldNew = Json.parse("""{
//                     |            "typeIdentifier": "String",
//                     |            "isSystem": false,
//                     |            "name": "canceledPeriods",
//                     |            "isReadonly": false,
//                     |            "relation": null,
//                     |            "isList": true,
//                     |            "isUnique": false,
//                     |            "isRequired": false,
//                     |            "description": null,
//                     |            "id": "cj5glw5r630kq0127ocb46v88",
//                     |            "enum": null,
//                     |            "constraints": [],
//                     |            "defaultValue": ["HALLO, SIE"],
//                     |            "relationSide": null
//                     |          }""".stripMargin)
//
//    fieldNew.as[Field].defaultValue.get should be(ListGCValue(Vector(StringGCValue("HALLO, SIE"))))
//  }
//
//  "The SchemaSerializer" should "be able to parse the old and the new format for Json" in {
//
//    val fieldOld = Json.parse("""{
//                     |            "typeIdentifier": "Json",
//                     |            "isSystem": false,
//                     |            "name": "canceledPeriods",
//                     |            "isReadonly": false,
//                     |            "relation": null,
//                     |            "isList": true,
//                     |            "isUnique": false,
//                     |            "isRequired": false,
//                     |            "description": null,
//                     |            "id": "cj5glw5r630kq0127ocb46v88",
//                     |            "enum": null,
//                     |            "constraints": [],
//                     |            "defaultValue": "[{\"a\":2},{\"a\":2}]",
//                     |            "relationSide": null
//                     |          }""".stripMargin)
//
//    fieldOld.as[Field].defaultValue.get should be(
//      ListGCValue(Vector(JsonGCValue(JsObject(Seq(("a", JsNumber(2))))), JsonGCValue(JsObject(Seq(("a", JsNumber(2))))))))
//
//    val fieldNew = Json.parse("""{
//                     |            "typeIdentifier": "Json",
//                     |            "isSystem": false,
//                     |            "name": "canceledPeriods",
//                     |            "isReadonly": false,
//                     |            "relation": null,
//                     |            "isList": true,
//                     |            "isUnique": false,
//                     |            "isRequired": false,
//                     |            "description": null,
//                     |            "id": "cj5glw5r630kq0127ocb46v88",
//                     |            "enum": null,
//                     |            "constraints": [],
//                     |            "defaultValue": [{"a":2},{"a":2}],
//                     |            "relationSide": null
//                     |          }""".stripMargin)
//
//    fieldNew.as[Field].defaultValue.get should be(
//      ListGCValue(Vector(JsonGCValue(JsObject(Seq(("a", JsNumber(2))))), JsonGCValue(JsObject(Seq(("a", JsNumber(2))))))))
//  }
//
//  "The SchemaSerializer" should "be able to parse the old and the new format for DateTime" in {
//
//    val fieldOld = Json.parse("""{
//                     |            "typeIdentifier": "DateTime",
//                     |            "isSystem": false,
//                     |            "name": "canceledPeriods",
//                     |            "isReadonly": false,
//                     |            "relation": null,
//                     |            "isList": true,
//                     |            "isUnique": false,
//                     |            "isRequired": false,
//                     |            "description": null,
//                     |            "id": "cj5glw5r630kq0127ocb46v88",
//                     |            "enum": null,
//                     |            "constraints": [],
//                     |            "defaultValue": "[\"2018\", \"2019\"]",
//                     |            "relationSide": null
//                     |          }""".stripMargin)
//
//    fieldOld.as[Field].defaultValue.get should be(
//      ListGCValue(Vector(DateTimeGCValue(new DateTime("2018", DateTimeZone.UTC)), DateTimeGCValue(new DateTime("2019", DateTimeZone.UTC)))))
//
//    val fieldNew = Json.parse("""{
//                     |            "typeIdentifier": "DateTime",
//                     |            "isSystem": false,
//                     |            "name": "canceledPeriods",
//                     |            "isReadonly": false,
//                     |            "relation": null,
//                     |            "isList": true,
//                     |            "isUnique": false,
//                     |            "isRequired": false,
//                     |            "description": null,
//                     |            "id": "cj5glw5r630kq0127ocb46v88",
//                     |            "enum": null,
//                     |            "constraints": [],
//                     |            "defaultValue": ["2018-01-01T00:00:00.000Z", "2019-01-01T00:00:00.000Z"],
//                     |            "relationSide": null
//                     |          }""".stripMargin)
//
//    val res = fieldNew.as[Field].defaultValue.get
//
//    println(res)
//
//    res should be(ListGCValue(Vector(DateTimeGCValue(new DateTime("2018", DateTimeZone.UTC)), DateTimeGCValue(new DateTime("2019", DateTimeZone.UTC)))))
//  }
//
//  "The SchemaSerializer" should "be able to parse the old and the new format for Boolean" in {
//
//    val fieldOld = Json.parse("""{
//                     |            "typeIdentifier": "Boolean",
//                     |            "isSystem": false,
//                     |            "name": "canceledPeriods",
//                     |            "isReadonly": false,
//                     |            "relation": null,
//                     |            "isList": true,
//                     |            "isUnique": false,
//                     |            "isRequired": false,
//                     |            "description": null,
//                     |            "id": "cj5glw5r630kq0127ocb46v88",
//                     |            "enum": null,
//                     |            "constraints": [],
//                     |            "defaultValue": "[true, false]",
//                     |            "relationSide": null
//                     |          }""".stripMargin)
//
//    fieldOld.as[Field].defaultValue.get should be(ListGCValue(Vector(BooleanGCValue(true), BooleanGCValue(false))))
//
//    val fieldNew = Json.parse("""{
//                     |            "typeIdentifier": "Boolean",
//                     |            "isSystem": false,
//                     |            "name": "canceledPeriods",
//                     |            "isReadonly": false,
//                     |            "relation": null,
//                     |            "isList": true,
//                     |            "isUnique": false,
//                     |            "isRequired": false,
//                     |            "description": null,
//                     |            "id": "cj5glw5r630kq0127ocb46v88",
//                     |            "enum": null,
//                     |            "constraints": [],
//                     |            "defaultValue": [true, false],
//                     |            "relationSide": null
//                     |          }""".stripMargin)
//
//    val res = fieldNew.as[Field].defaultValue.get
//    res should be(ListGCValue(Vector(BooleanGCValue(true), BooleanGCValue(false))))
//  }
//
//  "The SchemaSerializer" should "be able to parse the old and the new format for Float" in {
//
//    val fieldOld = Json.parse("""{
//                     |            "typeIdentifier": "Float",
//                     |            "isSystem": false,
//                     |            "name": "canceledPeriods",
//                     |            "isReadonly": false,
//                     |            "relation": null,
//                     |            "isList": true,
//                     |            "isUnique": false,
//                     |            "isRequired": false,
//                     |            "description": null,
//                     |            "id": "cj5glw5r630kq0127ocb46v88",
//                     |            "enum": null,
//                     |            "constraints": [],
//                     |            "defaultValue": "1.234",
//                     |            "relationSide": null
//                     |          }""".stripMargin)
//
//    fieldOld.as[Field].defaultValue.get should be(FloatGCValue(1.234))
//
//    val fieldNew = Json.parse("""{
//                     |            "typeIdentifier": "Float",
//                     |            "isSystem": false,
//                     |            "name": "canceledPeriods",
//                     |            "isReadonly": false,
//                     |            "relation": null,
//                     |            "isList": true,
//                     |            "isUnique": false,
//                     |            "isRequired": false,
//                     |            "description": null,
//                     |            "id": "cj5glw5r630kq0127ocb46v88",
//                     |            "enum": null,
//                     |            "constraints": [],
//                     |            "defaultValue": 1.234,
//                     |            "relationSide": null
//                     |          }""".stripMargin)
//
//    val res = fieldNew.as[Field].defaultValue.get
//    res should be(FloatGCValue(1.234))
//  }
//
//  "The SchemaSerializer" should "be able to parse the old and the new format for Floats that are 0" in {
//
//    val fieldOld = Json.parse("""{
//                     |            "typeIdentifier": "Float",
//                     |            "isSystem": false,
//                     |            "name": "canceledPeriods",
//                     |            "isReadonly": false,
//                     |            "relation": null,
//                     |            "isList": false,
//                     |            "isUnique": false,
//                     |            "isRequired": false,
//                     |            "description": null,
//                     |            "id": "cj5glw5r630kq0127ocb46v88",
//                     |            "enum": null,
//                     |            "constraints": [],
//                     |            "defaultValue": "0",
//                     |            "relationSide": null
//                     |          }""".stripMargin)
//
//    fieldOld.as[Field].defaultValue.get should be(FloatGCValue(0))
//
//    val fieldNew = Json.parse("""{
//                     |            "typeIdentifier": "Float",
//                     |            "isSystem": false,
//                     |            "name": "canceledPeriods",
//                     |            "isReadonly": false,
//                     |            "relation": null,
//                     |            "isList": false,
//                     |            "isUnique": false,
//                     |            "isRequired": false,
//                     |            "description": null,
//                     |            "id": "cj5glw5r630kq0127ocb46v88",
//                     |            "enum": null,
//                     |            "constraints": [],
//                     |            "defaultValue": 0,
//                     |            "relationSide": null
//                     |          }""".stripMargin)
//
//    val res = fieldNew.as[Field].defaultValue.get
//    res should be(FloatGCValue(0))
//  }
//
//  "The SchemaSerializer" should "be able to parse the old and the new format for Floats that are ints" in {
//
//    val fieldOld = Json.parse("""{
//                     |            "typeIdentifier": "Float",
//                     |            "isSystem": false,
//                     |            "name": "canceledPeriods",
//                     |            "isReadonly": false,
//                     |            "relation": null,
//                     |            "isList": false,
//                     |            "isUnique": false,
//                     |            "isRequired": false,
//                     |            "description": null,
//                     |            "id": "cj5glw5r630kq0127ocb46v88",
//                     |            "enum": null,
//                     |            "constraints": [],
//                     |            "defaultValue": "10",
//                     |            "relationSide": null
//                     |          }""".stripMargin)
//
//    fieldOld.as[Field].defaultValue.get should be(FloatGCValue(10))
//
//    val fieldNew = Json.parse("""{
//                     |            "typeIdentifier": "Float",
//                     |            "isSystem": false,
//                     |            "name": "canceledPeriods",
//                     |            "isReadonly": false,
//                     |            "relation": null,
//                     |            "isList": false,
//                     |            "isUnique": false,
//                     |            "isRequired": false,
//                     |            "description": null,
//                     |            "id": "cj5glw5r630kq0127ocb46v88",
//                     |            "enum": null,
//                     |            "constraints": [],
//                     |            "defaultValue": 1,
//                     |            "relationSide": null
//                     |          }""".stripMargin)
//
//    val res = fieldNew.as[Field].defaultValue.get
//    res should be(FloatGCValue(1))
//  }
}
