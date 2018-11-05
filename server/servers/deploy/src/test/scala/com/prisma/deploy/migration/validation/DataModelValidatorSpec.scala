package com.prisma.deploy.migration.validation

import com.prisma.deploy.connector.FieldRequirementsInterface
import com.prisma.deploy.specutils.DeploySpecBase
import com.prisma.shared.models.FieldBehaviour.{CreatedAtBehaviour, IdBehaviour, IdStrategy, UpdatedAtBehaviour}
import org.scalactic.{Bad, Good, Or}
import org.scalatest.{Matchers, WordSpecLike}

class DataModelValidatorSpec extends WordSpecLike with Matchers with DeploySpecBase {
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

  "a type without @id should error" in {
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

  "@createdAt should be detected" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  myCreatedAt: DateTime! @createdAt
        |}
      """.stripMargin
    val dataModel = validate(dataModelString)
    dataModel.type_!("Model").scalarField_!("myCreatedAt").behaviour should be(Some(CreatedAtBehaviour))
  }

  "@createdAt should error if the type of the field is not correct" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  createdAt: String! @createdAt
        |}
      """.stripMargin
    val error = validateThatMustError(dataModelString).head
    error.`type` should equal("Model")
    error.field should equal(Some("createdAt"))
    error.description should equal("Fields that are marked as @createdAt must be of type `DateTime!`.")
  }

  "@createdAt should error if the type of the field is not required" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  createdAt: DateTime @createdAt
        |}
      """.stripMargin
    val error = validateThatMustError(dataModelString).head
    error.`type` should equal("Model")
    error.field should equal(Some("createdAt"))
    error.description should equal("Fields that are marked as @createdAt must be of type `DateTime!`.")
  }

  "@updatedAt should be detected" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  myUpdatedAt: DateTime! @updatedAt
        |}
      """.stripMargin
    val dataModel = validate(dataModelString)
    dataModel.type_!("Model").scalarField_!("myUpdatedAt").behaviour should be(Some(UpdatedAtBehaviour))
  }

  "@updatedAt should error if the type of the field is not correct" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  updatedAt: String! @updatedAt
        |}
      """.stripMargin
    val error = validateThatMustError(dataModelString).head
    error.`type` should equal("Model")
    error.field should equal(Some("updatedAt"))
    error.description should equal("Fields that are marked as @updatedAt must be of type `DateTime!`.")
  }

  "@updatedAt should error if the type of the field is not required" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  updatedAt: DateTime @updatedAt
        |}
      """.stripMargin
    val error = validateThatMustError(dataModelString).head
    error.`type` should equal("Model")
    error.field should equal(Some("updatedAt"))
    error.description should equal("Fields that are marked as @updatedAt must be of type `DateTime!`.")
  }

  "@db should work" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  field: String @db(name: "some_columns")
        |}
      """.stripMargin

    val dataModel = validate(dataModelString)
    val field     = dataModel.type_!("Model").scalarField_!("field")
    field.columnName should be(Some("some_columns"))
  }

  def validateThatMustError(dataModel: String): Vector[DeployError] = {
    val result = validateInternal(dataModel)
    result match {
      case Good(dm)    => sys.error("The validation did not produce an error, which was expected.")
      case Bad(errors) => errors
    }
  }

  def validate(dataModel: String) = {
    val result = validateInternal(dataModel)
    result match {
      case Good(dm) => dm
      case Bad(errors) =>
        sys.error {
          s"""The validation returned the following unexpected errors:
          |   ${errors.mkString("\n")}
        """.stripMargin
        }
    }
  }

  def validateInternal(dataModel: String): Or[PrismaSdl, Vector[DeployError]] = {
    val requirements = new FieldRequirementsInterface {
      override val requiredReservedFields    = Vector.empty
      override val hiddenReservedField       = Vector.empty
      override val reservedFieldRequirements = Vector.empty
      override val isAutogenerated           = false
    }
    DataModelValidatorImpl.validate(dataModel, requirements, Set.empty)
  }
}
