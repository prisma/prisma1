package com.prisma.deploy.migration.validation

import com.prisma.deploy.specutils.DeploySpecBase
import com.prisma.gc_values.{EnumGCValue, StringGCValue}
import com.prisma.shared.models.ApiConnectorCapability.{EmbeddedScalarListsCapability, MongoRelationsCapability, NonEmbeddedScalarListCapability}
import com.prisma.shared.models.FieldBehaviour._
import com.prisma.shared.models.{OnDelete, RelationStrategy, TypeIdentifier}
import org.scalatest.{Matchers, WordSpecLike}

class GeneralDataModelValidatorSpec extends WordSpecLike with Matchers with DeploySpecBase with DataModelValidationSpecBase {

  "succeed in the simplest case" in {
    val dataModelString =
      """
        |type Todo {
        |  id: ID! @id
        |  title: String
        |}
      """.stripMargin
    val dataModel = validate(dataModelString)
    dataModel.type_!("Todo").field_!("title").typeIdentifier should equal(TypeIdentifier.String)
  }

  "fail if if is syntactically incorrect" in {
    val dataModelString =
      """
        |type Todo  {
        |  title: String
        |  isDone
        |}
      """.stripMargin
    val error = validateThatMustError(dataModelString).head
    error.`type` should equal("Global")
    error.description should include("There's a syntax error in the data model.")
  }

  "fail if a type is missing" in {
    // the relation directive is there as this used to cause an exception
    val dataModelString =
      """
        |type Todo  {
        |  id: ID @id
        |  title: String
        |  owner: User @relation(name: "Test", onDelete: CASCADE)
        |}
      """.stripMargin
    val error = validateThatMustError(dataModelString)
    error should have(size(1))
    error.head.`type` should equal("Todo")
    error.head.field should equal(Some("owner"))
    error.head.description should equal("The field `owner` has the type `User` but there's no type or enum declaration with that name.")
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

  "fail if schema refers to a type that is not there" in {
    val dataModelString =
      """
        |type Todo {
        |  id: ID! @id
        |  title: String
        |  comments: [Comment!]!
        |}
      """.stripMargin

    val errors = validateThatMustError(dataModelString)
    errors should have(size(1))
    val error = errors.head
    error.`type` should equal("Todo")
    error.field should equal(Some("comments"))
    error.description should include("no type or enum declaration with that name")
  }

  "fail if the values in an enum declaration don't begin uppercase" in {
    val dataModelString =
      """
        |type Todo {
        |  id: ID! @id
        |  title: String @one @two(a:"")
        |  status: TodoStatus
        |}
        |enum TodoStatus {
        |  active
        |  done
        |}
      """.stripMargin

    val errors = validateThatMustError(dataModelString)
    errors should have(size(1))
    val error1 = errors.head
    error1.`type` should equal("TodoStatus")
    error1.field should equal(None)
    error1.description should include("uppercase")
  }

  "fail if a directive appears more than once on a field" in {
    val dataModelString =
      """
        |type Todo {
        |  id: ID! @id
        |  title: String @default(value: "foo") @default(value: "bar")
        |}
      """.stripMargin
    val errors = validateThatMustError(dataModelString)
    println(errors)
    errors should have(size(1))
    val error1 = errors.head
    error1.`type` should equal("Todo")
    error1.field should equal(Some("title"))
    error1.description should include(s"Directives must appear exactly once on a field.")
  }

  "fail if an id field does not match the valid types for a passive connector" in {
    val dataModelString =
      """
        |type Todo {
        |  id: Float @id
        |}
      """.stripMargin
    val errors = validateThatMustError(dataModelString)
    val error1 = errors.head
    error1.`type` should equal("Todo")
    error1.field should equal(Some("id"))
    error1.description should include(s"The field `id` is marked as id and therefore has to have the type `Int!`, `ID!`, or `UUID!`.")
  }

  "not fail if an id field is of type Int for a passive connector" in {
    val dataModelString =
      """
        |type Todo {
        |  myId: Int! @id
        |}
      """.stripMargin
    val dataModel = validate(dataModelString)
    dataModel.type_!("Todo").scalarField_!("myId").behaviour should contain(IdBehaviour(IdStrategy.Auto))
  }

  "fail if there is a duplicate enum in datamodel" in {
    val dataModelString =
      """
        |type Todo {
        |  id: ID! @id
        |}
        |
        |enum Privacy {
        |  A
        |  B
        |}
        |
        |enum Privacy {
        |  C
        |}
      """.stripMargin
    val errors = validateThatMustError(dataModelString)
    errors should have(size(1))
    val error1 = errors.head
    error1.`type` should equal("Privacy")
    error1.field should equal(None)
    error1.description should include(s"The enum type `Privacy` is defined twice in the schema. Enum names must be unique.")
  }

  "fail if there are duplicate fields in a type" in {
    val dataModelString =
      """
        |type Todo {
        |  id: ID! @id
        |  title: String!
        |  TITLE: String!
        |}
      """.stripMargin
    val errors = validateThatMustError(dataModelString)
    errors should have(size(2))
    val error1 = errors.head
    error1.`type` should equal("Todo")
    error1.field should equal(Some("title"))
    error1.description should include(s"The type `Todo` has a duplicate fieldName. The detection of duplicates is performed case insensitive.")
    val error2 = errors(1)
    error2.`type` should equal("Todo")
    error2.field should equal(Some("TITLE"))
    error2.description should include(s"The type `Todo` has a duplicate fieldName. The detection of duplicates is performed case insensitive.")
  }

  "fail if there are duplicate types" in {
    val dataModelString =
      """
        |type Todo {
        |  id: ID! @id
        |}
        |
        |type TODO {
        |  id: ID! @id
        |}
      """.stripMargin
    val errors = validateThatMustError(dataModelString)
    val error1 = errors.head
    error1.`type` should equal("Todo")
    error1.field should equal(None)
    error1.description should include(s"The name of the type `Todo` occurs more than once. The detection of duplicates is performed case insensitive.")
  }

  "enum types must be detected" in {
    val dataModelString =
      """
        |enum Status {
        |  A,
        |  B
        |}
      """.stripMargin

    val dataModel = validate(dataModelString)
    val enum      = dataModel.enum_!("Status")
    enum.values should equal(Vector("A", "B"))
  }
}
