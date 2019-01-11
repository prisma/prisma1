package com.prisma.deploy.migration.validation

import com.prisma.shared.models.ConnectorCapability.EmbeddedTypesCapability
import org.scalatest.{Matchers, WordSpecLike}

class EmbeddedDirectiveSpec extends WordSpecLike with Matchers with DataModelValidationSpecBase {

  "should error if embedded types are not supported" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  other: Other
        |}
        |type Other @embedded {
        |  text: String
        |}
      """.stripMargin
    val errors = validateThatMustError(dataModelString)
    errors should have(size(1))
    val error = errors.head
    error.`type` should equal("Other")
    error.field should be(None)
    error.description should be("The type `Other` is marked as embedded but this connector does not support embedded types.")
  }

  "should succeed if embedded types are supported" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  other: Other
        |}
        |type Other @embedded {
        |  text: String
        |}
      """.stripMargin
    val dataModel = validate(dataModelString, Set(EmbeddedTypesCapability))
    dataModel.type_!("Other").isEmbedded should be(true)
    dataModel.type_!("Model").isEmbedded should be(false)
  }
}
