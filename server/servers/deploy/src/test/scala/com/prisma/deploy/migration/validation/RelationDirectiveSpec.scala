package com.prisma.deploy.migration.validation

import com.prisma.shared.models.ApiConnectorCapability.MongoRelationsCapability
import com.prisma.shared.models.{OnDelete, RelationStrategy}
import org.scalatest.{Matchers, WordSpecLike}

class RelationDirectiveSpec extends WordSpecLike with Matchers with DataModelValidationSpecBase {
  "@relation settings must be detected" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  model: Model @relation(name: "MyRelation", strategy: EMBED, onDelete: CASCADE)
        |}
      """.stripMargin

    val dataModel = validate(dataModelString)
    val field     = dataModel.type_!("Model").relationField_!("model")
    field.relationName should equal(Some("MyRelation"))
    field.cascade should equal(OnDelete.Cascade)
    field.strategy should equal(RelationStrategy.Embed)
  }

  "@relation must be optional" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  model: Model
        |}
      """.stripMargin

    val dataModel = validate(dataModelString)
    val field     = dataModel.type_!("Model").relationField_!("model")
    field.relationName should equal(None)
    field.cascade should equal(OnDelete.SetNull)
    field.strategy should equal(RelationStrategy.Auto)
  }

  "@relation strategy must be required between non-embedded types in Mongo" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  other: Other
        |}
        |
        |type Other {
        |  id: ID! @id
        |  model: Model
        |}
      """.stripMargin

    val errors = validateThatMustError(dataModelString, Set(MongoRelationsCapability))
    errors should have(size(2))
    val (error1, error2) = (errors.head, errors(1))
    error1.`type` should equal("Model")
    error1.field should equal(Some("other"))
    error1.description should equal("The field `other` must provide a relation strategy.")

    error2.`type` should equal("Other")
    error2.field should equal(Some("model"))
    error2.description should equal("The field `model` must provide a relation strategy.")
  }

  "@relation strategy must be optional if an embedded type is involved in Mongo" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  other: Other
        |}
        |
        |type Other @embedded {
        |  text: String
        |}
      """.stripMargin

    val dataModel = validate(dataModelString)

  }

  "@relation strategy must be required for one-to-one relations in SQL" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  other: Other
        |}
        |
        |type Other {
        |  id: ID! @id
        |  model: Model
        |}
      """.stripMargin

    val errors = validateThatMustError(dataModelString, Set())
    errors should have(size(2))
    val (error1, error2) = (errors.head, errors(1))
    error1.`type` should equal("Model")
    error1.field should equal(Some("other"))
    error1.description should equal("The field `other` must provide a relation strategy.")

    error2.`type` should equal("Other")
    error2.field should equal(Some("model"))
    error2.description should equal("The field `model` must provide a relation strategy.")
  }

  "@relation strategy must be optional for one-to-many and many-to-many relations in SQL" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  other: [Other!]!
        |  other2: [Other2!]!
        |}
        |
        |type Other {
        |  id: ID! @id
        |  model: Model
        |}
        |type Other2 {
        |  id: ID! @id
        |  models: [Model!]!
        |}
      """.stripMargin
    val dataModel = validate(dataModelString)
    dataModel.type_!("Model").relationField_!("other").isOneToMany should be(true)
    dataModel.type_!("Model").relationField_!("other2").isManyToMany should be(true)
  }
}
