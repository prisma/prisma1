package com.prisma.deploy.migration.validation

import com.prisma.shared.models.ConnectorCapability
import com.prisma.shared.models.ConnectorCapability.{IntIdCapability, RelationLinkTableCapability}
import org.scalatest.{Matchers, WordSpecLike}

class RelationTableDirectiveSpec extends WordSpecLike with Matchers with DataModelValidationSpecBase {
  "it must be parsed correctly" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  model: Model @relation(name: "ModelToModelRelation")
        |}
        |
        |type ModelToModelRelation @relationTable {
        |  A: Model!
        |  B: Model!
        |}
      """.stripMargin

    val dataModel         = validate(dataModelString, Set(RelationLinkTableCapability))
    val relationTableType = dataModel.type_!("ModelToModelRelation")
    relationTableType.isRelationTable should be(true)
  }

  "should error if link tables are not supported" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  model: Model @relation(name: "ModelToModelRelation")
        |}
        |
        |type ModelToModelRelation @relationTable {
        |  A: Model!
        |  B: Model!
        |}
      """.stripMargin

    val errors = validateThatMustError(dataModelString)
    errors should have(size(1))
    val error = errors.head
    error.`type` should be("ModelToModelRelation")
    error.field should be(None)
    error.description should be("The directive `@relationTable` is not supported by this connector.")
  }

  "should error if the name of the link table is not referred to from any relation" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  model: Model
        |}
        |
        |type MyLinkTable @relationTable {
        |  A: Model!
        |  B: Model!
        |}
      """.stripMargin

    val errors = validateThatMustError(dataModelString, Set(RelationLinkTableCapability))
    errors should have(size(1))
    val error = errors.head
    error.`type` should be("MyLinkTable")
    error.field should be(None)
    error.description should be("The link table `MyLinkTable` is not referenced in any relation field.")
  }

  "should error if it is referred to in a relation field" ignore {
    // TODO: due to our other checks it seems impossible to actually do this error
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  field: [ModelToModelRelation] @relation(name: "ModelToModelRelation")
        |}
        |
        |type Other {
        |  id: ID! @id
        |  model: Model @relation(name: "ModelToModelRelation")
        |}
        |
        |type ModelToModelRelation @relationTable {
        |  A: Model!
        |  B: Model!
        |}
      """.stripMargin

    val errors = validateThatMustError(dataModelString, Set(RelationLinkTableCapability))
    println(errors)
    errors should have(size(1))
    val error = errors.head
    error.`type` should be("Model")
    error.field should be(Some("field"))
    error.description should be("asjdfklsjdf")
  }

  "should error if the link table does not refer to the right types" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  other: Other @relation(name: "MyRelation", link: INLINE)
        |}
        |
        |type Other {
        |  id: ID! @id
        |  model: Model @relation(name: "MyRelation")
        |}
        |
        |type MyRelation @relationTable {
        |  A: Model!
        |  B: Model!
        |}
      """.stripMargin

    val errors = validateThatMustError(dataModelString, Set(RelationLinkTableCapability))
    println(errors)
    errors should have(size(1))
    val error = errors.head
    error.`type` should be("MyRelation")
    error.field should be(None)
    error.description should be("The link table `MyRelation` is not referencing the right types.")
  }

  "should error if the link table provides superfluous scalar fields" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  model: Model @relation(name: "ModelToModelRelation")
        |}
        |
        |type ModelToModelRelation @relationTable {
        |  A: Model!
        |  B: Model!
        |  field: Int!
        |}
      """.stripMargin

    val errors = validateThatMustError(dataModelString, Set(RelationLinkTableCapability))
    errors should have(size(1))
    val error = errors.head
    error.`type` should be("ModelToModelRelation")
    error.field should be(Some("field"))
    error.description should be("A link table must not specify any additional scalar fields.")
  }

  "should error if the link table provides superfluous relation fields" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  model: Model @relation(name: "ModelToModelRelation")
        |}
        |
        |type ModelToModelRelation @relationTable {
        |  A: Model!
        |  B: Model!
        |  C: Model!
        |}
      """.stripMargin

    val errors = validateThatMustError(dataModelString, Set(RelationLinkTableCapability))
    errors should have(size(1))
    val error = errors.head
    error.`type` should be("ModelToModelRelation")
    error.field should be(None)
    error.description should be("A link table must specify exactly two relation fields.")
  }

  "should succeed for legacy style relation tables" in {
    val capas: Set[ConnectorCapability] = Set(RelationLinkTableCapability)
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  model: Model @relation(name: "ModelToModelRelation")
        |}
        |
        |type ModelToModelRelation @relationTable {
        |  id: ID! @id
        |  A: Model!
        |  B: Model!
        |}
      """.stripMargin

    val dataModel         = validate(dataModelString, capas)
    val relationTableType = dataModel.type_!("ModelToModelRelation")
    relationTableType.isRelationTable should be(true)

    val warnings = validateThatMustWarn(dataModelString, capas)
    warnings should have(size(1))
    val warning = warnings.head
    warning.`type` should be("ModelToModelRelation")
    warning.field should be(Some("id"))
    warning.description should be(
      "Id fields on link tables are deprecated and will soon loose support. Please remove it from your datamodel to remove the underlying column.")
  }

  "should error for if the id field does not have the ID type" in {
    val capas: Set[ConnectorCapability] = Set(RelationLinkTableCapability, IntIdCapability)
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  model: Model @relation(name: "ModelToModelRelation")
        |}
        |
        |type ModelToModelRelation @relationTable {
        |  id: Int! @id
        |  A: Model!
        |  B: Model!
        |}
      """.stripMargin

    val errors = validateThatMustError(dataModelString, capas)
    errors should have(size(1))
    val error = errors.head
    error.`type` should be("ModelToModelRelation")
    error.field should be(Some("id"))
    error.description should be("The id field of a link table must be of type `ID!`.")
  }
}
