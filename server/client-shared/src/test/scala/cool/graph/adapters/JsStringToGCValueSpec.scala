package cool.graph.adapters

import cool.graph.GCDataTypes._
import cool.graph.shared.SchemaSerializer.CaseClassFormats._
import cool.graph.shared.models.Field
import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.{FlatSpec, Matchers}
import spray.json._

class JsStringToGCValueSpec extends FlatSpec with Matchers {

  "The SchemaSerializer" should "be able to parse the old and the new format for Enums" in {

    val fieldOld = """{
                  |            "typeIdentifier": "Enum",
                  |            "isSystem": false,
                  |            "name": "canceledPeriods",
                  |            "isReadonly": false,
                  |            "relation": null,
                  |            "isList": true,
                  |            "isUnique": false,
                  |            "isRequired": false,
                  |            "description": null,
                  |            "id": "cj5glw5r630kq0127ocb46v88",
                  |            "enum": null,
                  |            "constraints": [],
                  |            "defaultValue": "[HA]",
                  |            "relationSide": null
                  |          }""".stripMargin.parseJson

    fieldOld.convertTo[Field].defaultValue.get should be(ListGCValue(Vector(EnumGCValue("HA"))))

    val fieldNew = """{
                     |            "typeIdentifier": "Enum",
                     |            "isSystem": false,
                     |            "name": "canceledPeriods",
                     |            "isReadonly": false,
                     |            "relation": null,
                     |            "isList": true,
                     |            "isUnique": false,
                     |            "isRequired": false,
                     |            "description": null,
                     |            "id": "cj5glw5r630kq0127ocb46v88",
                     |            "enum": null,
                     |            "constraints": [],
                     |            "defaultValue": ["HA"],
                     |            "relationSide": null
                     |          }""".stripMargin.parseJson

    fieldNew.convertTo[Field].defaultValue.get should be(ListGCValue(Vector(EnumGCValue("HA"))))
  }

  "The SchemaSerializer" should "be able to parse the old and the new format for String" in {

    val fieldOld = """{
                     |            "typeIdentifier": "String",
                     |            "isSystem": false,
                     |            "name": "canceledPeriods",
                     |            "isReadonly": false,
                     |            "relation": null,
                     |            "isList": true,
                     |            "isUnique": false,
                     |            "isRequired": false,
                     |            "description": null,
                     |            "id": "cj5glw5r630kq0127ocb46v88",
                     |            "enum": null,
                     |            "constraints": [],
                     |            "defaultValue": "[\"HALLO, SIE\"]",
                     |            "relationSide": null
                     |          }""".stripMargin.parseJson

    fieldOld.convertTo[Field].defaultValue.get should be(ListGCValue(Vector(StringGCValue("HALLO, SIE"))))

    val fieldNew = """{
                     |            "typeIdentifier": "String",
                     |            "isSystem": false,
                     |            "name": "canceledPeriods",
                     |            "isReadonly": false,
                     |            "relation": null,
                     |            "isList": true,
                     |            "isUnique": false,
                     |            "isRequired": false,
                     |            "description": null,
                     |            "id": "cj5glw5r630kq0127ocb46v88",
                     |            "enum": null,
                     |            "constraints": [],
                     |            "defaultValue": ["HALLO, SIE"],
                     |            "relationSide": null
                     |          }""".stripMargin.parseJson

    fieldNew.convertTo[Field].defaultValue.get should be(ListGCValue(Vector(StringGCValue("HALLO, SIE"))))
  }

