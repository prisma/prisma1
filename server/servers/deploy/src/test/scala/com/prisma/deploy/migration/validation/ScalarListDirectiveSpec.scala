package com.prisma.deploy.migration.validation

import com.prisma.shared.models.ConnectorCapability.{EmbeddedScalarListsCapability, NonEmbeddedScalarListCapability}
import com.prisma.shared.models.FieldBehaviour.{ScalarListBehaviour, ScalarListStrategy}
import org.scalatest.{Matchers, WordSpecLike}

class ScalarListDirectiveSpec extends WordSpecLike with Matchers with DataModelValidationSpecBase {
  "should be optional if the embedded scalar lists are supported" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  tags: [String]
        |}
      """.stripMargin
    val dataModel2 = validate(dataModelString, Set(EmbeddedScalarListsCapability))
    dataModel2.type_!("Model").scalarField_!("tags").behaviour should be(Some(ScalarListBehaviour(ScalarListStrategy.Embedded)))

    val errors = validateThatMustError(dataModelString, Set(NonEmbeddedScalarListCapability))
    errors should have(size(1))
    val error = errors.head
    error.`type` should be("Model")
    error.field should be(Some("tags"))
    error.description should be("Valid values for the strategy argument of `@scalarList` are: RELATION.")
  }

  "must fail if scalar lists are not supported at all" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  tags: [String]
        |}
      """.stripMargin

    val errors = validateThatMustError(dataModelString)
    errors should have(size(1))
    val error = errors.head
    error.`type` should be("Model")
    error.field should be(Some("tags"))
    error.description should be("This connector does not support scalar lists.")
  }

  "must fail if an invalid argument is provided" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  tags: [String] @scalarList(strategy: FOOBAR)
        |}
      """.stripMargin

    println(validateThatMustError(dataModelString, Set(EmbeddedScalarListsCapability, NonEmbeddedScalarListCapability)))
    val error = validateThatMustError(dataModelString, Set(EmbeddedScalarListsCapability, NonEmbeddedScalarListCapability)).head
    error.`type` should equal("Model")
    error.field should equal(Some("tags"))
    error.description should equal("Valid values for the strategy argument of `@scalarList` are: EMBEDDED, RELATION.")

    val error2 = validateThatMustError(dataModelString, Set(NonEmbeddedScalarListCapability)).head
    error2.`type` should equal("Model")
    error2.field should equal(Some("tags"))
    error2.description should equal("Valid values for the strategy argument of `@scalarList` are: RELATION.")

    val error3 = validateThatMustError(dataModelString, Set(EmbeddedScalarListsCapability)).head
    error3.`type` should equal("Model")
    error3.field should equal(Some("tags"))
    error3.description should equal("Valid values for the strategy argument of `@scalarList` are: EMBEDDED.")
  }

  "Enum Lists must not fail" in {
    val dataModelString =
      """
        |enum UsedEnum {
        |    A,
        |    B
        |}
        |
        |type AWithId {
        |    id: ID! @id
        |    fieldA: UsedEnum
        |    fieldB: UsedEnum!
        |    fieldC: [UsedEnum]
        |}
      """.stripMargin

    validate(dataModelString, Set(EmbeddedScalarListsCapability))
  }
}
