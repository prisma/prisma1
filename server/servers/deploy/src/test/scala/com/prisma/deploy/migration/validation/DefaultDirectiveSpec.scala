package com.prisma.deploy.migration.validation

import com.prisma.gc_values.{EnumGCValue, StringGCValue}
import org.scalatest.{Matchers, WordSpecLike}

class DefaultDirectiveSpec extends WordSpecLike with Matchers with DataModelValidationSpecBase {
  "@default should work" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  field: String! @default(value: "my_value")
        |}
      """.stripMargin

    val dataModel = validate(dataModelString)
    val field     = dataModel.type_!("Model").scalarField_!("field")
    field.defaultValue should be(Some(StringGCValue("my_value")))
  }

  "@default should work for enum fields" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  field: Status! @default(value: B)
        |}
        |
        |enum Status {
        |  A,
        |  B
        |}
      """.stripMargin

    val dataModel = validate(dataModelString)
    val field     = dataModel.type_!("Model").enumField_!("field")
    field.defaultValue should be(Some(EnumGCValue("B")))
  }

  "@default should error if the provided value does not match the field type" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  field: String! @default(value: true)
        |}
      """.stripMargin

    val error = validateThatMustError(dataModelString).head
    error.`type` should equal("Model")
    error.field should equal(Some("field"))
    error.description should include("The value true is not a valid default for fields of type String.")
  }

  "@default should error if the provided value does not match the field type in the case of enums" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  field: Status! @default(value: X)
        |}
        |
        |enum Status {
        |  A,
        |  B
        |}
      """.stripMargin

    val error = validateThatMustError(dataModelString).head
    error.`type` should equal("Model")
    error.field should equal(Some("field"))
    error.description should include("The default value is invalid for this enum. Valid values are: A, B.")
  }
}