  "The SchemaSerializer" should "be able to parse the old and the new format for Json" in {

    val fieldOld = """{
                     |            "typeIdentifier": "Json",
                     |            "isSystem": false,
                     |            "name": "canceledPeriods",
                     |            "isReadonly": false,
                     |            "relation": null,
                     |            "isList": true,
                     |            "isUnique": false,
                     |            "isRequired": false,
                     |            "description": null,
                     |            "id": "cj5glw5r630kq0127ocb46v88",
                     |            "enum": null,
                     |            "constraints": [],
                     |            "defaultValue": "[{\"a\":2},{\"a\":2}]",
                     |            "relationSide": null
                     |          }""".stripMargin.parseJson

    fieldOld.convertTo[Field].defaultValue.get should be(
      ListGCValue(Vector(JsonGCValue(JsObject("a" -> JsNumber(2))), JsonGCValue(JsObject("a" -> JsNumber(2))))))

    val fieldNew = """{
                     |            "typeIdentifier": "Json",
                     |            "isSystem": false,
                     |            "name": "canceledPeriods",
                     |            "isReadonly": false,
                     |            "relation": null,
                     |            "isList": true,
                     |            "isUnique": false,
                     |            "isRequired": false,
                     |            "description": null,
                     |            "id": "cj5glw5r630kq0127ocb46v88",
                     |            "enum": null,
                     |            "constraints": [],
                     |            "defaultValue": [{"a":2},{"a":2}],
                     |            "relationSide": null
                     |          }""".stripMargin.parseJson

    fieldNew.convertTo[Field].defaultValue.get should be(
      ListGCValue(Vector(JsonGCValue(JsObject("a" -> JsNumber(2))), JsonGCValue(JsObject("a" -> JsNumber(2))))))
  }

  "The SchemaSerializer" should "be able to parse the old and the new format for DateTime" in {

    val fieldOld = """{
                     |            "typeIdentifier": "DateTime",
                     |            "isSystem": false,
                     |            "name": "canceledPeriods",
                     |            "isReadonly": false,
                     |            "relation": null,
                     |            "isList": true,
                     |            "isUnique": false,
                     |            "isRequired": false,
                     |            "description": null,
                     |            "id": "cj5glw5r630kq0127ocb46v88",
                     |            "enum": null,
                     |            "constraints": [],
                     |            "defaultValue": "[\"2018\", \"2019\"]",
                     |            "relationSide": null
                     |          }""".stripMargin.parseJson

    fieldOld.convertTo[Field].defaultValue.get should be(
      ListGCValue(Vector(DateTimeGCValue(new DateTime("2018", DateTimeZone.UTC)), DateTimeGCValue(new DateTime("2019", DateTimeZone.UTC)))))

    val fieldNew = """{
                     |            "typeIdentifier": "DateTime",
                     |            "isSystem": false,
                     |            "name": "canceledPeriods",
                     |            "isReadonly": false,
                     |            "relation": null,
                     |            "isList": true,
                     |            "isUnique": false,
                     |            "isRequired": false,
                     |            "description": null,
                     |            "id": "cj5glw5r630kq0127ocb46v88",
                     |            "enum": null,
                     |            "constraints": [],
                     |            "defaultValue": ["2018-01-01T00:00:00.000Z", "2019-01-01T00:00:00.000Z"],
                     |            "relationSide": null
                     |          }""".stripMargin.parseJson

    val res = fieldNew.convertTo[Field].defaultValue.get

    println(res)

    res should be(ListGCValue(Vector(DateTimeGCValue(new DateTime("2018", DateTimeZone.UTC)), DateTimeGCValue(new DateTime("2019", DateTimeZone.UTC)))))
  }

  "The SchemaSerializer" should "be able to parse the old and the new format for Boolean" in {

    val fieldOld = """{
                     |            "typeIdentifier": "Boolean",
                     |            "isSystem": false,
                     |            "name": "canceledPeriods",
                     |            "isReadonly": false,
                     |            "relation": null,
                     |            "isList": true,
                     |            "isUnique": false,
                     |            "isRequired": false,
                     |            "description": null,
                     |            "id": "cj5glw5r630kq0127ocb46v88",
                     |            "enum": null,
                     |            "constraints": [],
                     |            "defaultValue": "[true, false]",
                     |            "relationSide": null
                     |          }""".stripMargin.parseJson

    fieldOld.convertTo[Field].defaultValue.get should be(ListGCValue(Vector(BooleanGCValue(true), BooleanGCValue(false))))

    val fieldNew = """{
                     |            "typeIdentifier": "Boolean",
                     |            "isSystem": false,
                     |            "name": "canceledPeriods",
                     |            "isReadonly": false,
                     |            "relation": null,
                     |            "isList": true,
                     |            "isUnique": false,
                     |            "isRequired": false,
                     |            "description": null,
                     |            "id": "cj5glw5r630kq0127ocb46v88",
                     |            "enum": null,
                     |            "constraints": [],
                     |            "defaultValue": [true, false],
                     |            "relationSide": null
                     |          }""".stripMargin.parseJson

    val res = fieldNew.convertTo[Field].defaultValue.get
    res should be(ListGCValue(Vector(BooleanGCValue(true), BooleanGCValue(false))))
  }

