package com.prisma.deploy.migration.validation

import com.prisma.deploy.connector.FieldRequirementsInterface
import com.prisma.deploy.specutils.DeploySpecBase
import com.prisma.gc_values.{EnumGCValue, StringGCValue}
import com.prisma.shared.models.ApiConnectorCapability.{EmbeddedScalarListsCapability, MigrationsCapability, NonEmbeddedScalarListCapability}
import com.prisma.shared.models.{ConnectorCapability, OnDelete, RelationStrategy, TypeIdentifier}
import com.prisma.shared.models.FieldBehaviour._
import org.scalactic.{Bad, Good, Or}
import org.scalatest.{Matchers, WordSpecLike}

class DataModelValidatorSpec extends WordSpecLike with Matchers with DeploySpecBase {

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

  "@id should error when an unknown strategy is used" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id(strategy: FOO)
        |}
      """.stripMargin
    val error = validateThatMustError(dataModelString).head
    error.`type` should equal("Model")
    error.field should equal(Some("id"))
    error.description should equal("Valid values for the strategy argument of `@id` are: AUTO, NONE.")
  }

  "@id should error on embedded types" in {
    val dataModelString =
      """
        |type Model @embedded {
        |  id: ID! @id(strategy: NONE)
        |}
      """.stripMargin
    val error = validateThatMustError(dataModelString).head
    error.`type` should equal("Model")
    error.field should equal(Some("id"))
    error.description should equal("The `@id` directive is not allowed on embedded types.")
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

  "@scalarList should be optional" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  tags: [String!]
        |}
      """.stripMargin
    val dataModel = validate(dataModelString, Set(NonEmbeddedScalarListCapability))
    dataModel.type_!("Model").scalarField_!("tags").behaviour should be(Some(ScalarListBehaviour(ScalarListStrategy.Relation)))

    val dataModel2 = validate(dataModelString, Set(EmbeddedScalarListsCapability))
    dataModel2.type_!("Model").scalarField_!("tags").behaviour should be(Some(ScalarListBehaviour(ScalarListStrategy.Embedded)))
  }

  "@scalarList must fail if an invalid argument is provided" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  tags: [String!] @scalarList(strategy: FOOBAR)
        |}
      """.stripMargin
    val error = validateThatMustError(dataModelString).head
    error.`type` should equal("Model")
    error.field should equal(Some("tags"))
    error.description should include("Valid values for the strategy argument of `@scalarList` are:")
  }

  "@default should work" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  field: String! @default(value: "my_value")
        |}
      """.stripMargin

    val dataModel = validate(dataModelString)
    val field     = dataModel.type_!("Model").scalarField_!("field")
    field.defaultValue should be(Some(StringGCValue("my_value")))
  }

  "@default should work for enum fields" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  field: Status! @default(value: B)
        |}
        |
        |enum Status {
        |  A,
        |  B
        |}
      """.stripMargin

    val dataModel = validate(dataModelString)
    val field     = dataModel.type_!("Model").enumField_!("field")
    field.defaultValue should be(Some(EnumGCValue("B")))
  }

  "@default should error if the provided value does not match the field type" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  field: String! @default(value: true)
        |}
      """.stripMargin

    val error = validateThatMustError(dataModelString).head
    error.`type` should equal("Model")
    error.field should equal(Some("field"))
    error.description should include("The value true is not a valid default for fields of type String.")
  }

  "@default should error if the provided value does not match the field type in the case of enums" in {
    val dataModelString =
      """
        |type Model {
        |  id: ID! @id
        |  field: Status! @default(value: X)
        |}
        |
        |enum Status {
        |  A,
        |  B
        |}
      """.stripMargin

    val error = validateThatMustError(dataModelString).head
    error.`type` should equal("Model")
    error.field should equal(Some("field"))
    error.description should include("The default value is invalid for this enum. Valid values are: A, B.")
  }

  "@db should work on fields" in {
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

  "@db should work on types" in {
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

  "@relationTable must be detected" in {
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

    val dataModel         = validate(dataModelString)
    val relationTableType = dataModel.type_!("ModelToModelRelation")
    relationTableType.isRelationTable should be(true)
  }

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

  def validateThatMustError(dataModel: String, capabilities: Set[ConnectorCapability] = Set.empty): Vector[DeployError] = {
    val result = validateInternal(dataModel, capabilities)
    result match {
      case Good(dm)    => sys.error("The validation did not produce an error, which was expected.")
      case Bad(errors) => errors
    }
  }

  def validate(dataModel: String, capabilities: Set[ConnectorCapability] = Set.empty) = {
    val result = validateInternal(dataModel, capabilities)
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

  def validateInternal(dataModel: String, capabilities: Set[ConnectorCapability]): Or[PrismaSdl, Vector[DeployError]] = {
    val requirements = new FieldRequirementsInterface {
      override val requiredReservedFields    = Vector.empty
      override val hiddenReservedField       = Vector.empty
      override val reservedFieldRequirements = Vector.empty
      override val isAutogenerated           = false
    }
    DataModelValidatorImpl.validate(dataModel, requirements, capabilities)
  }
}
