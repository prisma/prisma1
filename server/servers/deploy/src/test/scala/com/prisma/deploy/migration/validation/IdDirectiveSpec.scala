package com.prisma.deploy.migration.validation

import com.prisma.shared.models.ConnectorCapability.{EmbeddedTypesCapability, IntIdCapability, UuidIdCapability}
import com.prisma.shared.models.FieldBehaviour.{IdBehaviour, IdStrategy}
import com.prisma.shared.models.TypeIdentifier
import org.scalatest.{Matchers, WordSpecLike}

class IdDirectiveSpec extends WordSpecLike with Matchers with DataModelValidationSpecBase {
  "without explicit strategy should work" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |}
      """.stripMargin
    val dataModel = validate(dataModelString)
    dataModel.type_!("Model").scalarField_!("id").behaviour should be(Some(IdBehaviour(IdStrategy.Auto)))
  }

  "should work with explicit default strategy" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id(strategy: AUTO)
        |}
      """.stripMargin

    val dataModel = validate(dataModelString)
    dataModel.type_!("Model").scalarField_!("id").behaviour should be(Some(IdBehaviour(IdStrategy.Auto)))
  }

  "should work with NONE strategy" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id(strategy: NONE)
        |}
      """.stripMargin
    val dataModel = validate(dataModelString)
    dataModel.type_!("Model").scalarField_!("id").behaviour should be(Some(IdBehaviour(IdStrategy.None)))
  }

  "should error if the field is not required" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID @id
        |}
      """.stripMargin
    val error = validateThatMustError(dataModelString).head
    error.`type` should equal("Model")
    error.field should equal(Some("id"))
    error.description should equal("Fields that are marked as id must be required.")
  }

  "should error when an unknown strategy is used" in {
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

//  "should error on embedded types" in {
//    val dataModelString =
//      """
//        |type Model @embedded {
//        |  id: ID! @id(strategy: NONE)
//        |}
//      """.stripMargin
//    val error = validateThatMustError(dataModelString, Set(EmbeddedTypesCapability)).head
//    error.`type` should equal("Model")
//    error.field should equal(Some("id"))
//    error.description should equal("The `@id` directive is not allowed on embedded types.")
//  }

  "should not error on embedded types" in {
    val dataModelString =
      """
        |type Model @embedded {
        |  id: ID! @id
        |}
      """.stripMargin
    val dataModel = validate(dataModelString, Set(EmbeddedTypesCapability))
    dataModel.type_!("Model").isEmbedded should be(true)
    dataModel.type_!("Model").scalarField_!("id").behaviour should be(Some(IdBehaviour(IdStrategy.Auto)))
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

  "should work on Int fields if this is supported" in {
    val dataModelString =
      """
        |type Model {
        |  id: Int! @id
        |}
      """.stripMargin
    val dataModel = validate(dataModelString, Set(IntIdCapability))
    dataModel.type_!("Model").scalarField_!("id").typeIdentifier should be(TypeIdentifier.Int)
  }

  "should error on Int fields if this is NOT supported" in {
    val dataModelString =
      """
        |type Model {
        |  id: Int! @id
        |}
      """.stripMargin
    val error = validateThatMustError(dataModelString).head
    error.`type` should equal("Model")
    error.field should equal(Some("id"))
    error.description should equal("The field `id` is marked as id must have one of the following types: `ID!`.")
  }

  "should work on UUID fields if this is supported" in {
    val dataModelString =
      """
        |type Model {
        |  id: UUID! @id
        |}
      """.stripMargin
    val dataModel = validate(dataModelString, Set(UuidIdCapability))
    dataModel.type_!("Model").scalarField_!("id").typeIdentifier should be(TypeIdentifier.UUID)
  }

  "should error UUID Int fields if this is NOT supported" in {
    val dataModelString =
      """
        |type Model {
        |  id: UUID! @id
        |}
      """.stripMargin
    val error = validateThatMustError(dataModelString, Set(IntIdCapability)).head
    error.`type` should equal("Model")
    error.field should equal(Some("id"))
    error.description should equal("The field `id` is marked as id must have one of the following types: `ID!`,`Int!`.")
  }
}
