package com.prisma.deploy.migration.validation

import com.prisma.shared.models.ConnectorCapability.{EmbeddedScalarListsCapability, NonEmbeddedScalarListCapability}
import com.prisma.shared.models.FieldBehaviour.{ScalarListBehaviour, ScalarListStrategy}
import org.scalatest.{Matchers, WordSpecLike}

class ScalarListDirectiveSpec extends WordSpecLike with Matchers with DataModelValidationSpecBase {
  "should be optional" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  tags: [String]
        |}
      """.stripMargin
    val dataModel = validate(dataModelString, Set(NonEmbeddedScalarListCapability))
    dataModel.type_!("Model").scalarField_!("tags").behaviour should be(Some(ScalarListBehaviour(ScalarListStrategy.Relation)))

    val dataModel2 = validate(dataModelString, Set(EmbeddedScalarListsCapability))
    dataModel2.type_!("Model").scalarField_!("tags").behaviour should be(Some(ScalarListBehaviour(ScalarListStrategy.Embedded)))
  }

  "must fail if an invalid argument is provided" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  tags: [String] @scalarList(strategy: FOOBAR)
        |}
      """.stripMargin

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

  "must fail if the placement is invalid" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  tags: [String!]! @scalarList(strategy: RELATION)
        |  tags2: [String!] @scalarList(strategy: RELATION)
        |  tags3: [String]! @scalarList(strategy: RELATION)
        |  tags4: [[String]] @scalarList(strategy: RELATION)
        |  tags5: [[String]!] @scalarList(strategy: RELATION)
        |}
      """.stripMargin
    val errors = validateThatMustError(dataModelString, Set(NonEmbeddedScalarListCapability))
    errors should have(size(5))
    errors.foreach { error =>
      error.`type` should equal("Model")
      error.description should equal(s"The field `${error.field.get}` has an invalid format. List fields must have the format `[String]`.")
    }
  }

  "must error if a scalar list field has the wrong format" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  tags: [String!]!
        |  tags2: [String!]
        |  tags3: [String]!
        |  tags4: [[String]]
        |  tags5: [[String]!]
        |}
      """.stripMargin
    val errors = validateThatMustError(dataModelString, Set(NonEmbeddedScalarListCapability))
    errors should have(size(5))
    errors.foreach { error =>
      error.`type` should equal("Model")
      error.description should equal(s"The field `${error.field.get}` has an invalid format. List fields must have the format `[String]`.")
    }
  }
}
