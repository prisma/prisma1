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
  "succeed if an unambiguous relation field does not specify the relation directive" in {
    val dataModelString =
      """
        |type Todo {
        |  id: ID! @id
        |  title: String
        |  comments: [Comment!]!
        |}
        |
        |type Comment {
        |  id: ID! @id
        |  text: String
        |}
      """.stripMargin
    validate(dataModelString)
  }

  "fail if ambiguous relation fields do not specify the relation directive" in {
    val dataModelString =
      """
        |type Todo {
        |  id: ID! @id
        |  title: String
        |  comments: [Comment!]!
        |  comments2: [Comment!]!
        |}
        |
        |type Comment {
        |  id: ID! @id
        |  text: String
        |}
      """.stripMargin
    val errors = validateThatMustError(dataModelString)
    errors should have(size(2))

    errors.head.`type` should equal("Todo")
    errors.head.field should equal(Some("comments"))
    errors.head.description should include("The relation field `comments` must specify a `@relation` directive")

    errors(1).`type` should equal("Todo")
    errors(1).field should equal(Some("comments2"))
    errors(1).description should include("The relation field `comments2` must specify a `@relation` directive")
  }

  "fail if ambiguous relation fields specify the same relation name" in {
    val dataModelString =
      """
        |type Todo {
        |  id: ID! @id
        |  title: String
        |  comments: [Comment!]! @relation(name: "TodoToComments")
        |  comments2: [Comment!]! @relation(name: "TodoToComments")
        |}
        |
        |type Comment {
        |  id: ID! @id
        |  todo: Todo! @relation(name: "TodoToComments")
        |  todo2: Todo! @relation(name: "TodoToComments")
        |  text: String
        |}
      """.stripMargin
    val errors = validateThatMustError(dataModelString)
    errors should have(size(4))
    errors.forall(_.description.contains("A relation directive cannot appear more than twice.")) should be(true)
  }

  // TODO: the backwards field should not be required here.
  "succeed if ambiguous relation fields specify the relation directive" in {
    val dataModelString =
      """
        |type Todo {
        |  id: ID! @id
        |  title: String
        |  comments: [Comment!]! @relation(name: "TodoToComments1")
        |  comments2: [Comment!]! @relation(name: "TodoToComments2")
        |}
        |
        |type Comment {
        |  id: ID! @id
        |  todo: Todo! @relation(name: "TodoToComments1")
        |  todo2: Todo! @relation(name: "TodoToComments2")
        |  text: String
        |}
      """.stripMargin
    val dataModel = validate(dataModelString)
    dataModel.type_!("Todo").relationField_!("comments").relationName should equal(Some("TodoToComments1"))
    dataModel.type_!("Todo").relationField_!("comments2").relationName should equal(Some("TodoToComments2"))
    dataModel.type_!("Comment").relationField_!("todo").relationName should equal(Some("TodoToComments1"))
    dataModel.type_!("Comment").relationField_!("todo2").relationName should equal(Some("TodoToComments2"))
  }

  "fail if a relation directive appears on a scalar field" in {
    val dataModelString =
      """
        |type Todo  {
        |  id: ID! @id
        |  title: String @relation(name: "TodoToComments")
        |}
        |
        |type Comment {
        |  id: ID! @id
        |  bla: String
        |}
      """.stripMargin
    val errors = validateThatMustError(dataModelString)
    errors should have(size(1))
    errors.head.`type` should equal("Todo")
    errors.head.field should equal(Some("title"))
    errors.head.description should include("cannot specify the `@relation` directive.")
  }

  "succeed if a relation name specifies the relation directive only once" in {
    val dataModelString =
      """
        |type Todo {
        |  id: ID! @id
        |  title: String
        |  comments: [Comment!]! @relation(name: "TodoToComments")
        |}
        |
        |type Comment {
        |  id: ID! @id
        |  bla: String
        |}
      """.stripMargin
    val dataModel = validate(dataModelString)
    dataModel.type_!("Todo").relationField_!("comments").relationName should equal(Some("TodoToComments"))
  }

  "succeed if a relation directive specifies a valid onDelete attribute" in {
    val dataModelString =
      """
        |type Todo {
        |  id: ID! @id
        |  title: String
        |  comments1: [Comment1!]! @relation(name: "TodoToComments1", onDelete: CASCADE)
        |  comments2: [Comment2!]! @relation(name: "TodoToComments2", onDelete: SET_NULL)
        |  comments3: [Comment3!]! @relation(name: "TodoToComments3")
        |}
        |
        |type Comment1 {
        |  id: ID! @id
        |  bla: String
        |}
        |type Comment2 {
        |  id: ID! @id
        |  bla: String
        |}
        |type Comment3 {
        |  id: ID! @id
        |  bla: String
        |}
      """.stripMargin
    val dataModel = validate(dataModelString)
    val todoType  = dataModel.type_!("Todo")
    todoType.relationField_!("comments1").cascade should equal(OnDelete.Cascade)
    todoType.relationField_!("comments2").cascade should equal(OnDelete.SetNull)
    todoType.relationField_!("comments3").cascade should equal(OnDelete.SetNull)
  }

  "fail if a relation directive specifies an invalid onDelete attribute" in {
    val dataModelString =
      """
        |type Todo {
        |  id: ID! @id
        |  title: String
        |  comments: [Comment!]! @relation(name: "TodoToComments", onDelete: INVALID)
        |}
        |
        |type Comment {
        |  id: ID! @id
        |  bla: String
        |}
      """.stripMargin
    val errors = validateThatMustError(dataModelString)
    errors should have(size(1))
    errors.head.description should include("Valid values are: CASCADE,SET_NULL.")
  }

  // TODO: adapt
  "succeed if a one field self relation does appear only once" in {
    val dataModelString =
      """
        |type Todo {
        |  id: ID! @id
        |  title: String
        |  todo: Todo @relation(name: "OneFieldSelfRelation")
        |  todos: [Todo!]! @relation(name: "OneFieldManySelfRelation")
        |}
      """.stripMargin

    val dataModel = validate(dataModelString)
  }

  "fail if the relation directive does not appear on the right fields case 1" in {
    val dataModelString =
      """
        |type Todo {
        |  id: ID! @id
        |  title: String
        |  comments: [Comment!]! @relation(name: "TodoToComments")
        |}
        |
        |type Comment {
        |  id: ID! @id
        |  bla: String
        |}
        |
        |type Author {
        |  id: ID! @id
        |  name: String
        |  todo: Todo @relation(name: "TodoToComments")
        |}
      """.stripMargin
    val errors = validateThatMustError(dataModelString)
    errors should have(size(1))
    val first = errors.head
    first.`type` should equal("Todo")
    first.field should equal(Some("comments"))
    first.description should include("But the other directive for this relation appeared on the type")
  }

  "fail if the relation directive does not appear on the right fields case 2" in {
    val dataModelString =
      """
        |type Todo {
        |  id: ID! @id
        |  title: String
        |  comments: [Comment!]! @relation(name: "TodoToComments")
        |}
        |
        |type Comment {
        |  id: ID! @id
        |  bla: String
        |}
        |
        |type Author {
        |  id: ID! @id
        |  name: String
        |  whatever: Comment @relation(name: "TodoToComments")
        |}
      """.stripMargin
    val errors = validateThatMustError(dataModelString)
    errors should have(size(2))
    val first = errors.head
    first.`type` should equal("Todo")
    first.field should equal(Some("comments"))
    first.description should include("But the other directive for this relation appeared on the type")

    val second = errors(1)
    second.`type` should equal("Author")
    second.field should equal(Some("whatever"))
    second.description should include("But the other directive for this relation appeared on the type")
  }

  "not accept that a many relation field is not marked as required" in {
    val dataModelString =
      """
        |type Todo {
        |  id: ID! @id
        |  title: String
        |  comments: [Comment!] @relation(name: "TodoToComments")
        |}
        |
        |type Comment {
        |  id: ID! @id
        |  text: String
        |  todo: Todo @relation(name: "TodoToComments")
        |}
      """.stripMargin
    val errors = validateThatMustError(dataModelString)
    errors should have(size(1))
    errors.head.`type` should equal("Todo")
    errors.head.field should equal(Some("comments"))
    errors.head.description should equal(
      "The relation field `comments` has the wrong format: `[Comment!]` Possible Formats: `Comment`, `Comment!`, `[Comment!]!`")
  }

  "succeed if a one relation field is marked as required" in {
    val dataModelString =
      """
        |type Todo {
        |  id: ID! @id
        |  title: String
        |  comments: [Comment!]! @relation(name: "TodoToComments")
        |}
        |
        |type Comment {
        |  id: ID! @id
        |  text: String
        |  todo: Todo! @relation(name: "TodoToComments")
        |}
      """.stripMargin
    val dataModel = validate(dataModelString)
    dataModel.type_!("Comment").relationField_!("todo").isRequired should be(true)
  }
}
