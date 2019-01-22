package com.prisma.deploy.migration.validation

import com.prisma.shared.models.ConnectorCapability.{EmbeddedTypesCapability, RelationLinkListCapability}
import org.scalatest.{Matchers, WordSpecLike}

class DbDirectiveSpec extends WordSpecLike with Matchers with DataModelValidationSpecBase {
  "it should work on fields" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  field: String @db(name: "some_column")
        |}
      """.stripMargin

    val dataModel = validate(dataModelString)
    val field     = dataModel.type_!("Model").scalarField_!("field")
    field.columnName should be(Some("some_column"))
  }

  "it must error on relation fields" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  other: Other @db(name: "some_column")
        |}
        |
        |type Other {
        |  id: ID! @id
        |}
      """.stripMargin
    val errors = validateThatMustError(dataModelString, Set(EmbeddedTypesCapability, RelationLinkListCapability))
    errors should have(size(1))
    val error = errors.head
    error.`type` should be("Model")
    error.description should be("The field `other` specifies the `@db` directive. Relation fields must not specify this directive.")
    error.field should be(Some("other"))
  }

  "it must nor error on inline relation fields" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  other: Other @db(name: "some_column") @relation(link: INLINE)
        |}
        |
        |type Other {
        |  id: ID! @id
        |}
      """.stripMargin

    val dataModel = validate(dataModelString)
    val field     = dataModel.type_!("Model").relationField_!("other")
    field.columnName should be(Some("some_column"))
  }

  "it should work on types" in {
    val dataModelString =
      """
        |type Model @db(name:"some_table") {
        |  id: ID! @id
        |  field: String
        |}
      """.stripMargin

    val dataModel = validate(dataModelString)
    val model     = dataModel.type_!("Model")
    model.tableName should equal(Some("some_table"))
  }

  "it must not be valid on embedded types" in {
    val dataModelString =
      """
        |type Model @db(name:"some_table") @embedded {
        |  field: String
        |}
      """.stripMargin
    val errors = validateThatMustError(dataModelString, Set(EmbeddedTypesCapability))
    errors should have(size(1))
    val error = errors.head
    error.`type` should be("Model")
    error.description should be("The type `Model` is specifies the `@db` directive. Embedded types must not specify this directive.")
    error.field should be(None)
  }
}
