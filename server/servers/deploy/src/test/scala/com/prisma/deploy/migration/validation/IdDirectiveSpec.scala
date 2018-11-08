package com.prisma.deploy.migration.validation

import com.prisma.shared.models.FieldBehaviour.{IdBehaviour, IdStrategy}
import org.scalatest.{Matchers, WordSpecLike}

class IdDirectiveSpec extends WordSpecLike with Matchers with DataModelValidationSpecBase {
  "@id without explicit strategy should work" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |}
      """.stripMargin
    val dataModel = validate(dataModelString)
    dataModel.type_!("Model").scalarField_!("id").behaviour should be(Some(IdBehaviour(IdStrategy.Auto)))
  }

  "@id should work with explicit default strategy" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id(strategy: AUTO)
        |}
      """.stripMargin

    val dataModel = validate(dataModelString)
    dataModel.type_!("Model").scalarField_!("id").behaviour should be(Some(IdBehaviour(IdStrategy.Auto)))
  }

  "@id should work with NONE strategy" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id(strategy: NONE)
        |}
      """.stripMargin
    val dataModel = validate(dataModelString)
    dataModel.type_!("Model").scalarField_!("id").behaviour should be(Some(IdBehaviour(IdStrategy.None)))
  }

  "@id should error when an unknown strategy is used" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id(strategy: FOO)
        |}
      """.stripMargin
    val error = validateThatMustError(dataModelString).head
    error.`type` should equal("Model")
    error.field should equal(Some("id"))
    error.description should equal("Valid values for the strategy argument of `@id` are: AUTO, NONE.")
  }

  "@id should error on embedded types" in {
    val dataModelString =
      """
        |type Model @embedded {
        |  id: ID! @id(strategy: NONE)
        |}
      """.stripMargin
    val error = validateThatMustError(dataModelString).head
    error.`type` should equal("Model")
    error.field should equal(Some("id"))
    error.description should equal("The `@id` directive is not allowed on embedded types.")
  }

  "a model type without @id should error" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID!
        |}
      """.stripMargin
    val error = validateThatMustError(dataModelString).head
    error.`type` should equal("Model")
    error.field should equal(None)
    error.description should equal("One field of the type `Model` must be marked as the id field with the `@id` directive.")
  }
}
