package com.prisma.deploy.migration.validation

import com.prisma.shared.models.ConnectorCapability.{IdSequenceCapability, IntIdCapability}
import com.prisma.shared.models.FieldBehaviour.{IdBehaviour, IdStrategy, Sequence}
import org.scalatest.{Matchers, WordSpecLike}

class SequenceDirectiveSpec extends WordSpecLike with Matchers with DataModelValidationSpecBase {
  "should be parsed correctly" in {
    val dataModelString =
      """
        |type Model {
        |  id: Int! @id(strategy: SEQUENCE) @sequence(name: "MY_SEQUENCE" initialValue:11 allocationSize:123)
        |}
      """.stripMargin

    val dataModel           = validate(dataModelString, Set(IdSequenceCapability, IntIdCapability))
    val behaviour           = dataModel.type_!("Model").scalarField_!("id").behaviour
    val expectedSequence    = Some(Sequence("MY_SEQUENCE", initialValue = 11, allocationSize = 123))
    val expectedIdBehaviour = Some(IdBehaviour(IdStrategy.Sequence, expectedSequence))
    behaviour should be(expectedIdBehaviour)
  }

  "should error if it is used on fields that are not of type Int" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id(strategy: SEQUENCE) @sequence(name: "MY_SEQUENCE" initialValue:1 allocationSize:100)
        |}
      """.stripMargin

    val errors = validateThatMustError(dataModelString, Set(IdSequenceCapability, IntIdCapability))
    println(errors)
    errors should have(size(1))
    val error = errors.head
    error.`type` should equal("Model")
    error.field should equal(Some("id"))
    error.description should equal(
      "The directive `@sequence` must only be specified for fields that are marked as id, are of type `Int` and use the sequence strategy. E.g. `id: Int! @id(strategy: SEQUENCE)`.")
  }

  "should error if sequences are not supported" in {
    val dataModelString =
      """
        |type Model {
        |  id: Int! @id(strategy: SEQUENCE) @sequence(name: "MY_SEQUENCE" initialValue:1 allocationSize:100)
        |}
      """.stripMargin

    val errors = validateThatMustError(dataModelString, Set(IntIdCapability))
    println(errors)
    errors should have(size(1))
    val error = errors.head
    error.`type` should equal("Model")
    error.field should equal(Some("id"))
    error.description should equal("Valid values for the strategy argument of `@id` are: AUTO, NONE.")
  }

  "should error if it is used on fields that are not marked as id" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  field: String! @sequence(name: "MY_SEQUENCE" initialValue:1 allocationSize:100)
        |}
      """.stripMargin

    val errors = validateThatMustError(dataModelString, Set(IdSequenceCapability))
    errors should have(size(1))
    val error = errors.head
    error.`type` should equal("Model")
    error.field should equal(Some("field"))
    error.description should equal(
      "The directive `@sequence` must only be specified for fields that are marked as id, are of type `Int` and use the sequence strategy. E.g. `id: Int! @id(strategy: SEQUENCE)`.")
  }

  "should error if it is used on fields that are not specifying the SEQUENCE strategy" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id(strategy: NONE) @sequence(name: "MY_SEQUENCE" initialValue:1 allocationSize:100)
        |}
      """.stripMargin

    val errors = validateThatMustError(dataModelString, Set(IdSequenceCapability))
    errors should have(size(1))
    val error = errors.head
    error.`type` should equal("Model")
    error.field should equal(Some("id"))
    error.description should equal(
      "The directive `@sequence` must only be specified for fields that are marked as id, are of type `Int` and use the sequence strategy. E.g. `id: Int! @id(strategy: SEQUENCE)`.")
  }
}