  "The SchemaSerializer" should "be able to parse the old and the new format for Float" in {

    val fieldOld = """{
                     |            "typeIdentifier": "Float",
                     |            "isSystem": false,
                     |            "name": "canceledPeriods",
                     |            "isReadonly": false,
                     |            "relation": null,
                     |            "isList": true,
                     |            "isUnique": false,
                     |            "isRequired": false,
                     |            "description": null,
                     |            "id": "cj5glw5r630kq0127ocb46v88",
                     |            "enum": null,
                     |            "constraints": [],
                     |            "defaultValue": "1.234",
                     |            "relationSide": null
                     |          }""".stripMargin.parseJson

    fieldOld.convertTo[Field].defaultValue.get should be(FloatGCValue(1.234))

    val fieldNew = """{
                     |            "typeIdentifier": "Float",
                     |            "isSystem": false,
                     |            "name": "canceledPeriods",
                     |            "isReadonly": false,
                     |            "relation": null,
                     |            "isList": true,
                     |            "isUnique": false,
                     |            "isRequired": false,
                     |            "description": null,
                     |            "id": "cj5glw5r630kq0127ocb46v88",
                     |            "enum": null,
                     |            "constraints": [],
                     |            "defaultValue": 1.234,
                     |            "relationSide": null
                     |          }""".stripMargin.parseJson

    val res = fieldNew.convertTo[Field].defaultValue.get
    res should be(FloatGCValue(1.234))
  }

  "The SchemaSerializer" should "be able to parse the old and the new format for Floats that are 0" in {

    val fieldOld = """{
                     |            "typeIdentifier": "Float",
                     |            "isSystem": false,
                     |            "name": "canceledPeriods",
                     |            "isReadonly": false,
                     |            "relation": null,
                     |            "isList": false,
                     |            "isUnique": false,
                     |            "isRequired": false,
                     |            "description": null,
                     |            "id": "cj5glw5r630kq0127ocb46v88",
                     |            "enum": null,
                     |            "constraints": [],
                     |            "defaultValue": "0",
                     |            "relationSide": null
                     |          }""".stripMargin.parseJson

    fieldOld.convertTo[Field].defaultValue.get should be(FloatGCValue(0))

    val fieldNew = """{
                     |            "typeIdentifier": "Float",
                     |            "isSystem": false,
                     |            "name": "canceledPeriods",
                     |            "isReadonly": false,
                     |            "relation": null,
                     |            "isList": false,
                     |            "isUnique": false,
                     |            "isRequired": false,
                     |            "description": null,
                     |            "id": "cj5glw5r630kq0127ocb46v88",
                     |            "enum": null,
                     |            "constraints": [],
                     |            "defaultValue": 0,
                     |            "relationSide": null
                     |          }""".stripMargin.parseJson

    val res = fieldNew.convertTo[Field].defaultValue.get
    res should be(FloatGCValue(0))
  }

  "The SchemaSerializer" should "be able to parse the old and the new format for Floats that are ints" in {

    val fieldOld = """{
                     |            "typeIdentifier": "Float",
                     |            "isSystem": false,
                     |            "name": "canceledPeriods",
                     |            "isReadonly": false,
                     |            "relation": null,
                     |            "isList": false,
                     |            "isUnique": false,
                     |            "isRequired": false,
                     |            "description": null,
                     |            "id": "cj5glw5r630kq0127ocb46v88",
                     |            "enum": null,
                     |            "constraints": [],
                     |            "defaultValue": "10",
                     |            "relationSide": null
                     |          }""".stripMargin.parseJson

    fieldOld.convertTo[Field].defaultValue.get should be(FloatGCValue(10))

    val fieldNew = """{
                     |            "typeIdentifier": "Float",
                     |            "isSystem": false,
                     |            "name": "canceledPeriods",
                     |            "isReadonly": false,
                     |            "relation": null,
                     |            "isList": false,
                     |            "isUnique": false,
                     |            "isRequired": false,
                     |            "description": null,
                     |            "id": "cj5glw5r630kq0127ocb46v88",
                     |            "enum": null,
                     |            "constraints": [],
                     |            "defaultValue": 1,
                     |            "relationSide": null
                     |          }""".stripMargin.parseJson

    val res = fieldNew.convertTo[Field].defaultValue.get
    res should be(FloatGCValue(1))
  }
}
