package com.prisma.deploy.migration.validation

import com.prisma.shared.models.FieldBehaviour.{CreatedAtBehaviour, UpdatedAtBehaviour}
import org.scalatest.{Matchers, WordSpecLike}

class CreatedAndUpdatedAtDirectiveSpec extends WordSpecLike with Matchers with DataModelValidationSpecBase {
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
    error.description should equal("Fields that are marked as `@createdAt` must be of type `DateTime!` or `DateTime`.")
  }

  "@createdAt should not error if the field is not required" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  createdAt: DateTime @createdAt
        |}
      """.stripMargin
    val dataModel = validate(dataModelString)
    dataModel.type_!("Model").scalarField_!("createdAt").behaviour should be(Some(CreatedAtBehaviour))
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
    error.description should equal("Fields that are marked as @updatedAt must be of type `DateTime!` or `DateTime`.")
  }

  "@updatedAt should not error if the field is not required" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  updatedAt: DateTime @updatedAt
        |}
      """.stripMargin
    val dataModel = validate(dataModelString)
    dataModel.type_!("Model").scalarField_!("updatedAt").behaviour should be(Some(UpdatedAtBehaviour))
  }

  "must error if @createdAt and @updatedAt are used simultaneously" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  field: DateTime! @updatedAt @createdAt
        |}
      """.stripMargin
    val error = validateThatMustError(dataModelString).head
    error.`type` should equal("Model")
    error.field should equal(Some("field"))
    error.description should equal("Fields cannot be marked simultaneously with `@createdAt` and `@updatedAt`.")
  }
}
