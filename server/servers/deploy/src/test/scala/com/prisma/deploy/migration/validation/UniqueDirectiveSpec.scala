package com.prisma.deploy.migration.validation

import com.prisma.shared.models.ConnectorCapability.EmbeddedTypesCapability
import com.prisma.shared.models.FieldBehaviour.{IdBehaviour, IdStrategy}
import org.scalatest.{Matchers, WordSpecLike}

class UniqueDirectiveSpec extends WordSpecLike with Matchers with DataModelValidationSpecBase {
  "it should work" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  name: String! @unique
        |}
      """.stripMargin
    val dataModel = validate(dataModelString)
    dataModel.type_!("Model").scalarField_!("name").isUnique should be(true)
  }

  "should error when used on embedded types" in {
    val dataModelString =
      """
        |type Model @embedded {
        |  name: String! @unique
        |}
      """.stripMargin
    val errors = validateThatMustError(dataModelString, Set(EmbeddedTypesCapability))
    errors should have(size(1))
    val error = errors.head
    println(error)
    error.`type` should be("Model")
    error.field should be(Some("name"))
    error.description should be("The field `name` is marked as unique but its type `Model` is embedded. This is disallowed.")
  }
}
